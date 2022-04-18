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
import com.github.rodm.teamcity.internal.DisablePluginAction;
import com.github.rodm.teamcity.internal.EnablePluginAction;
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
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

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

        private final TeamCityPluginExtension extension;

        public ConfigureEnvironmentTasksAction(TeamCityPluginExtension extension) {
            this.extension = extension;
        }

        @Override
        public void execute(final Project project) {
            DefaultTeamCityEnvironments environments = (DefaultTeamCityEnvironments) extension.getEnvironments();
            NamedDomainObjectContainer<TeamCityEnvironment> container = environments.getEnvironments();
            container.all(env -> {
                final DefaultTeamCityEnvironment environment = (DefaultTeamCityEnvironment) env;

                Provider<String> downloadUrl = gradleProperty(project, environment.propertyName("downloadUrl"))
                    .orElse(environment.getDownloadUrl());
                Provider<String> installerFile = environment.getInstallerFile();
                Provider<String> homeDir = gradleProperty(project, environment.propertyName("homeDir"))
                    .orElse(environment.getHomeDirProperty());
                Provider<String> dataDir = gradleProperty(project, environment.propertyName("dataDir"))
                    .orElse(environment.getDataDirProperty());
                Provider<String> pluginsDir = dataDir.map(path -> path + "/plugins");
                Provider<String> javaHome = gradleProperty(project, environment.propertyName("javaHome"))
                    .orElse(environment.getJavaHomeProperty());
                Provider<String> serverOptions = gradleProperty(project, environment.propertyName("serverOptions"))
                    .orElse(environment.getServerOptionsProvider());
                Provider<String> agentOptions = gradleProperty(project, environment.propertyName("agentOptions"))
                    .orElse(environment.getAgentOptionsProvider());

                final String name = capitalize(environment.getName());
                final TaskProvider<DownloadTeamCity> download = project.getTasks().register("download" + name, DownloadTeamCity.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.src(downloadUrl);
                    task.dest(project.file(installerFile));
                });
                project.getTasks().register("install" + name, InstallTeamCity.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getSource().set(project.file(installerFile));
                    task.getTarget().set(project.file(homeDir));
                    task.dependsOn(download);
                });

                final TaskProvider<Deploy> deployPlugin = project.getTasks().register("deployTo" + name, Deploy.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getPlugins().from(environment.getPlugins());
                    task.getPluginsDir().set(project.file(pluginsDir));
                    task.dependsOn(project.getTasks().named(BUILD_TASK_NAME));
                });

                final TaskProvider<Undeploy> undeployPlugin = project.getTasks().register("undeployFrom" + name, Undeploy.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getPlugins().from(environment.getPlugins());
                    task.getPluginsDir().set(project.file(pluginsDir));
                });

                if (TeamCityVersion.version(environment.getVersion()).equalOrGreaterThan(VERSION_2018_2)) {
                    final File dataDirAsFile = project.file(environment.getDataDirProperty().get());
                    deployPlugin.configure(task -> {
                        Set<File> plugins = ((FileCollection) environment.getPlugins()).getFiles();
                        List<String> disabledPlugins = new ArrayList<>();
                        task.doFirst(new DisablePluginAction(project.getLogger(), dataDirAsFile, plugins, disabledPlugins));
                        task.doLast(new EnablePluginAction(project.getLogger(), dataDirAsFile, plugins, disabledPlugins));
                    });
                    undeployPlugin.configure(task -> {
                        Set<File> plugins = ((FileCollection) environment.getPlugins()).getFiles();
                        task.doFirst(new DisablePluginAction(project.getLogger(), dataDirAsFile, plugins, new ArrayList<>()));
                    });
                }

                final TaskProvider<StartServer> startServer = project.getTasks().register("start" + name + "Server", StartServer.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getVersion().set(environment.getVersion());
                    task.getHomeDir().set(homeDir);
                    task.getDataDir().set(dataDir);
                    task.getJavaHome().set(javaHome);
                    task.getServerOptions().set(serverOptions);
                    task.doFirst(t -> project.mkdir(dataDir));
                    task.dependsOn(deployPlugin);
                });

                final TaskProvider<StopServer> stopServer = project.getTasks().register("stop" + name + "Server", StopServer.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getVersion().set(environment.getVersion());
                    task.getHomeDir().set(homeDir);
                    task.getJavaHome().set(javaHome);
                    task.finalizedBy(undeployPlugin);
                });

                final TaskProvider<StartAgent> startAgent = project.getTasks().register("start" + name + "Agent", StartAgent.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getVersion().set(environment.getVersion());
                    task.getHomeDir().set(homeDir);
                    task.getJavaHome().set(javaHome);
                    task.getAgentOptions().set(agentOptions);
                });

                final TaskProvider<StopAgent> stopAgent = project.getTasks().register("stop" + name + "Agent", StopAgent.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getVersion().set(environment.getVersion());
                    task.getHomeDir().set(homeDir);
                    task.getJavaHome().set(javaHome);
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

        private Provider<String> gradleProperty(final Project project, final String name) {
            Callable<String> callable = () ->
                project.findProperty(name) == null ? null : (String) project.property(name);
            return project.provider(callable);
        }
    }
}
