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
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.rodm.teamcity.internal.DockerTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.util.ArrayList;
import java.util.List;

import static com.github.rodm.teamcity.internal.DockerSupport.getDebugPort;

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
        DockerClient client = getDockerClient();

        String containerId = getContainerName().get();
        try {
            InspectContainerCmd inspectContainer = client.inspectContainerCmd(containerId);
            InspectContainerResponse inspectResponse = inspectContainer.exec();
            if (Boolean.TRUE.equals(inspectResponse.getState().getRunning())) {
                getLogger().info("TeamCity Server container '{}' is already running", containerId);
                return;
            }
        }
        catch (NotFoundException e) {
            // ignore
        }

        List<PortBinding> portBindings = new ArrayList<>();
        portBindings.add(PortBinding.parse(getPort().get() + ":8111"));
        getDebugPort(getServerOptions().get())
            .ifPresent(port -> portBindings.add(PortBinding.parse(port + ":" + port)));

        Bind dataDir = Bind.parse(getDataDir().get() + ":/data/teamcity_server/datadir");
        Bind logsDir = Bind.parse(getLogsDir().get() + ":/opt/teamcity/logs");
        HostConfig hostConfig = HostConfig.newHostConfig()
            .withAutoRemove(true)
            .withBinds(dataDir, logsDir)
            .withPortBindings(portBindings);

        String image = getImageName().get() + ":" + getVersion().get();
        CreateContainerCmd createContainer = client.createContainerCmd(image)
            .withName(containerId)
            .withHostConfig(hostConfig)
            .withEnv("TEAMCITY_SERVER_OPTS=" + getServerOptions().get());

        CreateContainerResponse response = createContainer.exec();
        getLogger().info("Created TeamCity Server container with id: {}", response.getId());

        StartContainerCmd startContainer = client.startContainerCmd(containerId);
        startContainer.exec();
        getLogger().info("Started TeamCity Server container");
    }
}
