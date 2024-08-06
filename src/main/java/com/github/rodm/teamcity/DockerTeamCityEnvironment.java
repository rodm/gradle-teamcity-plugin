/*
 * Copyright 2022 the original author or authors.
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

public interface DockerTeamCityEnvironment extends TeamCityEnvironment {

    /**
     * The name of the TeamCity Server Docker image.
     *
     * @return the Docker image name
     */
    String getServerImage();
    void setServerImage(String serverImage);

    /**
     * The tag of the TeamCity Server Docker image.
     *
     * @return the Docker image tag
     */
    String getServerTag();
    void setServerTag(String serverTag);

    /**
     * The name of the TeamCity Build Agent Docker image.
     *
     * @return the Docker image name
     */
    String getAgentImage();
    void setAgentImage(String agentImage);

    /**
     * The tag of the TeamCity Build Agent Docker image.
     *
     * @return the Docker image tag
     */
    String getAgentTag();
    void setAgentTag(String agentTag);

    /**
     * The name of the TeamCity Server container.
     *
     * @return the container name
     */
    String getServerName();
    void setServerName(String serverName);

    /**
     * The name of the TeamCity Build Agent container.
     *
     * @return the container name
     */
    String getAgentName();
    void setAgentName(String agentName);

    /**
     * The port the TeamCity Server is accessible on.
     *
     * @return the server port
     */
    String getPort();
    void setPort(String port);
}
