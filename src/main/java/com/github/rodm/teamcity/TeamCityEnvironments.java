/*
 * Copyright 2016 Rod MacKenzie
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

import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.plugins.ExtensionAware;

import java.io.File;

public interface TeamCityEnvironments extends ExtensionAware {

    /**
     * The downloads directory that TeamCity distributions are saved to by the download task. Defaults to "downloads".
     *
     * @return the downloads directory
     */
    String getDownloadsDir();
    void setDownloadsDir(String downloadsDir);

    /**
     * The base download URL used to download TeamCity distributions. Defaults to "https://download.jetbrains.com/teamcity"
     *
     * @return the base download URL
     */
    String getBaseDownloadUrl();
    void setBaseDownloadUrl(String baseDownloadUrl);

    /**
     * The base home directory used to install TeamCity distributions. Defaults to "servers"
     *
     * @return the base directory
     */
    String getBaseHomeDir();
    void setBaseHomeDir(String baseHomeDir);
    void setBaseHomeDir(File baseHomeDir);

    /**
     * The base data directory used to store TeamCity configurations. Defaults to "data"
     *
     * @return the base data directory
     */
    String getBaseDataDir();
    void setBaseDataDir(String baseDataDir);
    void setBaseDataDir(File baseDataDir);

    // methods to create and access TeamCityEnvironments
    TeamCityEnvironment getByName(String name);
    NamedDomainObjectProvider<TeamCityEnvironment> named(String name) throws UnknownDomainObjectException;
    LocalTeamCityEnvironment create(String name, Action<LocalTeamCityEnvironment> action) throws InvalidUserDataException;
    NamedDomainObjectProvider<LocalTeamCityEnvironment> register(String name, Action<LocalTeamCityEnvironment> action) throws InvalidUserDataException;

    <T extends TeamCityEnvironment> T create(String name, Class<T> type, Action<? super T> action) throws InvalidUserDataException;
    <T extends TeamCityEnvironment> NamedDomainObjectProvider<T> register(String name, Class<T> type, Action<? super T> action) throws InvalidUserDataException;
    <T extends TeamCityEnvironment> void registerFactory(Class<T> type, NamedDomainObjectFactory<T> factory);

    Class<LocalTeamCityEnvironment> Local = LocalTeamCityEnvironment.class;
    Class<DockerTeamCityEnvironment> Docker = DockerTeamCityEnvironment.class;
}
