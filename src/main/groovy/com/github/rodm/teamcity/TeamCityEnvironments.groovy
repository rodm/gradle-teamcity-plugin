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
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

class TeamCityEnvironments {

    public static final String DOWNLOADS_DIR_PROPERTY = 'teamcity.environments.downloadsDir'
    public static final String BASE_DOWNLOAD_URL_PROPERTY = 'teamcity.environments.baseDownloadUrl'
    public static final String BASE_DATA_DIR_PROPERTY = 'teamcity.environments.baseDataDir'
    public static final String BASE_HOME_DIR_PROPERTY = 'teamcity.environments.baseHomeDir'

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

    TeamCityEnvironments(Project project) {
        this.project = project
        this.environments = project.container(TeamCityEnvironment)
    }

    TeamCityEnvironment getByName(String name) {
        return environments.getByName(name)
    }

    @CompileStatic
    String getDownloadsDir() {
        return property(DOWNLOADS_DIR_PROPERTY, downloadsDir, DEFAULT_DOWNLOADS_DIR)
    }

    void setDownloadsDir(String downloadsDir) {
        this.downloadsDir = downloadsDir
    }

    @CompileStatic
    String getBaseDownloadUrl() {
        return property(BASE_DOWNLOAD_URL_PROPERTY, baseDownloadUrl, DEFAULT_BASE_DOWNLOAD_URL)
    }

    void setBaseDownloadUrl(String baseDownloadUrl) {
        this.baseDownloadUrl = baseDownloadUrl
    }

    @CompileStatic
    String getBaseDataDir() {
        return property(BASE_DATA_DIR_PROPERTY, baseDataDir, DEFAULT_BASE_DATA_DIR)
    }

    void setBaseDataDir(String baseDataDir) {
        this.baseDataDir = baseDataDir
    }

    @CompileStatic
    String getBaseHomeDir() {
        return property(BASE_HOME_DIR_PROPERTY, baseHomeDir, DEFAULT_BASE_HOME_DIR)
    }

    void setBaseHomeDir(String baseHomeDir) {
        this.baseHomeDir = baseHomeDir
    }

    private String property(String name, String value, String defaultValue) {
        if (project.hasProperty(name)) {
            return project.property(name)
        }
        return value ? value : defaultValue
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    methodMissing(String name, args) {
        if (args.length == 1 && args[0] instanceof Closure) {
            Closure configuration = args[0]
            environments.create(name, configuration)
        } else {
            throw new MissingMethodException(name, getClass(), args)
        }
    }
}
