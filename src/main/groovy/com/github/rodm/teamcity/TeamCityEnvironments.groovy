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
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException

import static groovy.transform.TypeCheckingMode.SKIP

@CompileStatic
class TeamCityEnvironments {

    public static final String DOWNLOADS_DIR_PROPERTY = 'teamcity.environments.downloadsDir'
    public static final String BASE_DOWNLOAD_URL_PROPERTY = 'teamcity.environments.baseDownloadUrl'
    public static final String BASE_DATA_DIR_PROPERTY = 'teamcity.environments.baseDataDir'
    public static final String BASE_HOME_DIR_PROPERTY = 'teamcity.environments.baseHomeDir'

    public static final String DEFAULT_DOWNLOADS_DIR = 'downloads'
    public static final String DEFAULT_BASE_DOWNLOAD_URL = 'https://download.jetbrains.com/teamcity'
    public static final String DEFAULT_BASE_DATA_DIR = 'data'
    public static final String DEFAULT_BASE_HOME_DIR = 'servers'

    private String downloadsDir

    private String baseDownloadUrl

    private String baseDataDir

    private String baseHomeDir

    final NamedDomainObjectContainer<TeamCityEnvironment> environments

    Project project

    TeamCityEnvironments(Project project) {
        this.project = project
        this.environments = project.container(TeamCityEnvironment)
    }

    TeamCityEnvironment getByName(String name) {
        return environments.getByName(name)
    }

    NamedDomainObjectProvider<TeamCityEnvironment> named(String name) throws UnknownDomainObjectException {
        return environments.named(name)
    }

    /**
     * The downloads directory that TeamCity distributions are saved to by the download task. Defaults to "downloads".
     */
    String getDownloadsDir() {
        return property(DOWNLOADS_DIR_PROPERTY, downloadsDir, DEFAULT_DOWNLOADS_DIR)
    }

    void setDownloadsDir(String downloadsDir) {
        this.downloadsDir = downloadsDir
    }

    /**
     * The base download URL used to download TeamCity distributions. Defaults to "https://download.jetbrains.com/teamcity"
     */
    String getBaseDownloadUrl() {
        return property(BASE_DOWNLOAD_URL_PROPERTY, baseDownloadUrl, DEFAULT_BASE_DOWNLOAD_URL)
    }

    void setBaseDownloadUrl(String baseDownloadUrl) {
        this.baseDownloadUrl = baseDownloadUrl
    }

    /**
     * The base data directory used to store TeamCity configurations. Defaults to "data"
     */
    String getBaseDataDir() {
        return property(BASE_DATA_DIR_PROPERTY, baseDataDir, DEFAULT_BASE_DATA_DIR)
    }

    void setBaseDataDir(String baseDataDir) {
        this.baseDataDir = baseDataDir
    }

    /**
     * The base home directory used to install TeamCity distributions. Defaults to "servers"
     */
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

    TeamCityEnvironment create(String name, Action<TeamCityEnvironment> action) throws InvalidUserDataException {
        return environments.create(name, action)
    }

    NamedDomainObjectProvider<TeamCityEnvironment> register(String name, Action<TeamCityEnvironment> action) throws InvalidUserDataException {
        return environments.register(name, action)
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    @CompileStatic(SKIP)
    methodMissing(String name, args) {
        if (args.length == 1 && args[0] instanceof Closure) {
            Closure configuration = args[0]
            environments.create(name, configuration)
        } else {
            throw new MissingMethodException(name, getClass(), args)
        }
    }
}
