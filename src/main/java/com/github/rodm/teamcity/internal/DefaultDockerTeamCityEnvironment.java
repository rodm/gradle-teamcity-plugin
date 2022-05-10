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
package com.github.rodm.teamcity.internal;

import com.github.rodm.teamcity.BaseTeamCityEnvironment;
import com.github.rodm.teamcity.DockerTeamCityEnvironment;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public class DefaultDockerTeamCityEnvironment extends BaseTeamCityEnvironment implements DockerTeamCityEnvironment {

    private final Property<String> serverImage;
    private final Property<String> agentImage;
    private final Property<String> serverName;
    private final Property<String> agentName;

    @Inject
    public DefaultDockerTeamCityEnvironment(String name, DefaultTeamCityEnvironments environments, ObjectFactory factory) {
        super(name, environments, factory);
        this.serverImage = factory.property(String.class).convention("jetbrains/teamcity-server");
        this.agentImage = factory.property(String.class).convention("jetbrains/teamcity-agent");
        this.serverName = factory.property(String.class).convention("teamcity-server");
        this.agentName = factory.property(String.class).convention("teamcity-agent");
    }

    public String getServerImage() {
        return serverImage.get();
    }

    public void setServerImage(String serverImage) {
        this.serverImage.set(serverImage);
    }

    public String getAgentImage() {
        return agentImage.get();
    }

    public void setAgentImage(String agentImage) {
        this.agentImage.set(agentImage);
    }

    public String getServerName() {
        return serverName.get();
    }

    public void setServerName(String serverName) {
        this.serverName.set(serverName);
    }

    public String getAgentName() {
        return agentName.get();
    }

    public void setAgentName(String agentName) {
        this.agentName.set(agentName);
    }
}
