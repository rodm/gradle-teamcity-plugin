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
package com.github.rodm.teamcity.internal;

import com.github.rodm.teamcity.TeamCityEnvironment;
import com.github.rodm.teamcity.TeamCityEnvironments;
import groovy.lang.Closure;
import groovy.lang.MissingMethodException;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.concurrent.Callable;

public class DefaultTeamCityEnvironments implements TeamCityEnvironments {

    public static final String DOWNLOADS_DIR_PROPERTY = "teamcity.environments.downloadsDir";
    public static final String BASE_DOWNLOAD_URL_PROPERTY = "teamcity.environments.baseDownloadUrl";
    public static final String BASE_DATA_DIR_PROPERTY = "teamcity.environments.baseDataDir";
    public static final String BASE_HOME_DIR_PROPERTY = "teamcity.environments.baseHomeDir";

    public static final String DEFAULT_DOWNLOADS_DIR = "downloads";
    public static final String DEFAULT_BASE_DOWNLOAD_URL = "https://download.jetbrains.com/teamcity";
    public static final String DEFAULT_BASE_DATA_DIR = "data";
    public static final String DEFAULT_BASE_HOME_DIR = "servers";

    private Property<String> baseDownloadUrl;
    private Property<String> downloadsDir;
    private Property<String> baseHomeDir;
    private Property<String> baseDataDir;

    private final Project project;
    private final NamedDomainObjectContainer<TeamCityEnvironment> environments;

    public DefaultTeamCityEnvironments(final Project project) {
        this.project = project;
        this.baseDownloadUrl = project.getObjects().property(String.class).convention(DEFAULT_BASE_DOWNLOAD_URL);
        this.downloadsDir = project.getObjects().property(String.class).convention(DEFAULT_DOWNLOADS_DIR);
        this.baseHomeDir = project.getObjects().property(String.class).convention(dir(DEFAULT_BASE_HOME_DIR));
        this.baseDataDir = project.getObjects().property(String.class).convention(dir(DEFAULT_BASE_DATA_DIR));
        NamedDomainObjectFactory<TeamCityEnvironment> factory = name ->
            new DefaultTeamCityEnvironment(name, DefaultTeamCityEnvironments.this, project.getObjects());
        this.environments = project.container(TeamCityEnvironment.class, factory);
    }

    /**
     * The downloads directory that TeamCity distributions are saved to by the download task. Defaults to "downloads".
     */
    public String getDownloadsDir() {
        return downloadsDir.get();
    }

    public void setDownloadsDir(String downloadsDir) {
        this.downloadsDir.set(downloadsDir);
    }

    public Provider<String> getDefaultDownloadsDir() {
        return gradleProperty(DOWNLOADS_DIR_PROPERTY).orElse(downloadsDir);
    }

    /**
     * The base download URL used to download TeamCity distributions. Defaults to "https://download.jetbrains.com/teamcity"
     */
    public String getBaseDownloadUrl() {
        return baseDownloadUrl.get();
    }

    public void setBaseDownloadUrl(String baseDownloadUrl) {
        this.baseDownloadUrl.set(baseDownloadUrl);
    }

    public Provider<String> getDefaultBaseDownloadUrl() {
        return gradleProperty(BASE_DOWNLOAD_URL_PROPERTY).orElse(baseDownloadUrl);
    }

    /**
     * The base home directory used to install TeamCity distributions. Defaults to "servers"
     */
    public String getBaseHomeDir() {
        return baseHomeDir.get();
    }

    public void setBaseHomeDir(String baseHomeDir) {
        this.baseHomeDir.set(project.getLayout().getProjectDirectory().dir(baseHomeDir).toString());
    }

    public void setBaseHomeDir(File baseHomeDir) {
        this.baseHomeDir.set(baseHomeDir.getAbsolutePath());
    }

    public Provider<String> getDefaultBaseHomeDir() {
        return gradleProperty(BASE_HOME_DIR_PROPERTY).orElse(baseHomeDir);
    }

    /**
     * The base data directory used to store TeamCity configurations. Defaults to "data"
     */
    public String getBaseDataDir() {
        return baseDataDir.get();
    }

    public void setBaseDataDir(String baseDataDir) {
        this.baseDataDir.set(project.getLayout().getProjectDirectory().dir(baseDataDir).toString());
    }

    public void setBaseDataDir(File baseDataDir) {
        this.baseDataDir.set(baseDataDir.getAbsolutePath());
    }

    public Provider<String> getDefaultBaseDataDir() {
        return gradleProperty(BASE_DATA_DIR_PROPERTY).orElse(baseDataDir);
    }

    public TeamCityEnvironment getByName(String name) {
        return environments.getByName(name);
    }

    public NamedDomainObjectProvider<TeamCityEnvironment> named(String name) throws UnknownDomainObjectException {
        return environments.named(name);
    }

    public TeamCityEnvironment create(String name, Action<TeamCityEnvironment> action) throws InvalidUserDataException {
        return environments.create(name, action);
    }

    public NamedDomainObjectProvider<TeamCityEnvironment> register(String name, Action<TeamCityEnvironment> action) throws InvalidUserDataException {
        return environments.register(name, action);
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    public TeamCityEnvironment methodMissing(String name, Object arg) {
        Object[] args = (Object[]) arg;
        if (args.length == 1 && args[0] instanceof Closure) {
            Closure configuration = (Closure) args[0];
            return environments.create(name, configuration);
        } else {
            throw new MissingMethodException(name, getClass(), args);
        }
    }

    public Provider<String> gradleProperty(final String name) {
        Callable<String> callable = () ->
            project.findProperty(name) == null ? null : (String) project.property(name);
        return project.provider(callable);
    }

    public final NamedDomainObjectContainer<TeamCityEnvironment> getEnvironments() {
        return environments;
    }

    private String dir(String path) {
        return project.getLayout().getProjectDirectory().dir(path).toString();
    }
}
