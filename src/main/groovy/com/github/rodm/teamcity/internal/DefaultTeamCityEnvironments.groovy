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
package com.github.rodm.teamcity.internal

import com.github.rodm.teamcity.TeamCityEnvironment
import com.github.rodm.teamcity.TeamCityEnvironments
import groovy.transform.CompileStatic
import org.gradle.api.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

import static groovy.transform.TypeCheckingMode.SKIP

@CompileStatic
class DefaultTeamCityEnvironments implements TeamCityEnvironments {

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

    final Project project
    final NamedDomainObjectContainer<TeamCityEnvironment> environments

    DefaultTeamCityEnvironments(Project project) {
        this.project = project
        this.baseDownloadUrl = project.objects.property(String).convention(DEFAULT_BASE_DOWNLOAD_URL)
        this.downloadsDir = project.objects.property(String).convention(DEFAULT_DOWNLOADS_DIR)
        this.baseHomeDir = project.objects.property(String).convention(dir(DEFAULT_BASE_HOME_DIR))
        this.baseDataDir = project.objects.property(String).convention(dir(DEFAULT_BASE_DATA_DIR))
        def factory = new NamedDomainObjectFactory<TeamCityEnvironment>() {
            @Override
            TeamCityEnvironment create(String name) {
                return new DefaultTeamCityEnvironment(name, DefaultTeamCityEnvironments.this, project.objects)
            }
        } as NamedDomainObjectFactory<TeamCityEnvironment>
        this.environments = project.container(TeamCityEnvironment, factory)
    }

    /**
     * The downloads directory that TeamCity distributions are saved to by the download task. Defaults to "downloads".
     */
    String getDownloadsDir() {
        return downloadsDir.get()
    }

    void setDownloadsDir(String downloadsDir) {
        this.downloadsDir.set(downloadsDir)
    }

    Provider<String> getDefaultDownloadsDir() {
        return gradleProperty(DOWNLOADS_DIR_PROPERTY).orElse(downloadsDir)
    }

    /**
     * The base download URL used to download TeamCity distributions. Defaults to "https://download.jetbrains.com/teamcity"
     */
    String getBaseDownloadUrl() {
        return baseDownloadUrl.get()
    }

    void setBaseDownloadUrl(String baseDownloadUrl) {
        this.baseDownloadUrl.set(baseDownloadUrl)
    }

    Provider<String> getDefaultBaseDownloadUrl() {
        return gradleProperty(BASE_DOWNLOAD_URL_PROPERTY).orElse(baseDownloadUrl)
    }

    /**
     * The base home directory used to install TeamCity distributions. Defaults to "servers"
     */
    String getBaseHomeDir() {
        return baseHomeDir.get()
    }

    void setBaseHomeDir(String baseHomeDir) {
        this.baseHomeDir.set(project.layout.projectDirectory.dir(baseHomeDir).toString())
    }

    void setBaseHomeDir(File baseHomeDir) {
        this.baseHomeDir.set(baseHomeDir.absolutePath)
    }

    Provider<String> getDefaultBaseHomeDir() {
        return gradleProperty(BASE_HOME_DIR_PROPERTY).orElse(baseHomeDir)
    }

    /**
     * The base data directory used to store TeamCity configurations. Defaults to "data"
     */
    String getBaseDataDir() {
        return baseDataDir.get()
    }

    void setBaseDataDir(String baseDataDir) {
        this.baseDataDir.set(project.layout.projectDirectory.dir(baseDataDir).toString())
    }

    void setBaseDataDir(File baseDataDir) {
        this.baseDataDir.set(baseDataDir.absolutePath)
    }

    Provider<String> getDefaultBaseDataDir() {
        return gradleProperty(BASE_DATA_DIR_PROPERTY).orElse(baseDataDir)
    }

    TeamCityEnvironment getByName(String name) {
        return environments.getByName(name)
    }

    NamedDomainObjectProvider<TeamCityEnvironment> named(String name) throws UnknownDomainObjectException {
        return environments.named(name)
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

    Provider<String> gradleProperty(String name) {
        def callable = { project.findProperty(name) ?: null }
        return project.<String>provider(callable)
    }

    private String dir(String path) {
        project.layout.projectDirectory.dir(path).toString()
    }
}
