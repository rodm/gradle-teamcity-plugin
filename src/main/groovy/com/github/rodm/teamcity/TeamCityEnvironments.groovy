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

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

interface TeamCityEnvironments {

    /**
     * The downloads directory that TeamCity distributions are saved to by the download task. Defaults to "downloads".
     */
    Property<String> getDownloadsDir()
    void setDownloadsDir(String downloadsDir)
    Provider<String> defaultDownloadsDir()

    /**
     * The base download URL used to download TeamCity distributions. Defaults to "https://download.jetbrains.com/teamcity"
     */
    Property<String> getBaseDownloadUrl()
    void setBaseDownloadUrl(String baseDownloadUrl)
    Provider<String> defaultBaseDownloadUrl()

    /**
     * The base home directory used to install TeamCity distributions. Defaults to "servers"
     */
    Property<String> getBaseHomeDir()
    void setBaseHomeDir(String baseHomeDir)
    void setBaseHomeDir(File baseHomeDir)
    Provider<String> defaultBaseHomeDir()

    /**
     * The base data directory used to store TeamCity configurations. Defaults to "data"
     */
    Property<String> getBaseDataDir()
    void setBaseDataDir(String baseDataDir)
    void setBaseDataDir(File baseDataDir)
    Provider<String> defaultBaseDataDir()

    // methods to create and access TeamCityEnvironments
    TeamCityEnvironment getByName(String name)
    NamedDomainObjectProvider<TeamCityEnvironment> named(String name) throws UnknownDomainObjectException
    TeamCityEnvironment create(String name, Action<TeamCityEnvironment> action) throws InvalidUserDataException
    NamedDomainObjectProvider<TeamCityEnvironment> register(String name, Action<TeamCityEnvironment> action) throws InvalidUserDataException
}
