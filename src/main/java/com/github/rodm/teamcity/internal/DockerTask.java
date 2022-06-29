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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

public abstract class DockerTask extends DefaultTask {

    protected static DockerClient getDockerClient() {
        DefaultDockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(clientConfig.getDockerHost())
            .build();
        return DockerClientImpl.getInstance(clientConfig, httpClient);
    }

    protected static void checkImageAvailable(DockerClient client, String image) {
        try {
            client.inspectImageCmd(image).exec();
        }
        catch (NotFoundException e) {
            String message = String.format("Docker image '%s' not available. Please use docker pull to download this image", image);
            throw new GradleException(message);
        }
    }

    protected static boolean isContainerRunning(DockerClient client, String containerId) {
        try {
            InspectContainerCmd inspectContainer = client.inspectContainerCmd(containerId);
            InspectContainerResponse inspectResponse = inspectContainer.exec();
            return Boolean.TRUE.equals(inspectResponse.getState().getRunning());
        }
        catch (NotFoundException e) {
            // ignore
        }
        return false;
    }

    @Input
    public abstract Property<String> getContainerName();
}
