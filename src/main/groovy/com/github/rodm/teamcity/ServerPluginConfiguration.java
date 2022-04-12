/*
 * Copyright 2015 Rod MacKenzie
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

import com.github.rodm.teamcity.internal.DefaultPublishConfiguration;
import com.github.rodm.teamcity.internal.DefaultSignConfiguration;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ExtensionAware;

import java.io.File;

/**
 * Server-side plugin configuration
 */
public class ServerPluginConfiguration extends PluginConfiguration implements TeamCityEnvironments {

    private static final Logger LOGGER = Logging.getLogger(ServerPluginConfiguration.class);

    private final ConfigurableFileCollection web;
    private PublishConfiguration publish;
    private SignConfiguration sign;

    private final TeamCityEnvironments environments;

    public ServerPluginConfiguration(Project project, TeamCityEnvironments environments) {
        super(project);
        this.web = project.getObjects().fileCollection();
        this.environments = environments;
    }

    /**
     * Configures the server-side plugin descriptor for the TeamCity plugin.
     *
     * <p>The given action is executed to configure the server-side plugin descriptor.</p>
     *
     * @param configuration The action.
     */
    public void descriptor(Action<ServerPluginDescriptor> configuration) {
        if (getDescriptor() == null) {
            ServerPluginDescriptor descriptor = ((ExtensionAware) this).getExtensions().create("descriptor", ServerPluginDescriptor.class);
            descriptor.init();
            setDescriptor(descriptor);
        }
        configuration.execute((ServerPluginDescriptor) getDescriptor());
    }

    public ConfigurableFileCollection getWeb() {
        return web;
    }

    public void setWeb(Object web) {
        this.web.setFrom(web);
    }

    public void web(Object web) {
        this.web.from(web);
    }

    /**
     * Configures the credentials to publish the plugin to the JetBrains Plugin Repository.
     *
     * <p>The given action is executed to configure the publishing credentials.</p>
     *
     * @param configuration The action.
     */
    public void publish(Action<PublishConfiguration> configuration) {
        if (publish == null) {
            publish = ((ExtensionAware) this).getExtensions().create(PublishConfiguration.class, "publish", DefaultPublishConfiguration.class);
        }
        configuration.execute(publish);
    }

    public PublishConfiguration getPublish() {
        return publish;
    }

    public void sign(Action<SignConfiguration> configuration) {
        if (sign == null) {
            sign = ((ExtensionAware) this).getExtensions().create(SignConfiguration.class, "sign", DefaultSignConfiguration.class);
        }
        configuration.execute(sign);
    }

    public SignConfiguration getSign() {
        return sign;
    }

    public void setDownloadsDir(String downloadsDir) {
        LOGGER.warn("downloadsDir property in server configuration is deprecated");
        environments.setDownloadsDir(downloadsDir);
    }

    public String getDownloadsDir() {
        return environments.getDownloadsDir();
    }

    public void setBaseDownloadUrl(String baseDownloadUrl) {
        LOGGER.warn("baseDownloadUrl property in server configuration is deprecated");
        environments.setBaseDownloadUrl(baseDownloadUrl);
    }

    public String getBaseDownloadUrl() {
        return environments.getBaseDownloadUrl();
    }

    public void setBaseHomeDir(String baseHomeDir) {
        LOGGER.warn("baseHomeDir property in server configuration is deprecated");
        environments.setBaseHomeDir(baseHomeDir);
    }

    public void setBaseHomeDir(File baseHomeDir) {
        LOGGER.warn("baseHomeDir property in server configuration is deprecated");
        environments.setBaseHomeDir(baseHomeDir);
    }

    public String getBaseHomeDir() {
        return environments.getBaseHomeDir();
    }

    public void setBaseDataDir(String baseDataDir) {
        LOGGER.warn("baseDataDir property in server configuration is deprecated");
        environments.setBaseDataDir(baseDataDir);
    }

    public void setBaseDataDir(File baseDataDir) {
        LOGGER.warn("baseDataDir property in server configuration is deprecated");
        environments.setBaseDataDir(baseDataDir);
    }

    public String getBaseDataDir() {
        return environments.getBaseDataDir();
    }

    public void environments(Action<TeamCityEnvironments> configuration) {
        LOGGER.warn("environments configuration in server configuration is deprecated");
        configuration.execute(environments);
    }

    public TeamCityEnvironment getByName(String name) {
        return environments.getByName(name);
    }

    public NamedDomainObjectProvider<TeamCityEnvironment> named(String name) {
        return environments.named(name);
    }

    public TeamCityEnvironment create(String name, Action<TeamCityEnvironment> action) {
        return environments.create(name, action);
    }

    public NamedDomainObjectProvider<TeamCityEnvironment> register(String name, Action<TeamCityEnvironment> action) {
        return environments.register(name, action);
    }
}
