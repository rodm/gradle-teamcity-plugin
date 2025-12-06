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
package com.github.rodm.teamcity.docker;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

public abstract class StopContainerAction implements WorkAction<StopContainerAction.StopContainerParameters> {

    private static final Logger LOGGER = Logging.getLogger(StopContainerAction.class);

    public interface StopContainerParameters extends WorkParameters {
        Property<String> getContainerName();
    }

    @Override
    public void execute() {
        final StopContainerParameters parameters = getParameters();
        final DockerOperations dockerOperations = new DockerOperations();

        String containerId = parameters.getContainerName().get();
        if (!dockerOperations.isContainerRunning(containerId)) {
            LOGGER.info("Container {} is already stopped", containerId);
            return;
        }

        dockerOperations.stopContainer(containerId);
        LOGGER.info("Container stopped {}", containerId);
    }
}
