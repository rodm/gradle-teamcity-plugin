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
package com.github.rodm.teamcity

import com.github.rodm.teamcity.internal.DefaultTeamCityEnvironment
import com.github.rodm.teamcity.tasks.Deploy
import com.github.rodm.teamcity.tasks.DownloadTeamCity
import com.github.rodm.teamcity.tasks.InstallTeamCity
import com.github.rodm.teamcity.tasks.ServerPlugin
import com.github.rodm.teamcity.tasks.StartAgent
import com.github.rodm.teamcity.tasks.StartServer
import com.github.rodm.teamcity.tasks.StopAgent
import com.github.rodm.teamcity.tasks.StopServer
import com.github.rodm.teamcity.tasks.Undeploy
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger

import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP
import static com.github.rodm.teamcity.TeamCityServerPlugin.SERVER_PLUGIN_TASK_NAME
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2
import static org.gradle.language.base.plugins.LifecycleBasePlugin.BUILD_TASK_NAME

class TeamCityEnvironmentsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(TeamCityPlugin)

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        project.afterEvaluate(new ConfigureEnvironmentTasksAction(extension))
    }

    static class ConfigureEnvironmentTasksAction implements Action<Project> {

        private TeamCityPluginExtension extension

        ConfigureEnvironmentTasksAction(TeamCityPluginExtension extension) {
            this.extension = extension
        }

        @Override
        void execute(Project project) {
            NamedDomainObjectContainer<TeamCityEnvironment> environments = extension.environments.environments
            environments.each { DefaultTeamCityEnvironment environment ->

                String name = environment.name.capitalize()
                def download = project.tasks.register("download${name}", DownloadTeamCity) {
                    group = TEAMCITY_GROUP
                    src { environment.downloadUrl }
                    dest { project.file(environment.installerFile) }
                }

                project.tasks.register("install${name}", InstallTeamCity) {
                    group = TEAMCITY_GROUP
                    source.set(project.file(environment.installerFile))
                    target.set(project.file(environment.homeDirProperty.get()))
                    dependsOn download
                }

                def deployPlugin = project.tasks.register("deployTo${name}", Deploy) {
                    group = TEAMCITY_GROUP
                    plugins.from(environment.plugins)
                    pluginsDir.set(environment.pluginsDir)
                    dependsOn project.tasks.named(BUILD_TASK_NAME)
                }

                def undeployPlugin = project.tasks.register("undeployFrom${name}", Undeploy) {
                    group = TEAMCITY_GROUP
                    plugins.from(environment.plugins)
                    pluginsDir.set(environment.pluginsDir)
                }
                if (TeamCityVersion.version(environment.version) >= VERSION_2018_2) {
                    def dataDir = project.file(environment.dataDirProperty.get())
                    deployPlugin.configure {
                        def plugins = environment.plugins.files
                        def disabledPlugins = []
                        doFirst(new DisablePluginAction(project.logger, dataDir, plugins, disabledPlugins))
                        doLast(new EnablePluginAction(project.logger, dataDir, plugins, disabledPlugins))
                    }
                    undeployPlugin.configure {
                        def plugins = environment.plugins.files
                        doFirst(new DisablePluginAction(project.logger, dataDir, plugins, []))
                    }
                }

                def startServer = project.tasks.register("start${name}Server", StartServer) {
                    group = TEAMCITY_GROUP
                    version.set(environment.version)
                    homeDir.set(environment.homeDirProperty)
                    dataDir.set(environment.dataDirProperty)
                    javaHome.set(environment.javaHomeProperty)
                    serverOptions.set(environment.serverOptions.map({it.join(' ')}))
                    doFirst {
                        project.mkdir(getDataDir())
                    }
                    dependsOn deployPlugin
                }

                def stopServer = project.tasks.register("stop${name}Server", StopServer) {
                    group = TEAMCITY_GROUP
                    version.set(environment.version)
                    homeDir.set(environment.homeDirProperty)
                    javaHome.set(environment.javaHomeProperty)
                    finalizedBy undeployPlugin
                }

                def startAgent = project.tasks.register("start${name}Agent", StartAgent) {
                    group = TEAMCITY_GROUP
                    version.set(environment.version)
                    homeDir.set(environment.homeDirProperty)
                    javaHome.set(environment.javaHomeProperty)
                    agentOptions.set(environment.agentOptions.map({it.join(' ')}))
                }

                def stopAgent = project.tasks.register("stop${name}Agent", StopAgent) {
                    group = TEAMCITY_GROUP
                    version.set(environment.version)
                    homeDir.set(environment.homeDirProperty)
                    javaHome.set(environment.javaHomeProperty)
                }

                project.tasks.register("start${name}") {
                    group = TEAMCITY_GROUP
                    description = 'Starts the TeamCity Server and Build Agent'
                    dependsOn = [startServer, startAgent]
                }
                project.tasks.register("stop${name}") {
                    group = TEAMCITY_GROUP
                    description = 'Stops the TeamCity Server and Build Agent'
                    dependsOn = [stopServer, stopAgent]
                }

                project.tasks.withType(ServerPlugin) {
                    if (environment.plugins.isEmpty()) {
                        environment.plugins(project.tasks.named(SERVER_PLUGIN_TASK_NAME))
                    }
                }
            }
        }
    }

    @CompileStatic
    static abstract class PluginAction implements Action<Task> {

        private static final String SUPER_USER_TOKEN_PATH = 'system/pluginData/superUser/token.txt'

        private Logger logger
        protected File dataDir
        protected Set<File> plugins
        protected List<String> unloadedPlugins
        private boolean enable
        private String path

        private String host = 'localhost'
        private int port = 8111

        PluginAction(Logger logger, File dataDir, Set<File> plugins, List<String> unloadedPlugins, boolean enable) {
            this.logger = logger
            this.dataDir = dataDir
            this.plugins = plugins
            this.unloadedPlugins = unloadedPlugins
            this.enable = enable
        }

        Logger getLogger() {
            return logger
        }

        String getPath() {
            return path
        }

        @Override
        void execute(Task task) {
            path = task.path
            plugins.each { file ->
                if (canExecuteAction(task, file.name)) {
                    executeAction(file.name)
                } else {
                    skipAction(file.name)
                }
            }
        }

        abstract boolean canExecuteAction(Task task, String pluginName)

        abstract void sendRequest(HttpURLConnection request, String pluginName)

        void executeAction(String pluginName) {
            if (!isServerAvailable()) {
                logger.info("${path}: Cannot connect to the server on http://${host}:${port}.")
                return
            }
            def password
            def tokenFile = new File(dataDir, SUPER_USER_TOKEN_PATH)
            if (tokenFile.isFile()) {
                try {
                    password = tokenFile.text.toLong().toString()
                    logger.debug("${path}: Using ${password} maintenance token to authenticate")
                }
                catch (NumberFormatException ignored) {
                    logger.warn("${path}: Malformed maintenance token")
                    return
                }
            } else {
                logger.warn("${path}: Maintenance token file does not exist. Cannot reload plugin.")
                logger.warn("${path}: Check the server was started with '-Dteamcity.superUser.token.saveToFile=true' property.")
                return
            }

            String authToken = "Basic " + ":${password}".bytes.encodeBase64().toString()

            def actionURL = getPluginActionURL(pluginName)
            logger.debug("${path}: Sending " + actionURL.toString())

            HttpURLConnection request = actionURL.openConnection() as HttpURLConnection
            request.requestMethod = "POST"
            request.setRequestProperty ("Authorization", authToken)
            try {
                sendRequest(request, pluginName)
            }
            catch (IOException ex) {
                if (request.responseCode == 401) {
                    logger.warn("${path}: Cannot authenticate with server on http://${host}:${port} with maintenance token ${password}.")
                    logger.warn("${path}: Check the server was started with '-Dteamcity.superUser.token.saveToFile=true' property.")
                }
                logger.warn("${path}: Cannot connect to the server on http://${host}:${port}: ${request.responseCode}", ex)
            }
        }

        @SuppressWarnings('UnusedMethodParameter')
        void skipAction(String pluginName) {}

        boolean isServerAvailable() {
            try {
                def socket = new Socket(host, port)
                return socket.connected
            }
            catch (ConnectException ignored) {
                return false
            }
        }

        private URL getPluginActionURL(String pluginName) {
            def pluginPath = URLEncoder.encode("<TeamCity Data Directory>/plugins/${pluginName}", 'UTF-8')
            "http://${host}:${port}/httpAuth/admin/plugins.html?action=setEnabled&enabled=${enable}&pluginPath=${pluginPath}".toURL()
        }
    }

    @CompileStatic
    static class DisablePluginAction extends PluginAction {

        DisablePluginAction(Logger logger, File dataDir, Set<File> plugins, List<String> disabledPlugins) {
            super(logger, dataDir, plugins, disabledPlugins, false)
        }

        @Override
        boolean canExecuteAction(Task task, String pluginName) {
            def pluginDir = new File(dataDir, 'plugins')
            new File(pluginDir, pluginName).exists()
        }

        @Override
        void skipAction(String pluginName) {
            unloadedPlugins.add(pluginName)
        }

        void sendRequest(HttpURLConnection request, String pluginName) {
            def result = request.inputStream.text
            if (result.contains("Plugin unloaded successfully")) {
                logger.info("${path}: Plugin '${pluginName}' successfully unloaded")
                unloadedPlugins.add(pluginName)
            } else {
                if (result.contains("Plugin unloaded partially")) {
                    logger.warn("${path}: Plugin '${pluginName}' partially unloaded - some parts could still be running. Server restart could be needed.")
                    unloadedPlugins.add(pluginName)
                } else {
                    def message = result.replace('<response>', '').replace('</response>', '')
                    logger.warn("${path}: Disabling plugin '${pluginName}' failed: ${message}")
                }
            }
        }
    }

    @CompileStatic
    static class EnablePluginAction extends PluginAction {

        EnablePluginAction(Logger logger, File dataDir, Set<File> plugins, List<String> disabledPlugins) {
            super(logger, dataDir, plugins, disabledPlugins, true)
        }

        @Override
        boolean canExecuteAction(Task task, String pluginName) {
            unloadedPlugins.contains(pluginName)
        }

        void sendRequest(HttpURLConnection request, String pluginName) {
            def result = request.inputStream.text
            if (result.contains("Plugin loaded successfully")) {
                logger.info("${path}: Plugin '${pluginName}' successfully loaded")
            } else {
                def message = result.replace('<response>', '').replace('</response>', '')
                logger.warn("${path}: Enabling plugin '${pluginName}' failed: ${message}")
            }
        }
    }
}
