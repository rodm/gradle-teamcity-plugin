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

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import static com.github.rodm.teamcity.docker.DockerSupport.getDebugPort;
import static com.github.rodm.teamcity.docker.DockerOperations.IMAGE_NOT_AVAILABLE;
import static java.lang.String.format;

public abstract class StartAgentContainerAction implements WorkAction<StartAgentContainerAction.StartAgentParameters> {

    private static final Logger LOGGER = Logging.getLogger(StartAgentContainerAction.class);

    public interface StartAgentParameters extends WorkParameters {
        Property<String> getContainerName();
        Property<String> getVersion();
        Property<String> getDataDir();
        Property<String> getAgentOptions();
        Property<String> getImageName();
        Property<String> getServerContainerName();
    }

    @Override
    public void execute() {
        final StartAgentParameters parameters = getParameters();
        final DockerOperations dockerOperations = new DockerOperations();

        String image = parameters.getImageName().get() + ":" + parameters.getVersion().get();
        if (!dockerOperations.isImageAvailable(image)) {
            throw new GradleException(format(IMAGE_NOT_AVAILABLE, image));
        }

        String containerId = parameters.getContainerName().get();
        if (dockerOperations.isContainerRunning(containerId)) {
            LOGGER.info("TeamCity Build Agent container '{}' is already running", containerId);
            return;
        }

        String serverContainerId = parameters.getServerContainerName().get();
        if (!dockerOperations.isContainerRunning(serverContainerId)) {
            throw new GradleException(format("TeamCity Server container '%s' is not running", serverContainerId));
        }

        String ipAddress = dockerOperations.getIpAddress(parameters.getServerContainerName().get());
        String serverUrl = "http://" + ipAddress + ":8111/";
        String agentOptions = parameters.getAgentOptions().get();
        ContainerConfiguration configuration = ContainerConfiguration.builder()
            .image(image)
            .name(containerId)
            .autoRemove()
            .bind(parameters.getDataDir().get() + "/agent/conf", "/data/teamcity_agent/conf")
            .environment("SERVER_URL", serverUrl)
            .environment("TEAMCITY_AGENT_OPTS", agentOptions);
        getDebugPort(agentOptions).ifPresent(debugPort -> configuration
            .bindPort(debugPort, debugPort)
            .exposePort(debugPort));

        String id = dockerOperations.createContainer(configuration);
        LOGGER.info("Created TeamCity Build Agent container with id: {}", id);

        dockerOperations.startContainer(containerId);
        LOGGER.info("Started TeamCity Build Agent container");
    }
}
