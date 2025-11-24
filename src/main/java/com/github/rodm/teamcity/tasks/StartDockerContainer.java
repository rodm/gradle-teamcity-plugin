/*
 * Copyright 2025 the original author or authors.
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

import com.github.rodm.teamcity.docker.ContainerConfiguration;
import com.github.rodm.teamcity.docker.CreateContainerAction;
import com.github.rodm.teamcity.docker.DockerTask;
import com.github.rodm.teamcity.docker.StartContainerAction;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.workers.WorkQueue;

@UntrackedTask(because = "Should always run the Docker task")
public abstract class StartDockerContainer extends DockerTask {

    public StartDockerContainer() {
        setDescription("Starts the Docker container");
    }

    @Input
    public abstract Property<String> getDataDir();

    @Input
    public abstract Property<String> getImage();

    @Input
    public abstract MapProperty<String, String> getPorts();

    @Input
    public abstract MapProperty<String, String> getVolumes();

    @Input
    public abstract MapProperty<String, String> getEnvironmentVariables();

    @TaskAction
    void startContainer() {
        String image = getImage().get();
        String name = getContainerName().get();
        ContainerConfiguration configuration = ContainerConfiguration.builder()
            .image(image)
            .name(name)
            .autoRemove()
            .environment(getEnvironmentVariables().get());

        getPorts().get().forEach((hostPort, containerPort) -> configuration
            .bindPort(hostPort, containerPort)
            .exposePort(hostPort));

        getVolumes().get().forEach((hostPath, containerPath) -> configuration
            .bind(getDataDir().get() + "/" + hostPath, containerPath));

        WorkQueue queue = getExecutor().classLoaderIsolation(spec -> spec.getClasspath().from(getClasspath()));
        queue.submit(CreateContainerAction.class, params -> {
            params.getConfiguration().set(configuration);
            params.getDescription().set("Create container " + name);
        });
        queue.await();

        queue.submit(StartContainerAction.class, params -> {
            params.getContainerName().set(getContainerName());
            params.getDescription().set("Start container" + name);
        });
        queue.await();
    }
}
