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

import com.github.rodm.teamcity.internal.ContainerConfiguration;
import com.github.rodm.teamcity.internal.DockerOperations;
import com.github.rodm.teamcity.internal.DockerTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.util.Optional;

import static com.github.rodm.teamcity.internal.DockerSupport.getDebugPort;
import static java.lang.String.format;

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
        DockerOperations dockerOperations = new DockerOperations();

        String image = getImageName().get() + ":" + getVersion().get();
        if (!dockerOperations.isImageAvailable(image)) {
            throw new GradleException(format(IMAGE_NOT_AVAILABLE, image));
        }

        String containerId = getContainerName().get();
        if (dockerOperations.isContainerRunning(containerId)) {
            getLogger().info("TeamCity Build Agent container '{}' is already running", containerId);
            return;
        }

        String serverUrl = "http://" + dockerOperations.getIpAddress(getServerContainerName().get()) + ":" + getServerPort().get() + "/";
        ContainerConfiguration configuration = ContainerConfiguration.builder()
            .image(image)
            .name(getContainerName().get())
            .autoRemove()
            .bind(getDataDir().get() + "/agent/conf", "/data/teamcity_agent/conf")
            .environment("SERVER_URL", serverUrl)
            .environment("TEAMCITY_AGENT_OPTS", getAgentOptions().get());
        Optional<String> debugPort = getDebugPort(getAgentOptions().get());
        debugPort.ifPresent(port -> configuration.bindPort(port, port).exposePort(port));

        String id = dockerOperations.createContainer(configuration);
        getLogger().info("Created TeamCity Build Agent container with id: {}", id);

        dockerOperations.startContainer(containerId);
        getLogger().info("Started TeamCity Build Agent container");
    }
}
