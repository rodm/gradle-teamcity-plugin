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
package com.github.rodm.teamcity.tasks;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.rodm.teamcity.internal.DockerTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.github.rodm.teamcity.internal.DockerSupport.getDebugPort;

public abstract class StartDockerAgent extends DockerTask {

    public StartDockerAgent() {
        setDescription("Starts the TeamCity Agent using Docker");
    }

    @Input
    public abstract Property<String> getVersion();

    @Input
    public abstract Property<String> getDataDir();

    @Input
    public abstract Property<String> getAgentOptions();

    @Input
    public abstract Property<String> getImageName();

    @Input
    public abstract Property<String> getServerContainerName();

    @Input
    public abstract Property<String> getServerPort();

    @TaskAction
    void startAgent() {
        DockerClient client = getDockerClient();

        String image = getImageName().get() + ":" + getVersion().get();
        try {
            client.inspectImageCmd(image).exec();
        }
        catch (NotFoundException e) {
            String message = String.format("Docker image '%s' not available. Please use docker pull to download this image", image);
            throw new GradleException(message);
        }

        String containerId = getContainerName().get();
        try {
            InspectContainerCmd inspectContainer = client.inspectContainerCmd(containerId);
            InspectContainerResponse inspectResponse = inspectContainer.exec();
            if (Boolean.TRUE.equals(inspectResponse.getState().getRunning())) {
                getLogger().info("TeamCity Build Agent container '{}' is already running", containerId);
                return;
            }
        }
        catch (NotFoundException e) {
            // ignore
        }

        List<PortBinding> portBindings = new ArrayList<>();
        getDebugPort(getAgentOptions().get())
            .ifPresent(port -> portBindings.add(PortBinding.parse(port + ":" + port)));

        Bind configDir = Bind.parse(getDataDir().get() + "/agent/conf:/data/teamcity_agent/conf");
        HostConfig hostConfig = HostConfig.newHostConfig()
            .withAutoRemove(true)
            .withBinds(configDir)
            .withPortBindings(portBindings);

        CreateContainerCmd createContainer = client.createContainerCmd(image)
            .withName(getContainerName().get())
            .withEnv("SERVER_URL=http://" + getIpAddress(client) + ":" + getServerPort().get() + "/")
            .withEnv("TEAMCITY_AGENT_OPTS=" + getAgentOptions().get())
            .withHostConfig(hostConfig);

        CreateContainerResponse response = createContainer.exec();
        getLogger().info("Created TeamCity Build Agent container with id: {}", response.getId());

        StartContainerCmd startContainer = client.startContainerCmd(getContainerName().get());
        startContainer.exec();
        getLogger().info("Started TeamCity Build Agent container");
    }

    private String getIpAddress(DockerClient client) {
        InspectContainerCmd inspectContainer = client.inspectContainerCmd(getServerContainerName().get());
        InspectContainerResponse inspectResponse = inspectContainer.exec();
        Map<String, ContainerNetwork> networks = inspectResponse.getNetworkSettings().getNetworks();
        return networks.values().stream()
            .findFirst()
            .map(ContainerNetwork::getIpAddress)
            .orElseThrow(() -> new GradleException("Failed to get IP address for TeamCity Server"));
    }
}
