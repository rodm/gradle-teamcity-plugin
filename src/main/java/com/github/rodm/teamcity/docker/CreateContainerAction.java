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

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import static com.github.rodm.teamcity.docker.DockerOperations.IMAGE_NOT_AVAILABLE;
import static java.lang.String.format;

public abstract class CreateContainerAction implements WorkAction<CreateContainerAction.CreateContainerParameters> {

    private static final Logger LOGGER = Logging.getLogger(CreateContainerAction.class);

    public interface CreateContainerParameters extends WorkParameters {
        Property<ContainerConfiguration> getConfiguration();
    }

    @Override
    public void execute() {
        final CreateContainerParameters parameters = getParameters();
        final DockerOperations dockerOperations = new DockerOperations();

        ContainerConfiguration configuration = parameters.getConfiguration().get();
        String image = configuration.getImage();
        if (!dockerOperations.isImageAvailable(image)) {
            throw new GradleException(format(IMAGE_NOT_AVAILABLE, image));
        }

        String containerId = configuration.getName();
        if (dockerOperations.isContainerAvailable(containerId)) {
            LOGGER.info("Container '{}' already exists", containerId);
            return;
        }

        String id = dockerOperations.createContainer(configuration);
        LOGGER.info("Created container {} with id: {}", containerId, id);
    }
}
