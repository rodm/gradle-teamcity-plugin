/*
 * Copyright 2015 Rod MacKenzie
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
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.plugins.ExtensionAware
import org.gradle.util.ConfigureUtil

import static groovy.transform.TypeCheckingMode.SKIP

/**
 * Server-side plugin configuration
 */
@CompileStatic
class ServerPluginConfiguration extends PluginConfiguration {

    private Project project
    private ConfigurableFileCollection web
    private PublishConfiguration publish
    private SignConfiguration sign

    @Delegate
    private TeamCityEnvironments environments

    ServerPluginConfiguration(Project project, TeamCityEnvironments environments) {
        super(project)
        this.project = project
        this.web = project.objects.fileCollection()
        this.environments = environments
    }

    /**
     * Configures the server-side plugin descriptor for the TeamCity plugin.
     *
     * <p>The given action is executed to configure the server-side plugin descriptor.</p>
     *
     * @param configuration The action.
     */
    @CompileStatic(SKIP)
    def descriptor(Action<ServerPluginDescriptor> configuration) {
        if (!descriptor) {
            descriptor = (this as ExtensionAware).extensions.create('descriptor', ServerPluginDescriptor, project)
            descriptor.init()
        }
        configuration.execute(descriptor)
    }

    ConfigurableFileCollection getWeb() {
        return web
    }

    void setWeb(Object web) {
        this.web.setFrom(web)
    }

    void web(Object web) {
        this.web.from(web)
    }

    /**
     * Configures the credentials to publish the plugin to the JetBrains Plugin Repository.
     *
     * <p>The given action is executed to configure the publishing credentials.</p>
     *
     * @param configuration The action.
     */
    def publish(Action<PublishConfiguration> configuration) {
        if (!publish) {
            publish = (this as ExtensionAware).extensions.create('publish', PublishConfiguration, project)
        }
        configuration.execute(publish)
    }

    PublishConfiguration getPublish() {
        return publish
    }

    void sign(Action<SignConfiguration> configuration) {
        if (!sign) {
            sign = (this as ExtensionAware).extensions.create('sign', SignConfiguration, project)
        }
        configuration.execute(sign)
    }

    SignConfiguration getSign() {
        return sign
    }

    void setDownloadsDir(String downloadsDir) {
        project.logger.warn('downloadsDir property in server configuration is deprecated')
        environments.setDownloadsDir(downloadsDir)
    }

    void setBaseDownloadUrl(String baseDownloadUrl) {
        project.logger.warn('baseDownloadUrl property in server configuration is deprecated')
        environments.setBaseDownloadUrl(baseDownloadUrl)
    }

    void setBaseDataDir(String baseDataDir) {
        project.logger.warn('baseDataDir property in server configuration is deprecated')
        environments.setBaseDataDir(baseDataDir)
    }

    void setBaseHomeDir(String baseHomeDir) {
        project.logger.warn('baseHomeDir property in server configuration is deprecated')
        environments.setBaseHomeDir(baseHomeDir)
    }

    void environments(Closure config) {
        project.logger.warn('environments configuration in server configuration is deprecated')
        ConfigureUtil.configure(config, environments)
    }
}
