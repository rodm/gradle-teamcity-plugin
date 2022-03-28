/*
 * Copyright 2018 Rod MacKenzie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rodm.teamcity;

import com.github.rodm.teamcity.internal.DefaultTeamCityEnvironment;
import com.github.rodm.teamcity.internal.DefaultTeamCityEnvironments;
import com.github.rodm.teamcity.tasks.Deploy;
import com.github.rodm.teamcity.tasks.DownloadTeamCity;
import com.github.rodm.teamcity.tasks.InstallTeamCity;
import com.github.rodm.teamcity.tasks.ServerPlugin;
import com.github.rodm.teamcity.tasks.StartAgent;
import com.github.rodm.teamcity.tasks.StartServer;
import com.github.rodm.teamcity.tasks.StopAgent;
import com.github.rodm.teamcity.tasks.StopServer;
import com.github.rodm.teamcity.tasks.Undeploy;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP;
import static com.github.rodm.teamcity.TeamCityServerPlugin.SERVER_PLUGIN_TASK_NAME;
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.BUILD_TASK_NAME;

public class TeamCityEnvironmentsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(TeamCityPlugin.class);

        TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension.class);
        project.afterEvaluate(new ConfigureEnvironmentTasksAction(extension));
    }

    public static class ConfigureEnvironmentTasksAction implements Action<Project> {

        private TeamCityPluginExtension extension;

        public ConfigureEnvironmentTasksAction(TeamCityPluginExtension extension) {
            this.extension = extension;
        }

        @Override
        public void execute(final Project project) {
            NamedDomainObjectContainer<TeamCityEnvironment> environments = ((DefaultTeamCityEnvironments) extension.getEnvironments()).getEnvironments();
            environments.all((Action<TeamCityEnvironment>) env -> {
                final DefaultTeamCityEnvironment environment = (DefaultTeamCityEnvironment) env;

                final String name = capitalize(environment.getName());
                final TaskProvider<DownloadTeamCity> download = project.getTasks().register("download" + name, DownloadTeamCity.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.src(environment.getDownloadUrl());
                    task.dest(project.file(environment.getInstallerFile()));
                });

                project.getTasks().register("install" + name, InstallTeamCity.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getSource().set(project.file(environment.getInstallerFile()));
                    task.getTarget().set(project.file(environment.getHomeDirProperty().get()));
                    task.dependsOn(download);
                });

                final TaskProvider<Deploy> deployPlugin = project.getTasks().register("deployTo" + name, Deploy.class, new Action<Deploy>() {
                    @Override
                    public void execute(Deploy task) {
                        task.setGroup(TEAMCITY_GROUP);
                        task.getPlugins().from(environment.getPlugins());
                        task.getPluginsDir().set(environment.getPluginsDir());
                        task.dependsOn(project.getTasks().named(BUILD_TASK_NAME));
                    }
                });

                final TaskProvider<Undeploy> undeployPlugin = project.getTasks().register("undeployFrom" + name, Undeploy.class, new Action<Undeploy>() {
                    @Override
                    public void execute(Undeploy task) {
                        task.setGroup(TEAMCITY_GROUP);
                        task.getPlugins().from(environment.getPlugins());
                        task.getPluginsDir().set(environment.getPluginsDir());
                    }
                });

                if (TeamCityVersion.version(environment.getVersion()).equalOrGreaterThan(VERSION_2018_2)) {
                    final File dataDir = project.file(environment.getDataDirProperty().get());
                    deployPlugin.configure(task -> {
                        Set<File> plugins = ((FileCollection) environment.getPlugins()).getFiles();
                        List<String> disabledPlugins = new ArrayList<>();
                        task.doFirst(new DisablePluginAction(project.getLogger(), dataDir, plugins, disabledPlugins));
                        task.doLast(new EnablePluginAction(project.getLogger(), dataDir, plugins, disabledPlugins));
                    });
                    undeployPlugin.configure(task -> {
                        Set<File> plugins = ((FileCollection) environment.getPlugins()).getFiles();
                        task.doFirst(new DisablePluginAction(project.getLogger(), dataDir, plugins, new ArrayList<>()));
                    });
                }

                final TaskProvider<StartServer> startServer = project.getTasks().register("start" + name + "Server", StartServer.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getVersion().set(environment.getVersion());
                    task.getHomeDir().set(environment.getHomeDirProperty());
                    task.getDataDir().set(environment.getDataDirProperty());
                    task.getJavaHome().set(environment.getJavaHomeProperty());
                    task.getServerOptions().set(environment.getServerOptionsProvider());
                    task.doFirst(t -> project.mkdir(environment.getDataDir()));
                    task.dependsOn(deployPlugin);
                });

                final TaskProvider<StopServer> stopServer = project.getTasks().register("stop" + name + "Server", StopServer.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getVersion().set(environment.getVersion());
                    task.getHomeDir().set(environment.getHomeDirProperty());
                    task.getJavaHome().set(environment.getJavaHomeProperty());
                    task.finalizedBy(undeployPlugin);
                });

                final TaskProvider<StartAgent> startAgent = project.getTasks().register("start" + name + "Agent", StartAgent.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getVersion().set(environment.getVersion());
                    task.getHomeDir().set(environment.getHomeDirProperty());
                    task.getJavaHome().set(environment.getJavaHomeProperty());
                    task.getAgentOptions().set(environment.getAgentOptionsProvider());
                });

                final TaskProvider<StopAgent> stopAgent = project.getTasks().register("stop" + name + "Agent", StopAgent.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getVersion().set(environment.getVersion());
                    task.getHomeDir().set(environment.getHomeDirProperty());
                    task.getJavaHome().set(environment.getJavaHomeProperty());
                });

                project.getTasks().register("start" + name, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.setDescription("Starts the TeamCity Server and Build Agent");
                    task.dependsOn(startServer, startAgent);
                });
                project.getTasks().register("stop" + name, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.setDescription("Stops the TeamCity Server and Build Agent");
                    task.dependsOn(stopAgent, stopServer);
                });

                project.getTasks().withType(ServerPlugin.class, task -> {
                    if (((FileCollection) environment.getPlugins()).isEmpty()) {
                        environment.plugins(project.getTasks().named(SERVER_PLUGIN_TASK_NAME));
                    }
                });
            });
        }

        private String capitalize(String name) {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }

    public static abstract class PluginAction implements Action<Task> {

        private static final String SUPER_USER_TOKEN_PATH = "system/pluginData/superUser/token.txt";

        private Logger logger;
        protected File dataDir;
        protected Set<File> plugins;
        protected List<String> unloadedPlugins;
        private boolean enable;
        private String path;

        private String host = "localhost";
        private int port = 8111;

        public PluginAction(Logger logger, File dataDir, Set<File> plugins, List<String> unloadedPlugins, boolean enable) {
            this.logger = logger;
            this.dataDir = dataDir;
            this.plugins = plugins;
            this.unloadedPlugins = unloadedPlugins;
            this.enable = enable;
        }

        public Logger getLogger() {
            return logger;
        }

        public String getPath() {
            return path;
        }

        @Override
        public void execute(final Task task) {
            path = task.getPath();
            plugins.forEach(file -> {
                if (canExecuteAction(task, file.getName())) {
                    executeAction(file.getName());
                } else {
                    skipAction(file.getName());
                }
            });
        }

        public abstract boolean canExecuteAction(Task task, String pluginName);

        public abstract void sendRequest(HttpURLConnection request, String pluginName);

        public void executeAction(String pluginName) {
            if (!isServerAvailable()) {
                logger.info(getPath() + ": Cannot connect to the server on http://" + host + ":" + port + ".");
                return;
            }

            String password;
            File tokenFile = new File(dataDir, SUPER_USER_TOKEN_PATH);
            if (tokenFile.isFile()) {
                try {
                    String content = new String(Files.readAllBytes(tokenFile.toPath()));
                    password = Long.valueOf(content).toString();
                    logger.debug(getPath() + ": Using " + password + " maintenance token to authenticate");
                }
                catch (IOException ignored) {
                    logger.warn(getPath() + ": Failure reading super user token file");
                    return;
                }
                catch (NumberFormatException ignored) {
                    logger.warn(getPath() + ": Malformed maintenance token");
                    return;
                }
            } else {
                logger.warn(getPath() + ": Maintenance token file does not exist. Cannot reload plugin.");
                logger.warn(getPath() + ": Check the server was started with \'-Dteamcity.superUser.token.saveToFile=true\' property.");
                return;
            }

            String authToken = "Basic " + Base64.getEncoder().encodeToString((":" + password).getBytes(StandardCharsets.UTF_8));

            URL actionURL = getPluginActionURL(pluginName);
            logger.debug(getPath() + ": Sending " + actionURL);

            try {
                HttpURLConnection request = (HttpURLConnection) actionURL.openConnection();
                try {
                    request.setRequestMethod("POST");
                    request.setRequestProperty("Authorization", authToken);
                    sendRequest(request, pluginName);
                }
                catch (IOException ex) {
                    if (request.getResponseCode() == 401) {
                        logger.warn(getPath() + ": Cannot authenticate with server on http://" + host + ":" + port + " with maintenance token " + password + ".");
                        logger.warn(getPath() + ": Check the server was started with \'-Dteamcity.superUser.token.saveToFile=true\' property.");
                    }
                    logger.warn(getPath() + ": Cannot connect to the server on http://" + host + ":" + port + ": " + request.getResponseCode(), ex);
                }
            }
            catch (IOException e) {
                logger.warn(getPath() + ": Cannot connect to server.");
            }
        }

        @SuppressWarnings("UnusedMethodParameter")
        public void skipAction(String pluginName) {
        }

        public boolean isServerAvailable() {
            try {
                Socket socket = new Socket(host, port);
                return socket.isConnected();
            }
            catch (IOException ignored) {
                return false;
            }
        }

        private URL getPluginActionURL(final String pluginName) {
            try {
                final String pluginPath = URLEncoder.encode("<TeamCity Data Directory>/plugins/" + pluginName, "UTF-8");
                return new URL("http://" + host + ":" + port + "/httpAuth/admin/plugins.html?action=setEnabled&enabled=" + enable + "&pluginPath=" + pluginPath);
            }
            catch (MalformedURLException | UnsupportedEncodingException e) {
                throw new GradleException("Failure creating plugin action URL");
            }
        }
    }

    public static class DisablePluginAction extends PluginAction {
        public DisablePluginAction(Logger logger, File dataDir, Set<File> plugins, List<String> disabledPlugins) {
            super(logger, dataDir, plugins, disabledPlugins, false);
        }

        @Override
        public boolean canExecuteAction(Task task, String pluginName) {
            File pluginDir = new File(dataDir, "plugins");
            return new File(pluginDir, pluginName).exists();
        }

        @Override
        public void skipAction(String pluginName) {
            unloadedPlugins.add(pluginName);
        }

        public void sendRequest(HttpURLConnection request, final String pluginName) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
                String result = br.lines().collect(Collectors.joining());
                if (result.contains("Plugin unloaded successfully")) {
                    getLogger().info(getPath() + ": Plugin \'" + pluginName + "\' successfully unloaded");
                    unloadedPlugins.add(pluginName);
                } else {
                    if (result.contains("Plugin unloaded partially")) {
                        getLogger().warn(getPath() + ": Plugin \'" + pluginName + "\' partially unloaded - some parts could still be running. Server restart could be needed.");
                        unloadedPlugins.add(pluginName);
                    } else {
                        final String message = result.replace("<response>", "").replace("</response>", "");
                        getLogger().warn(getPath() + ": Disabling plugin \'" + pluginName + "\' failed: " + message);
                    }
                }
            }
            catch (IOException e) {
                throw new GradleException("Failure reading response from server");
            }
        }
    }

    public static class EnablePluginAction extends PluginAction {

        public EnablePluginAction(Logger logger, File dataDir, Set<File> plugins, List<String> disabledPlugins) {
            super(logger, dataDir, plugins, disabledPlugins, true);
        }

        @Override
        public boolean canExecuteAction(Task task, String pluginName) {
            return unloadedPlugins.contains(pluginName);
        }

        public void sendRequest(HttpURLConnection request, final String pluginName) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
                String result = br.lines().collect(Collectors.joining());
                if (result.contains("Plugin loaded successfully")) {
                    getLogger().info(getPath() + ": Plugin \'" + pluginName + "\' successfully loaded");
                } else {
                    final String message = result.replace("<response>", "").replace("</response>", "");
                    getLogger().warn(getPath() + ": Enabling plugin \'" + pluginName + "\' failed: " + message);
                }
            }
            catch (IOException e) {
                throw new GradleException("Failure reading response from server");
            }
        }
    }
}
