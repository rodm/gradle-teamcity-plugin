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

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

public abstract class StartContainerAction implements WorkAction<StartContainerAction.StartContainerParameters> {

    private static final Logger LOGGER = Logging.getLogger(StartContainerAction.class);

    public interface StartContainerParameters extends WorkParameters {
        Property<String> getContainerName();
        Property<String> getDescription();
    }

    @Override
    public void execute() {
        final StartContainerParameters parameters = getParameters();
        final DockerOperations dockerOperations = new DockerOperations();

        String description = parameters.getDescription().get();
        String containerId = parameters.getContainerName().get();
        if (dockerOperations.isContainerRunning(containerId)) {
            LOGGER.info("{} container '{}' is already running", description, containerId);
            return;
        }

        dockerOperations.startContainer(containerId);
        LOGGER.info("{} container started", description);
    }
}
