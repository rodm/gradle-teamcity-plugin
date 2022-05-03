/*
 * Copyright 2022 Rod MacKenzie
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

/**
 * Docker options
 */
public class DockerOptions {

    private String serverImage;
    private String agentImage;
    private String serverName;
    private String agentName;

    public String getServerImage() {
        return serverImage;
    }

    public void setServerImage(String serverImage) {
        this.serverImage = serverImage;
    }

    public String getAgentImage() {
        return agentImage;
    }

    public void setAgentImage(String agentImage) {
        this.agentImage = agentImage;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }
}
