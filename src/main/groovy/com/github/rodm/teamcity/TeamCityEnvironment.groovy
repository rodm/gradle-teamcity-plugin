/*
 * Copyright 2016 Rod MacKenzie
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

import groovy.transform.CompileStatic
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory

import javax.inject.Inject

@CompileStatic
class TeamCityEnvironment {

    /**
     * The name of the environment
     */
    final String name

    private String version = '9.0'

    /**
     * The download URL used to download the TeamCity distribution for this environment.
     */
    String downloadUrl

    /**
     * The home directory for this environment's TeamCity installation.
     */
    File homeDir

    /**
     * The data directory for this environment's TeamCity configuration.
     */
    File dataDir

    /**
     * The Java home directory used to start the server and agent for this environment.
     */
    File javaHome

    private ConfigurableFileCollection plugins

    private List<String> serverOptions = [
        '-Dteamcity.development.mode=true',
        '-Dteamcity.development.shadowCopyClasses=true',
        '-Dteamcity.superUser.token.saveToFile=true'
    ]

    private List<String> agentOptions = []

    @Inject
    TeamCityEnvironment(String name, ObjectFactory factory) {
        this.name = name
        this.plugins = factory.fileCollection()
    }

    File getPluginsDir() {
        return new File(dataDir, 'plugins')
    }

    /**
     * The version of TeamCity this environment uses. Defaults to version '9.0'
     */
    String getVersion() {
        return version
    }

    void setVersion(String version) {
        this.version = version
        TeamCityVersion.version(version)
    }

    /**
     * The list of plugins to be deployed to this environment.
     */
    ConfigurableFileCollection getPlugins() {
        return plugins
    }

    def setPlugins(Object plugins) {
        this.plugins.setFrom(plugins)
    }

    def setPlugins(List<Object> plugins) {
        this.plugins.setFrom(plugins)
    }

    def plugins(Object plugin) {
        this.plugins.from(plugin)
    }

    /**
     * The Java command line options to be used when starting the TeamCity Server.
     * Defaults to
     *      '-Dteamcity.development.mode=true'
     *      '-Dteamcity.development.shadowCopyClasses=true'
     *      '-Dteamcity.superUser.token.saveToFile=true'
     */
    Object getServerOptions() {
        return serverOptions.join(' ')
    }

    def setServerOptions(Object options) {
        this.serverOptions.clear()
        if (options instanceof List) {
            this.serverOptions.addAll(options)
        } else {
            this.serverOptions.add(options.toString())
        }
    }

    void serverOptions(String... options) {
        this.serverOptions.addAll(options)
    }

    /**
     * The Java command line options to be used when starting the TeamCity Agent.
     */
    Object getAgentOptions() {
        return agentOptions.join(' ')
    }

    void setAgentOptions(Object options) {
        this.agentOptions.clear()
        if (options instanceof List) {
            this.agentOptions.addAll(options)
        } else {
            this.agentOptions.add(options.toString())
        }
    }

    void agentOptions(String... options) {
        this.agentOptions.addAll(options)
    }
}
