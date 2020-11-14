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
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

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

    private Property<String> baseDownloadUrl
    private Property<String> downloadsDir
    private Property<String> baseHomeDir
    private Property<String> baseDataDir

    final NamedDomainObjectContainer<TeamCityEnvironment> environments

    Project project

    TeamCityEnvironments(Project project) {
        this.project = project
        this.baseDownloadUrl = project.objects.property(String).convention(DEFAULT_BASE_DOWNLOAD_URL)
        this.downloadsDir = project.objects.property(String).convention(DEFAULT_DOWNLOADS_DIR)
        def defaultHomePath = project.layout.projectDirectory.dir(DEFAULT_BASE_HOME_DIR).toString()
        this.baseHomeDir = project.objects.property(String).convention(defaultHomePath)
        def defaultDataPath = project.layout.projectDirectory.dir(DEFAULT_BASE_DATA_DIR).toString()
        this.baseDataDir = project.objects.property(String).convention(defaultDataPath)
        NamedDomainObjectFactory<TeamCityEnvironment> factory = new NamedDomainObjectFactory<TeamCityEnvironment>() {
            @Override
            TeamCityEnvironment create(String name) {
                return new TeamCityEnvironment(name, TeamCityEnvironments.this, project.objects)
            }
        }
        this.environments = project.container(TeamCityEnvironment, factory)
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
    Property<String> getDownloadsDir() {
        return downloadsDir
    }

    void setDownloadsDir(String downloadsDir) {
        this.downloadsDir.set(downloadsDir)
    }

    Provider<String> defaultDownloadsDir() {
        return gradleProperty(DOWNLOADS_DIR_PROPERTY).orElse(downloadsDir)
    }

    /**
     * The base download URL used to download TeamCity distributions. Defaults to "https://download.jetbrains.com/teamcity"
     */
    Property<String> getBaseDownloadUrl() {
        return baseDownloadUrl
    }

    void setBaseDownloadUrl(String baseDownloadUrl) {
        this.baseDownloadUrl.set(baseDownloadUrl)
    }

    Provider<String> defaultBaseDownloadUrl() {
        return gradleProperty(BASE_DOWNLOAD_URL_PROPERTY).orElse(baseDownloadUrl)
    }

    /**
     * The base home directory used to install TeamCity distributions. Defaults to "servers"
     */
    Property<String> getBaseHomeDir() {
        return baseHomeDir
    }

    void setBaseHomeDir(String baseHomeDir) {
        this.baseHomeDir.set(project.layout.projectDirectory.dir(baseHomeDir).toString())
    }

    void setBaseHomeDir(File baseHomeDir) {
        this.baseHomeDir.set(baseHomeDir.absolutePath)
    }

    Provider<String> defaultBaseHomeDir() {
        return gradleProperty(BASE_HOME_DIR_PROPERTY).orElse(baseHomeDir)
    }

    /**
     * The base data directory used to store TeamCity configurations. Defaults to "data"
     */
    Property<String> getBaseDataDir() {
        return baseDataDir
    }

    void setBaseDataDir(String baseDataDir) {
        this.baseDataDir.set(project.layout.projectDirectory.dir(baseDataDir).toString())
    }

    void setBaseDataDir(File baseDataDir) {
        this.baseDataDir.set(baseDataDir.absolutePath)
    }

    Provider<String> defaultBaseDataDir() {
        return gradleProperty(BASE_DATA_DIR_PROPERTY).orElse(baseDataDir)
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

    private Provider<String> gradleProperty(String name) {
        final Project project = this.project
        def callable = { project.findProperty(name) ?: null }
        return project.<String>provider(callable)
    }
}
