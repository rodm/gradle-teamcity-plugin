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
import com.github.rodm.teamcity.docker.QueryContainerAction;
import com.github.rodm.teamcity.docker.StartContainerAction;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static com.github.rodm.teamcity.docker.DockerSupport.getDebugPort;

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
    public abstract Property<String> getLogsDir();

    @Input
    public abstract Property<String> getAgentOptions();

    @Input
    public abstract Property<String> getImageName();

    @Input
    public abstract Property<String> getImageTag();

    @Input
    public abstract Property<String> getServerContainerName();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    void startAgent() {
        String image = getImageName().get() + ":" + getImageTag().get();
        String agentOptions = getAgentOptions().get();
        Path path = getOutputDir().file(getServerContainerName().get() + ".properties").get().getAsFile().toPath();

        try {
            WorkQueue queue = executor.classLoaderIsolation(spec -> spec.getClasspath().from(getClasspath()));
            queue.submit(QueryContainerAction.class, params -> {
                params.getContainerName().set(getServerContainerName());
                params.getDescription().set("TeamCity Build Agent");
                params.getOutputPath().set(path.toFile());
            });
            queue.await();

            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(path)) {
                properties.load(reader);
            }

            String ipAddress = properties.getProperty("ipaddress", "localhost");
            String serverUrl = "http://" + ipAddress + ":8111/";
            ContainerConfiguration configuration = ContainerConfiguration.builder()
                .image(image)
                .name(getContainerName().get())
                .autoRemove()
                .bind(getConfigDir().get(), "/data/teamcity_agent/conf")
                .bind(getLogsDir().get(), "/opt/buildagent/logs")
                .environment("SERVER_URL", serverUrl)
                .environment("AGENT_NAME", "Default Agent")
                .environment("TEAMCITY_AGENT_OPTS", agentOptions);
            getDebugPort(agentOptions).ifPresent(debugPort -> configuration
                .bindPort(debugPort, debugPort)
                .exposePort(debugPort));

            queue.submit(CreateContainerAction.class, params -> {
                params.getConfiguration().set(configuration);
                params.getDescription().set("TeamCity Build Agent");
            });
            queue.await();

            Files.createDirectories(Paths.get(getConfigDir().get()));
            queue.submit(StartContainerAction.class, params -> {
                params.getContainerName().set(getContainerName());
                params.getDescription().set("TeamCity Build Agent");
            });
            queue.await();
        }
        catch (IOException e) {
            getLogger().warn("Failed to create the agent configuration directory", e);
        }
    }
}
