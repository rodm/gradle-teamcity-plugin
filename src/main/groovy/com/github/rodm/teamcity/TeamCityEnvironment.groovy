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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import javax.inject.Inject

@CompileStatic
class TeamCityEnvironment {

    private static final List<String> DEFAULT_SERVER_OPTIONS = [
        '-Dteamcity.development.mode=true',
        '-Dteamcity.development.shadowCopyClasses=true',
        '-Dteamcity.superUser.token.saveToFile=true'
    ].asImmutable()

    /**
     * The name of the environment
     */
    final String name

    private String version = '9.0'
    private Property<String> downloadUrl

    /**
     * The home directory for this environment's TeamCity installation.
     */
    File homeDir

    /**
     * The data directory for this environment's TeamCity configuration.
     */
    File dataDir

    private Property<String> javaHome
    private ConfigurableFileCollection plugins
    private ListProperty<String> serverOptions
    private ListProperty<String> agentOptions

    @Inject
    TeamCityEnvironment(String name, ObjectFactory factory) {
        this.name = name
        this.downloadUrl= factory.property(String)
        this.javaHome = factory.property(String).convention(System.getProperty('java.home'))
        this.plugins = factory.fileCollection()
        this.serverOptions = factory.listProperty(String)
        this.serverOptions.addAll(DEFAULT_SERVER_OPTIONS)
        this.agentOptions = factory.listProperty(String)
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
     * The download URL used to download the TeamCity distribution for this environment.
     */
    Property<String> getDownloadUrl() {
        return downloadUrl
    }

    void setDownloadUrl(String downloadUrl) {
        this.downloadUrl.set(downloadUrl)
    }

    /**
     * The Java home directory used to start the server and agent for this environment.
     */
    Property<String> getJavaHome() {
        return javaHome
    }
    void setJavaHome(String javaHome) {
        this.javaHome.set(javaHome)
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
        return serverOptions
    }

    def setServerOptions(Object options) {
        this.serverOptions.empty()
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
        return agentOptions
    }

    void setAgentOptions(Object options) {
        this.agentOptions.empty()
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
