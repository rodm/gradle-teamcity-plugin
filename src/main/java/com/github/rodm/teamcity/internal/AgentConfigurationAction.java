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
package com.github.rodm.teamcity.internal;

import com.github.rodm.teamcity.tasks.StartLocalAgent;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.FileSystemOperations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AgentConfigurationAction implements Action<Task> {

    @Override
    public void execute(Task task) {
        StartLocalAgent startAgent = (StartLocalAgent) task;
        FileSystemOperations fileOperations = startAgent.getFileOperations();
        try {
            String propertyFile = startAgent.getConfigDir().get() + "/buildAgent.properties";
            if (Files.notExists(Paths.get(propertyFile))) {
                Files.createDirectories(Paths.get(startAgent.getConfigDir().get()));
                fileOperations.copy(copySpec -> {
                    copySpec.from(startAgent.getHomeDir().map(path -> path + "/buildAgent/conf"));
                    copySpec.into(startAgent.getConfigDir());
                });
            }
        }
        catch (IOException e) {
            task.getLogger().warn(e.getMessage());
            throw new GradleException("Failed to create agent configuration directory.", e);
        }
    }
}
