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

public abstract class StartDockerServer extends DockerTask {

    public StartDockerServer() {
        setDescription("Starts the TeamCity Server using Docker");
    }

    @Input
    public abstract Property<String> getVersion();

    @Input
    public abstract Property<String> getDataDir();

    @Input
    public abstract Property<String> getLogsDir();

    @Input
    public abstract Property<String> getServerOptions();

    @Input
    public abstract Property<String> getImageName();

    @Input
    public abstract Property<String> getPort();

    @TaskAction
    void startServer() {
        DockerOperations dockerOperations = new DockerOperations();

        String image = getImageName().get() + ":" + getVersion().get();
        if (!dockerOperations.isImageAvailable(image)) {
            throw new GradleException(format(IMAGE_NOT_AVAILABLE, image));
        }

        String containerId = getContainerName().get();
        if (dockerOperations.isContainerRunning(containerId)) {
            getLogger().info("TeamCity Server container '{}' is already running", containerId);
            return;
        }

        ContainerConfiguration configuration = ContainerConfiguration.builder()
            .image(image)
            .name(getContainerName().get())
            .bind(getDataDir().get(),"/data/teamcity_server/datadir")
            .bind(getLogsDir().get(),"/opt/teamcity/logs")
            .bindPort(getPort().get(), "8111")
            .environment("TEAMCITY_SERVER_OPTS", getServerOptions().get());
        Optional<String> debugPort = getDebugPort(getServerOptions().get());
        debugPort.ifPresent(port -> configuration.bindPort(port, port).exposePort(port));

        String id = dockerOperations.createContainer(configuration);
        getLogger().info("Created TeamCity Server container with id: {}", id);

        dockerOperations.startContainer(containerId);
        getLogger().info("Started TeamCity Server container");
    }
}
