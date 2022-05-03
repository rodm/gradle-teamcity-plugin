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

public interface TeamCityEnvironment {

    /**
     * The version of TeamCity this environment uses. Defaults to version '9.0'
     *
     * @return the TeamCity version
     */
    String getVersion();
    void setVersion(String version);

    /**
     * The download URL used to download the TeamCity distribution for this environment.
     *
     * @return the download URL
     */
    String getDownloadUrl();
    void setDownloadUrl(String downloadUrl);

    /**
     * The home directory for this environment's TeamCity installation.
     *
     * @return the home directory
     */
    String getHomeDir();
    void setHomeDir(String homeDir);

    /**
     * The data directory for this environment's TeamCity configuration.
     *
     * @return the data directory
     */
    String getDataDir();
    void setDataDir(String dataDir);

    /**
     * The Java home directory used to start the server and agent for this environment.
     *
     * @return the Java home directory
     */
    String getJavaHome();
    void setJavaHome(String javaHome);

    /**
     * The list of plugins to be deployed to this environment.
     *
     * @return the list of plugins
     */
    Object getPlugins();
    void setPlugins(Object plugins);
    void plugins(Object plugin);

    /**
     * The Java command line options to be used when starting the TeamCity Server.
     * Defaults to
     *      '-Dteamcity.development.mode=true'
     *      '-Dteamcity.development.shadowCopyClasses=true'
     *      '-Dteamcity.superUser.token.saveToFile=true'
     *      '-Dteamcity.kotlinConfigsDsl.generateDslDocs=false'
     *
     * @return the list of server options
     */
    Object getServerOptions();
    void setServerOptions(Object options);
    void serverOptions(String... options);

    /**
     * The Java command line options to be used when starting the TeamCity Agent.
     *
     * @return the list of agent options
     */
    Object getAgentOptions();
    void setAgentOptions(Object options);
    void agentOptions(String... options);

    // Convenience accessors for base properties
    String getBaseHomeDir();
    String getBaseDataDir();

    void useDocker();
    void useDocker(Action<DockerOptions> configuration);
    DockerOptions getDockerOptions();
    boolean isDockerEnabled();
}
