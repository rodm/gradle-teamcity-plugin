/*
 * Copyright 2018 Rod MacKenzie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rodm.teamcity

import com.github.rodm.teamcity.tasks.InstallTeamCity
import com.github.rodm.teamcity.tasks.StartAgent
import com.github.rodm.teamcity.tasks.StartServer
import com.github.rodm.teamcity.tasks.StopAgent
import com.github.rodm.teamcity.tasks.StopServer
import com.github.rodm.teamcity.tasks.Unpack
import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete

import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2

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
            def build = project.tasks.getByName('build')
            TeamCityEnvironments environments = extension.environments
            environments.environments.each { environment ->
                defaultMissingProperties(project, environments, environment)

                String name = environment.name.capitalize()
                def download = project.tasks.create(String.format("download%s", name), Download)
                download.src { environment.downloadUrl }
                download.dest { project.file("${environments.downloadsDir}/${toFilename(environment.downloadUrl)}") }

                def unpack = project.tasks.create(String.format("unpack%s", name), Unpack) {
                    conventionMapping.map('source') {
                        project.file("${environments.downloadsDir}/${toFilename(environment.downloadUrl)}")
                    }
                    conventionMapping.map('target') { environment.homeDir }
                }
                unpack.dependsOn download
                def install = project.tasks.create(String.format("install%s", name), InstallTeamCity)
                install.dependsOn unpack

                def deployPlugin = project.tasks.create(String.format('deployTo%s', name), Copy) {
                    from { environment.plugins }
                    into { environment.pluginsDir }
                }
                deployPlugin.dependsOn build
                if (TeamCityVersion.version(environment.version) >= VERSION_2018_2) {
                    deployPlugin.doFirst(new DisablePluginAction(project.logger, environment.dataDir))
                    deployPlugin.doLast(new EnablePluginAction(project.logger, environment.dataDir))
                }

                def undeployPlugin = project.tasks.create(String.format('undeployFrom%s', name), Delete) {
                    delete {
                        project.fileTree(dir: environment.pluginsDir,
                            includes: project.files(environment.plugins).collect { it.name })
                    }
                }

                def startServer = project.tasks.create(String.format('start%sServer', name), StartServer) {
                    conventionMapping.map('homeDir') { environment.homeDir }
                    conventionMapping.map('dataDir') { environment.dataDir }
                    conventionMapping.map('javaHome') { environment.javaHome }
                    conventionMapping.map('serverOptions') { environment.serverOptions }
                }
                startServer.dependsOn deployPlugin

                def stopServer = project.tasks.create(String.format('stop%sServer', name), StopServer) {
                    conventionMapping.map('homeDir') { environment.homeDir }
                    conventionMapping.map('javaHome') { environment.javaHome }
                }
                stopServer.finalizedBy undeployPlugin

                project.tasks.create(String.format('start%sAgent', name), StartAgent) {
                    conventionMapping.map('homeDir') { environment.homeDir }
                    conventionMapping.map('javaHome') { environment.javaHome }
                    conventionMapping.map('agentOptions') { environment.agentOptions }
                }

                project.tasks.create(String.format('stop%sAgent', name), StopAgent) {
                    conventionMapping.map('homeDir') { environment.homeDir }
                    conventionMapping.map('javaHome') { environment.javaHome }
                }
            }
        }

        private static void defaultMissingProperties(Project project, TeamCityEnvironments environments, TeamCityEnvironment environment) {
            environment.with {
                downloadUrl = downloadUrl ?: "${environments.baseDownloadUrl}/TeamCity-${version}.tar.gz"
                homeDir = homeDir ?: project.file("${environments.baseHomeDir}/TeamCity-${version}")
                dataDir = dataDir ?: project.file("${environments.baseDataDir}/" + (version =~ (/(\d+\.\d+).*/))[0][1])
                javaHome = javaHome ?: project.file(System.properties['java.home'])

                if (plugins.isEmpty()) {
                    def serverPlugin = project.tasks.findByName('serverPlugin')
                    if (serverPlugin) {
                        plugins serverPlugin.archivePath
                    }
                }
            }
        }

        private static String toFilename(String url) {
            return url[(url.lastIndexOf('/') + 1)..-1]
        }
    }

    static abstract class AbstractPluginAction implements Action<Copy> {

        private static final String SUPER_USER_TOKEN_PATH = 'system/pluginData/superUser/token.txt'

        private Logger logger
        private File dataDir
        private boolean enable
        private String path

        private String host = 'localhost'
        private int port = 8111

        AbstractPluginAction(Logger logger, File dataDir, boolean enable) {
            this.logger = logger
            this.dataDir = dataDir
            this.enable = enable
        }

        Logger getLogger() {
            return logger
        }

        String getPath() {
            return path
        }

        @Override
        void execute(Copy copy) {
            path = copy.path
            copy.inputs.sourceFiles.files.each { file ->
                processPlugin(file.name)
            }
        }

        abstract void sendRequest(HttpURLConnection request);

        void processPlugin(String pluginName) {
            if (!isServerAvailable()) {
                logger.info("${path}: Cannot connect to the server on http://${host}:${port}.")
                return
            }
            def password
            def tokenFile = new File(dataDir, SUPER_USER_TOKEN_PATH)
            if (!tokenFile.isFile()) {
                logger.warn("${path}: Maintenance token file does not exist. Cannot reload plugin.")
                logger.warn("${path}: Check the server was started with '-Dteamcity.superUser.token.saveToFile=true' property.")
                return
            } else {
                try {
                    password = tokenFile.text.toLong().toString()
                    logger.debug("${path}: Using ${password} maintenance token to authenticate")
                }
                catch (NumberFormatException ignored) {
                    logger.warn("${path}: Malformed maintenance token")
                    return
                }
            }

            byte[] bytes = Base64.getEncoder().encode(":${password}".getBytes('UTF-8'))
            String authToken = "Basic " + new String(bytes)

            def actionURL = getPluginActionURL(pluginName)
            logger.debug("${path}: Sending " + actionURL.toString())

            HttpURLConnection request = actionURL.openConnection() as HttpURLConnection
            request.requestMethod = "POST"
            request.setRequestProperty ("Authorization", authToken)
            try {
                sendRequest(request)
            }
            catch (IOException ex) {
                if (request.responseCode == 401) {
                    logger.warn("${path}: Cannot authenticate with server on http://${host}:${port} with maintenance token ${password}.")
                    logger.warn("${path}: Check the server was started with '-Dteamcity.superUser.token.saveToFile=true' property.")
                }
                logger.warn("${path}: Cannot connect to the server on http://${host}:${port}: ${request.responseCode}", ex)
            }
        }

        private boolean isServerAvailable() {
            try {
                def socket = new Socket(host, port)
                return socket.connected
            }
            catch (ConnectException ignored) {
                return false
            }
        }

        private URL getPluginActionURL(String pluginName) {
            "http://${host}:${port}/httpAuth/admin/plugins.html?action=setEnabled&enabled=${enable}&pluginPath=%3CTeamCity%20Data%20Directory%3E/plugins/${pluginName}".toURL()
        }
    }

    static class DisablePluginAction extends AbstractPluginAction {

        DisablePluginAction(Logger logger, File dataDir) {
            super(logger, dataDir, false)
        }

        void sendRequest(HttpURLConnection request) {
            def result = request.inputStream.text
            if (!result.contains("Plugin unloaded successfully")) {
                if (result.contains("Plugin unloaded partially")) {
                    logger.warn("${path}: Plugin partially unloaded - some parts could still be running. Server restart could be needed.")
                } else {
                    logger.warn(result)
                }
            } else {
                logger.info("${path}: Plugin successfully unloaded")
            }
        }
    }

    static class EnablePluginAction extends AbstractPluginAction {

        EnablePluginAction(Logger logger, File dataDir) {
            super(logger, dataDir, true)
        }

        void sendRequest(HttpURLConnection request) {
            def result = request.inputStream.text
            if (!result.contains("Plugin loaded successfully")) {
                logger.warn(result)
            } else {
                logger.info("${path}: Plugin successfully loaded")
            }
        }
    }
}
