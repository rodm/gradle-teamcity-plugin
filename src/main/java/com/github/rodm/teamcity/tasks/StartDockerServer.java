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

import com.github.rodm.teamcity.docker.ContainerConfiguration;
import com.github.rodm.teamcity.docker.CreateContainerAction;
import com.github.rodm.teamcity.docker.DockerTask;
import com.github.rodm.teamcity.docker.StartContainerAction;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.github.rodm.teamcity.docker.DockerSupport.getDebugPort;

public abstract class StartDockerServer extends DockerTask {

    private final WorkerExecutor executor;

    @Inject
    public StartDockerServer(WorkerExecutor executor) {
        setDescription("Starts the TeamCity Server using Docker");
        this.executor = executor;
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
        String image = getImageName().get() + ":" + getVersion().get();
        String serverOptions = getServerOptions().get();
        ContainerConfiguration configuration = ContainerConfiguration.builder()
            .image(image)
            .name(getContainerName().get())
            .autoRemove()
            .bind(getDataDir().get(),"/data/teamcity_server/datadir")
            .bind(getLogsDir().get(),"/opt/teamcity/logs")
            .bindPort(getPort().get(), "8111")
            .environment("TEAMCITY_SERVER_OPTS", serverOptions);
        getDebugPort(serverOptions).ifPresent(debugPort -> configuration
            .bindPort(debugPort, debugPort)
            .exposePort(debugPort));

        WorkQueue queue = executor.classLoaderIsolation(spec -> spec.getClasspath().from(getClasspath()));
        queue.submit(CreateContainerAction.class, params -> {
            params.getConfiguration().set(configuration);
            params.getDescription().set("TeamCity Server");
        });
        queue.await();

        try {
            Files.createDirectories(Paths.get(getLogsDir().get()));

            queue.submit(StartContainerAction.class, params -> {
                params.getContainerName().set(getContainerName());
                params.getDescription().set("TeamCity Server");
            });
            queue.await();
        }
        catch (IOException e) {
            getLogger().warn("Failed to create the logs directory");
        }
    }
}
