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
package com.github.rodm.teamcity.docker;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public abstract class QueryContainerAction implements WorkAction<QueryContainerAction.ServerAddressParameters> {

    private static final Logger LOGGER = Logging.getLogger(QueryContainerAction.class);

    public interface ServerAddressParameters extends WorkParameters {
        Property<String> getContainerName();
        RegularFileProperty getOutputPath();
    }

    @Override
    public void execute() {
        final ServerAddressParameters parameters = getParameters();
        final DockerOperations dockerOperations = new DockerOperations();

        String containerId = parameters.getContainerName().get();
        if (!dockerOperations.isContainerRunning(containerId)) {
            LOGGER.info("Container {} is not running", containerId);
            return;
        }

        String ipAddress = dockerOperations.getIpAddress(parameters.getContainerName().get());

        Path path = parameters.getOutputPath().get().getAsFile().toPath();
        try (Writer writer = Files.newBufferedWriter(path)) {
            Properties props = new Properties();
            props.setProperty("ipaddress", ipAddress);
            props.store(writer, null);
        } catch (IOException e) {
            LOGGER.error("Failed to write container properties to file");
        }
    }
}
