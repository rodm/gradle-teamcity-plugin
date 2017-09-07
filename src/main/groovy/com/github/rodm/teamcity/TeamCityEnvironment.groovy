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

@CompileStatic
class TeamCityEnvironment {

    /**
     * The name of the environment
     */
    final String name

    /**
     * The version of TeamCity this environment uses. Defaults to version '9.0'
     */
    String version = '9.0'

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

    private List<Object> plugins = []

    private List<String> serverOptions = ['-Dteamcity.development.mode=true', '-Dteamcity.development.shadowCopyClasses=true']

    private List<String> agentOptions = []

    TeamCityEnvironment(String name) {
        this.name = name
    }

    File getPluginsDir() {
        return new File(dataDir, 'plugins')
    }

    /**
     * The list of plugins to be deployed to this environment.
     */
    List<Object> getPlugins() {
        return plugins
    }

    def setPlugins(Object plugins) {
        this.plugins.clear()
        this.plugins.addAll(plugins)
    }

    def setPlugins(List<Object> plugins) {
        this.plugins.clear()
        this.plugins.addAll(plugins)
    }

    def plugins(Object plugin) {
        this.plugins.add(plugin)
    }

    /**
     * The Java command line options to be used when starting the TeamCity Server.
     * Defaults to '-Dteamcity.development.mode=true -Dteamcity.development.shadowCopyClasses=true'
     */
    def getServerOptions() {
        return serverOptions.join(' ')
    }

    def setServerOptions(String options) {
        this.serverOptions.clear()
        this.serverOptions.addAll(options)
    }

    def setServerOptions(List<String> options) {
        this.serverOptions.clear()
        this.serverOptions.addAll(options)
    }

    def serverOptions(String... options) {
        this.serverOptions.addAll(options)
    }

    /**
     * The Java command line options to be used when starting the TeamCity Agent.
     */
    def getAgentOptions() {
        return agentOptions.join(' ')
    }

    def setAgentOptions(String options) {
        this.agentOptions.clear()
        this.agentOptions.add(options)
    }

    def setAgentOptions(List<String> options) {
        this.agentOptions.clear()
        this.agentOptions.addAll(options)
    }

    def agentOptions(String... options) {
        this.agentOptions.addAll(options)
    }
}
