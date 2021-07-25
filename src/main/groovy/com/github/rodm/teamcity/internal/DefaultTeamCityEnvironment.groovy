/*
 * Copyright 2021 the original author or authors.
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
package com.github.rodm.teamcity.internal

import com.github.rodm.teamcity.TeamCityEnvironment
import com.github.rodm.teamcity.TeamCityVersion
import groovy.transform.CompileStatic
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

import static groovy.transform.TypeCheckingMode.SKIP

@CompileStatic
class DefaultTeamCityEnvironment implements TeamCityEnvironment {

    private static final List<String> DEFAULT_SERVER_OPTIONS = [
        '-Dteamcity.development.mode=true',
        '-Dteamcity.development.shadowCopyClasses=true',
        '-Dteamcity.superUser.token.saveToFile=true',
        '-Dteamcity.kotlinConfigsDsl.generateDslDocs=false'
    ].asImmutable()

    /**
     * The name of the environment
     */
    final String name

    private DefaultTeamCityEnvironments environments

    private String version = '9.0'
    private Property<String> downloadUrl
    private Provider<String> installerFile
    private Property<String> homeDir
    private Property<String> dataDir
    private Property<String> javaHome
    private ConfigurableFileCollection plugins
    private ListProperty<String> serverOptions
    private ListProperty<String> agentOptions

    DefaultTeamCityEnvironment(String name, DefaultTeamCityEnvironments environments, ObjectFactory factory) {
        this.name = name
        this.environments = environments
        this.downloadUrl = factory.property(String).convention(defaultDownloadUrl())
        this.installerFile = factory.property(String).value(defaultInstallerFile())
        this.homeDir = factory.property(String).convention(defaultHomeDir())
        this.dataDir = factory.property(String).convention(defaultDataDir())
        this.javaHome = factory.property(String).convention(System.getProperty('java.home'))
        this.plugins = factory.fileCollection()
        this.serverOptions = factory.listProperty(String)
        this.serverOptions.addAll(DEFAULT_SERVER_OPTIONS)
        this.agentOptions = factory.listProperty(String)
    }

    /**
     * The version of TeamCity this environment uses. Defaults to version '9.0'
     */
    String getVersion() {
        return version
    }

    void setVersion(String version) {
        TeamCityVersion.version(version)
        this.version = version
    }

    /**
     * The download URL used to download the TeamCity distribution for this environment.
     */
    String getDownloadUrl() {
        return environments.gradleProperty(propertyName('downloadUrl')).orElse(downloadUrl).get()
    }

    void setDownloadUrl(String downloadUrl) {
        this.downloadUrl.set(downloadUrl)
    }

    Provider<String> getInstallerFile() {
        return installerFile
    }

    /**
     * The home directory for this environment's TeamCity installation.
     */
    String getHomeDir() {
        return getHomeDirProperty().get()
    }

    void setHomeDir(String homeDir) {
        this.homeDir.set(homeDir)
    }

    Provider<String> getHomeDirProperty() {
        return environments.gradleProperty(propertyName('homeDir')).orElse(homeDir)
    }

    /**
     * The data directory for this environment's TeamCity configuration.
     */
    String getDataDir() {
        return getDataDirProperty().get()
    }

    void setDataDir(String dataDir) {
        this.dataDir.set(dataDir)
    }

    Provider<String> getDataDirProperty() {
        return environments.gradleProperty(propertyName('dataDir')).orElse(dataDir)
    }

    /**
     * The Java home directory used to start the server and agent for this environment.
     */
    String getJavaHome() {
        return getJavaHomeProperty().get()
    }

    void setJavaHome(String javaHome) {
        this.javaHome.set(javaHome)
    }

    Provider<String> getJavaHomeProperty() {
        return environments.gradleProperty(propertyName('javaHome')).orElse(javaHome)
    }

    /**
     * The list of plugins to be deployed to this environment.
     */
    Object getPlugins() {
        return plugins
    }

    void setPlugins(Object plugins) {
        this.plugins.setFrom(plugins)
    }

    void plugins(Object plugin) {
        this.plugins.from(plugin)
    }

    /**
     * The Java command line options to be used when starting the TeamCity Server.
     * Defaults to
     *      '-Dteamcity.development.mode=true'
     *      '-Dteamcity.development.shadowCopyClasses=true'
     *      '-Dteamcity.superUser.token.saveToFile=true'
     *      '-Dteamcity.kotlinConfigsDsl.generateDslDocs=false'
     */
    Object getServerOptions() {
        return serverOptions
    }

    @CompileStatic(SKIP)
    void setServerOptions(Object options) {
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

    @CompileStatic(SKIP)
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

    String getBaseHomeDir() {
        this.environments.getDefaultBaseHomeDir().get()
    }

    String getBaseDataDir() {
        this.environments.getDefaultBaseDataDir().get()
    }

    File getPluginsDir() {
        return new File(dataDir.get(), 'plugins')
    }

    private Provider<String> defaultDownloadUrl() {
        return environments.getDefaultBaseDownloadUrl().map { it + "/TeamCity-${version}.tar.gz" }
    }

    private Provider<String> defaultInstallerFile() {
        return environments.getDefaultDownloadsDir().map {it + '/' + filename() }
    }

    private String filename() {
        def url = downloadUrl.get()
        return url[(url.lastIndexOf('/') + 1)..-1]
    }

    private Provider<String> defaultHomeDir() {
        return environments.getDefaultBaseHomeDir().map {it + "/TeamCity-${version}" }
    }

    private Provider<String> defaultDataDir() {
        return environments.getDefaultBaseDataDir().map {it + "/${TeamCityVersion.version(version).dataVersion}" }
    }

    private String propertyName(String property) {
        return "teamcity.environments.${name}.${property}".toString()
    }
}
