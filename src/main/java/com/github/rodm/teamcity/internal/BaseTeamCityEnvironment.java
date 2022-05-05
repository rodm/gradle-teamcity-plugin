/*
 * Copyright 2022 the original author or authors.
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

import com.github.rodm.teamcity.DockerOptions;
import com.github.rodm.teamcity.TeamCityEnvironment;
import com.github.rodm.teamcity.TeamCityVersion;
import org.gradle.api.Action;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class BaseTeamCityEnvironment implements TeamCityEnvironment {

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

    private final DefaultTeamCityEnvironments environments;

    private String version = "9.0";
    private final Property<String> dataDir;
    private final ConfigurableFileCollection plugins;
    private final ListProperty<String> serverOptions;
    private final ListProperty<String> agentOptions;

    @Inject
    public BaseTeamCityEnvironment(String name, DefaultTeamCityEnvironments environments, ObjectFactory factory) {
        this.name = name;
        this.environments = environments;
        this.dataDir = factory.property(String.class).convention(defaultDataDir());
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
     * The data directory for this environment's TeamCity configuration.
     */
    public String getDataDir() {
        return getDataDirProperty().get();
    }

    public void setDataDir(String dataDir) {
        this.dataDir.set(dataDir);
    }

    public Provider<String> getDataDirProperty() {
        return gradleProperty(propertyName("dataDir")).orElse(dataDir);
    }

    public Provider<String> getPluginsDirProperty() {
        return getDataDirProperty().map(path -> path + "/plugins");
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
        return gradleProperty(propertyName("serverOptions")).orElse(asStringProvider(serverOptions));
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
        return gradleProperty(propertyName("agentOptions")).orElse(asStringProvider(agentOptions));
    }

    public String getBaseHomeDir() {
        return environments.getBaseHomeDirProperty().get();
    }

    public String getBaseDataDir() {
        return environments.getBaseDataDirProperty().get();
    }

    private Provider<String> defaultDataDir() {
        return environments.getBaseDataDirProperty()
            .map(dir -> dir + "/" + TeamCityVersion.version(getVersion()).getDataVersion());
    }

    private Provider<String> gradleProperty(final String name) {
        return environments.gradleProperty(name);
    }

    private String propertyName(final String property) {
        return "teamcity.environments." + getName() + "." + property;
    }

    private Provider<String> asStringProvider(ListProperty<String> options) {
        return options.map(strings -> String.join(" ", strings));
    }

    private boolean useDocker = false;
    private DockerOptions dockerOptions;

    @Override
    public void useDocker() {
        useDocker(dockerOptions -> {
            dockerOptions.setServerImage("jetbrains/teamcity-server");
            dockerOptions.setAgentImage("jetbrains/teamcity-agent");
            dockerOptions.setServerName("teamcity-server");
            dockerOptions.setAgentName("teamcity-agent");
        });
    }

    @Override
    public void useDocker(Action<DockerOptions> configuration) {
        useDocker = true;
        if (dockerOptions == null) {
            dockerOptions = ((ExtensionAware) this).getExtensions().create("docker", DockerOptions.class);
        }
        configuration.execute(dockerOptions);
    }

    @Override
    public DockerOptions getDockerOptions() {
        return dockerOptions;
    }

    @Override
    public boolean isDockerEnabled() {
        return useDocker;
    }
}
