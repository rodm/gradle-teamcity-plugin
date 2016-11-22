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
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

@CompileStatic
class ServerPluginConfiguration extends PluginConfiguration {

    TeamCityEnvironments environments

    private Project project

    ServerPluginConfiguration(Project project, NamedDomainObjectContainer<TeamCityEnvironment> environments) {
        super(project.copySpec {})
        this.project = project
        this.environments = new TeamCityEnvironments(project)
    }

    def descriptor(Closure closure) {
        descriptor = new ServerPluginDescriptor()
        ConfigureUtil.configure(closure, descriptor)
    }

    String getDownloadsDir() {
        environments.getDownloadsDir()
    }

    void setDownloadsDir(String downloadsDir) {
        environments.setDownloadsDir(downloadsDir)
    }

    String getBaseDownloadUrl() {
        environments.getBaseDownloadUrl()
    }

    void setBaseDownloadUrl(String baseDownloadUrl) {
        environments.setBaseDownloadUrl(baseDownloadUrl)
    }

    String getBaseDataDir() {
        environments.getBaseDataDir()
    }

    void setBaseDataDir(String baseDataDir) {
        environments.setBaseDataDir(baseDataDir)
    }

    String getBaseHomeDir() {
        environments.getBaseHomeDir()
    }

    void setBaseHomeDir(String baseHomeDir) {
        environments.setBaseHomeDir(baseHomeDir)
    }

    def getEnvironments() {
        environments.environments
    }

    void environments(Closure config) {
        ConfigureUtil.configure(config, environments)
    }
}
