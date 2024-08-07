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

import com.github.rodm.teamcity.docker.DockerTask;
import com.github.rodm.teamcity.docker.StartAgentContainerAction;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@UntrackedTask(because = "Should always run the Docker task")
public abstract class StartDockerAgent extends DockerTask {

    private final WorkerExecutor executor;

    @Inject
    public StartDockerAgent(WorkerExecutor executor) {
        setDescription("Starts the TeamCity Agent using Docker");
        this.executor = executor;
    }

    @Input
    public abstract Property<String> getDataDir();

    @Input
    public abstract Property<String> getConfigDir();

    @Input
    public abstract Property<String> getAgentOptions();

    @Input
    public abstract Property<String> getImageName();

    @Input
    public abstract Property<String> getImageTag();

    @Input
    public abstract Property<String> getServerContainerName();

    @TaskAction
    void startAgent() {
        try {
            Files.createDirectories(Paths.get(getConfigDir().get()));

            WorkQueue queue = executor.classLoaderIsolation(spec -> spec.getClasspath().from(getClasspath()));
            queue.submit(StartAgentContainerAction.class, params -> {
                params.getContainerName().set(getContainerName());
                params.getDataDir().set(getDataDir());
                params.getConfigDir().set(getConfigDir());
                params.getAgentOptions().set(getAgentOptions());
                params.getImageName().set(getImageName());
                params.getImageTag().set(getImageTag());
                params.getServerContainerName().set(getServerContainerName());
            });
            queue.await();
        }
        catch (IOException e) {
            getLogger().warn("Failed to create the agent configuration directory");
        }
    }
}
