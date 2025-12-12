/*
 * Copyright 2025 Rod MacKenzie
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

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CreateDataDirAction implements Action<Task> {

    private final Provider<String> dataDir;

    public CreateDataDirAction(Provider<String> dataDir) {
        this.dataDir = dataDir;
    }

    @Override
    public void execute(Task task) {
        try {
            Files.createDirectories(Paths.get(dataDir.get()));
        } catch (IOException e) {
            throw new GradleException("Failed to create environment data directory", e);
        }
    }
}
