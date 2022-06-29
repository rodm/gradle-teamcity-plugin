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
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.rodm.teamcity.internal.DockerTask;
import org.gradle.api.tasks.TaskAction;

public abstract class StopDockerServer extends DockerTask {

    public StopDockerServer() {
        setDescription("Stops the TeamCity Server using Docker");
    }

    @TaskAction
    void stopServer() {
        DockerClient client = getDockerClient();
        String containerId = getContainerName().get();
        try {
            StopContainerCmd stopContainer = client.stopContainerCmd(containerId);
            stopContainer.exec();
            getLogger().info("TeamCity Server container stopped");
        }
        catch (NotModifiedException e) {
            getLogger().info("TeamCity Server container is already stopped");
        }
        catch (NotFoundException e) {
            getLogger().info("TeamCity Server container '{}' not found", containerId);
        }
    }
}
