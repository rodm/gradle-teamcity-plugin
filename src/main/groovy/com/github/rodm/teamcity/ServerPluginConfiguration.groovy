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
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

@CompileStatic
class ServerPluginConfiguration extends PluginConfiguration {

    @Delegate
    private TeamCityEnvironments environments

    private Project project

    ServerPluginConfiguration(Project project, TeamCityEnvironments environments) {
        super(project.copySpec {})
        this.project = project
        this.environments = environments
    }

    def descriptor(Closure closure) {
        descriptor = new ServerPluginDescriptor()
        ConfigureUtil.configure(closure, descriptor)
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
