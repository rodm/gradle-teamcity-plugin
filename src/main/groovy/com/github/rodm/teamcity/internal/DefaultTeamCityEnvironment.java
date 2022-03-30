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
package com.github.rodm.teamcity.internal;

import com.github.rodm.teamcity.TeamCityEnvironment;
import com.github.rodm.teamcity.TeamCityVersion;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultTeamCityEnvironment implements TeamCityEnvironment {

    private static final List<String> DEFAULT_SERVER_OPTIONS = Collections.unmodifiableList(
        Arrays.asList(
            "-Dteamcity.development.mode=true",
            "-Dteamcity.development.shadowCopyClasses=true",
            "-Dteamcity.superUser.token.saveToFile=true",
            "-Dteamcity.kotlinConfigsDsl.generateDslDocs=false"
        ));

    /**
     * The name of the environment
     */
    private final String name;

    private DefaultTeamCityEnvironments environments;

    private String version = "9.0";
    private Property<String> downloadUrl;
    private Provider<String> installerFile;
    private Property<String> homeDir;
    private Property<String> dataDir;
    private Property<String> javaHome;
    private ConfigurableFileCollection plugins;
    private ListProperty<String> serverOptions;
    private ListProperty<String> agentOptions;

    public DefaultTeamCityEnvironment(String name, DefaultTeamCityEnvironments environments, ObjectFactory factory) {
        this.name = name;
        this.environments = environments;
        this.downloadUrl = factory.property(String.class).convention(defaultDownloadUrl());
        this.installerFile = factory.property(String.class).value(defaultInstallerFile());
        this.homeDir = factory.property(String.class).convention(defaultHomeDir());
        this.dataDir = factory.property(String.class).convention(defaultDataDir());
        this.javaHome = factory.property(String.class).convention(System.getProperty("java.home"));
        this.plugins = factory.fileCollection();
        this.serverOptions = factory.listProperty(String.class);
        this.serverOptions.addAll(DEFAULT_SERVER_OPTIONS);
        this.agentOptions = factory.listProperty(String.class);
    }

    public final String getName() {
        return name;
    }

    /**
     * The version of TeamCity this environment uses. Defaults to version '9.0'
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        TeamCityVersion.version(version);
        this.version = version;
    }

    /**
     * The download URL used to download the TeamCity distribution for this environment.
     */
    public String getDownloadUrl() {
        return environments.gradleProperty(propertyName("downloadUrl")).orElse(downloadUrl).get();
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl.set(downloadUrl);
    }

    public Provider<String> getInstallerFile() {
        return installerFile;
    }

    /**
     * The home directory for this environment's TeamCity installation.
     */
    public String getHomeDir() {
        return getHomeDirProperty().get();
    }

    public void setHomeDir(String homeDir) {
        this.homeDir.set(homeDir);
    }

    public Provider<String> getHomeDirProperty() {
        return environments.gradleProperty(propertyName("homeDir")).orElse(homeDir);
    }

    /**
     * The data directory for this environment's TeamCity configuration.
     */
    public String getDataDir() {
        return getDataDirProperty().get();
    }

    public void setDataDir(String dataDir) {
        this.dataDir.set(dataDir);
    }

    public Provider<String> getDataDirProperty() {
        return environments.gradleProperty(propertyName("dataDir")).orElse(dataDir);
    }

    /**
     * The Java home directory used to start the server and agent for this environment.
     */
    public String getJavaHome() {
        return getJavaHomeProperty().get();
    }

    public void setJavaHome(String javaHome) {
        this.javaHome.set(javaHome);
    }

    public Provider<String> getJavaHomeProperty() {
        return environments.gradleProperty(propertyName("javaHome")).orElse(javaHome);
    }

    /**
     * The list of plugins to be deployed to this environment.
     */
    public Object getPlugins() {
        return plugins;
    }

    public void setPlugins(Object plugins) {
        this.plugins.setFrom(plugins);
    }

    public void plugins(Object plugin) {
        this.plugins.from(plugin);
    }

    /**
     * The Java command line options to be used when starting the TeamCity Server.
     * Defaults to
     *      '-Dteamcity.development.mode=true'
     *      '-Dteamcity.development.shadowCopyClasses=true'
     *      '-Dteamcity.superUser.token.saveToFile=true'
     *      '-Dteamcity.kotlinConfigsDsl.generateDslDocs=false'
     */
    public Object getServerOptions() {
        return getServerOptionsProvider().get();
    }

    public void setServerOptions(Object options) {
        this.serverOptions.empty();
        if (options instanceof List) {
            this.serverOptions.addAll((Iterable<? extends String>) options);
        } else {
            this.serverOptions.add(options.toString());
        }
    }

    public void serverOptions(String... options) {
        this.serverOptions.addAll(options);
    }

    public Provider<String> getServerOptionsProvider() {
        return optionsAsStringProvider("serverOptions", serverOptions);
    }

    /**
     * The Java command line options to be used when starting the TeamCity Agent.
     */
    public Object getAgentOptions() {
        return getAgentOptionsProvider().get();
    }

    public void setAgentOptions(Object options) {
        this.agentOptions.empty();
        if (options instanceof List) {
            this.agentOptions.addAll((Iterable<? extends String>) options);
        } else {
            this.agentOptions.add(options.toString());
        }
    }

    public void agentOptions(String... options) {
        this.agentOptions.addAll(options);
    }

    public Provider<String> getAgentOptionsProvider() {
        return optionsAsStringProvider("agentOptions", agentOptions);
    }

    public String getBaseHomeDir() {
        return this.environments.getDefaultBaseHomeDir().get();
    }

    public String getBaseDataDir() {
        return this.environments.getDefaultBaseDataDir().get();
    }

    public File getPluginsDir() {
        return new File(dataDir.get(), "plugins");
    }

    private Provider<String> defaultDownloadUrl() {
        return environments.getDefaultBaseDownloadUrl().map(baseUrl -> baseUrl + "/TeamCity-" + version + ".tar.gz");
    }

    private Provider<String> defaultInstallerFile() {
        return environments.getDefaultDownloadsDir().map(dir -> dir + "/" + filename());
    }

    private String filename() {
        String url = downloadUrl.get();
        int index = url.lastIndexOf("/") + 1;
        return url.substring(index);
    }

    private Provider<String> defaultHomeDir() {
        return environments.getDefaultBaseHomeDir().map(dir -> dir + "/TeamCity-" + getVersion());
    }

    private Provider<String> defaultDataDir() {
        return environments.getDefaultBaseDataDir()
            .map(dir -> dir + "/" + TeamCityVersion.version(getVersion()).getDataVersion());
    }

    private Provider<String> optionsAsStringProvider(String property, ListProperty<String> options) {
        return environments.gradleProperty(propertyName(property))
            .orElse(options.map(strings -> String.join(" ", strings)));
    }

    private String propertyName(final String property) {
        return "teamcity.environments." + getName() + "." + property;
    }
}