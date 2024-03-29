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
package com.github.rodm.teamcity.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.gradle.api.GradleException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DockerOperations {

    public static final String IMAGE_NOT_AVAILABLE = "Docker image '%s' not available. Please use docker pull to download this image";

    private final DockerClient client;

    public DockerOperations() {
        DefaultDockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(clientConfig.getDockerHost())
            .build();
        client = DockerClientImpl.getInstance(clientConfig, httpClient);
    }

    @SuppressWarnings("unused")
    DockerOperations(DockerClient client) {
        this.client = client;
    }

    public boolean isImageAvailable(String image) {
        try {
            client.inspectImageCmd(image).exec();
            return true;
        }
        catch (NotFoundException e) {
            return false;
        }
    }

    public String createContainer(ContainerConfiguration configuration) {
        List<Bind> binds = configuration.getBinds().
            stream()
            .map(Bind::parse)
            .collect(Collectors.toList());
        List<PortBinding> portBindings = configuration.getPortBindings()
            .stream()
            .map(PortBinding::parse)
            .collect(Collectors.toList());

        HostConfig hostConfig = HostConfig.newHostConfig()
            .withAutoRemove(configuration.getAutoRemove())
            .withBinds(binds)
            .withPortBindings(portBindings);

        List<ExposedPort> exposedPorts = configuration.getExposedPorts()
            .stream()
            .map(ExposedPort::parse)
            .collect(Collectors.toList());

        try (CreateContainerCmd createContainer = client.createContainerCmd(configuration.getImage())) {
            createContainer.withName(configuration.getName())
                .withHostConfig(hostConfig)
                .withEnv(configuration.getEnvironment())
                .withExposedPorts(exposedPorts);

            CreateContainerResponse response = createContainer.exec();
            return response.getId();
        }
    }

    public boolean isContainerAvailable(String containerId) {
        try {
            client.inspectContainerCmd(containerId).exec();
            return true;
        }
        catch (NotFoundException e) {
            return false;
        }
    }

    public boolean isContainerRunning(String containerId) {
        try {
            InspectContainerCmd inspectContainer = client.inspectContainerCmd(containerId);
            InspectContainerResponse inspectResponse = inspectContainer.exec();
            return Boolean.TRUE.equals(inspectResponse.getState().getRunning());
        }
        catch (NotFoundException e) {
            return false;
        }
    }

    public void startContainer(String containerId) {
        StartContainerCmd startContainer = client.startContainerCmd(containerId);
        try {
            startContainer.exec();
        }
        catch (NotModifiedException e) {
            // ignore - already started
        }
    }

    public void stopContainer(String containerId) {
        StopContainerCmd stopContainer = client.stopContainerCmd(containerId);
        try {
            stopContainer.exec();
        }
        catch (NotModifiedException e) {
            // ignore - already stopped
        }
    }

    public String getIpAddress(String containerId) {
        InspectContainerCmd inspectContainer = client.inspectContainerCmd(containerId);
        InspectContainerResponse inspectResponse = inspectContainer.exec();
        Map<String, ContainerNetwork> networks = inspectResponse.getNetworkSettings().getNetworks();
        return networks.values().stream()
            .map(ContainerNetwork::getIpAddress)
            .filter(ipAddress -> ipAddress != null && !ipAddress.isEmpty())
            .findFirst()
            .orElseThrow(() -> new GradleException("Failed to get IP address for container: " + containerId));
    }
}
