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

    public static final String DOWNLOADS_DIR_PROPERTY = 'com.github.rodm.teamcity.downloadsDir'
    public static final String BASE_DOWNLOAD_URL_PROPERTY = 'com.github.rodm.teamcity.baseDownloadUrl'
    public static final String BASE_DATA_DIR_PROPERTY = 'com.github.rodm.teamcity.baseDataDir'
    public static final String BASE_HOME_DIR_PROPERTY = 'com.github.rodm.teamcity.baseHomeDir'

    public static final String DEFAULT_DOWNLOADS_DIR = 'downloads'
    public static final String DEFAULT_BASE_DOWNLOAD_URL = 'http://download.jetbrains.com/teamcity'
    public static final String DEFAULT_BASE_DATA_DIR = 'data'
    public static final String DEFAULT_BASE_HOME_DIR = 'servers'

    private String downloadsDir

    private String baseDownloadUrl

    private String baseDataDir

    private String baseHomeDir

    final NamedDomainObjectContainer<TeamCityEnvironment> environments

    private Project project

    ServerPluginConfiguration(Project project, NamedDomainObjectContainer<TeamCityEnvironment> environments) {
        super(project.copySpec {})
        this.project = project
        this.environments = environments
    }

    def descriptor(Closure closure) {
        descriptor = new ServerPluginDescriptor()
        ConfigureUtil.configure(closure, descriptor)
    }

    String getDownloadsDir() {
        downloadsDir ? downloadsDir : property(DOWNLOADS_DIR_PROPERTY, DEFAULT_DOWNLOADS_DIR)
    }

    String getBaseDownloadUrl() {
        baseDownloadUrl ? baseDownloadUrl : property(BASE_DOWNLOAD_URL_PROPERTY, DEFAULT_BASE_DOWNLOAD_URL)
    }

    String getBaseDataDir() {
        baseDataDir ? baseDataDir : property(BASE_DATA_DIR_PROPERTY, DEFAULT_BASE_DATA_DIR)
    }

    String getBaseHomeDir() {
        baseHomeDir ? baseHomeDir : property(BASE_HOME_DIR_PROPERTY, DEFAULT_BASE_HOME_DIR)
    }

    void environments(Closure config) {
        environments.configure(config)
    }

    private String property(String name, String defaultValue) {
        if (project.hasProperty(name)) {
            return project.property(name)
        }
        return defaultValue
    }
}
