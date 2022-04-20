/*
 * Copyright 2021 the original author or authors.
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

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail;
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult;
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess;
import com.jetbrains.plugin.structure.base.plugin.PluginProblem;
import com.jetbrains.plugin.structure.teamcity.TeamcityPlugin;
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager;
import org.apache.commons.io.FileUtils;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory;
import org.jetbrains.intellij.pluginRepository.PluginUploader;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public abstract class PublishAction implements WorkAction<PublishAction.PublishParameters> {

    private static final Logger LOGGER = Logging.getLogger(PublishAction.class);

    public interface PublishParameters extends WorkParameters {
        Property<String> getHost();
        ListProperty<String> getChannels();
        Property<String> getToken();
        Property<String> getNotes();
        RegularFileProperty getDistributionFile();
    }

    @Override
    public void execute() {
        final PublishParameters parameters = getParameters();

        File distributionFile = parameters.getDistributionFile().get().getAsFile();
        PluginCreationResult<TeamcityPlugin> creationResult = createPlugin(distributionFile);
        if (creationResult instanceof PluginCreationSuccess) {
            final String pluginId = ((PluginCreationSuccess<TeamcityPlugin>) creationResult).getPlugin().getPluginId();
            LOGGER.info("Uploading plugin {} from {} to {}", pluginId, distributionFile.getAbsolutePath(), parameters.getHost().get());
            for (String channel : parameters.getChannels().get()) {
                LOGGER.info("Uploading plugin to {} channel", channel);
                try {
                    String uploadChannel = ("default".equals(channel)) ? "" : channel;
                    PluginUploader uploader = createRepositoryUploader(parameters.getHost().get(), parameters.getToken().getOrNull());
                    uploader.uploadPlugin(pluginId, distributionFile, uploadChannel, parameters.getNotes().getOrNull());
                    LOGGER.info("Uploaded successfully");
                }
                catch (Exception exception) {
                    throw new GradleException("Failed to upload plugin", exception);
                }
            }
        } else if (creationResult instanceof PluginCreationFail) {
            PluginCreationFail<TeamcityPlugin> fail = (PluginCreationFail<TeamcityPlugin>) creationResult;
            List<String> problems = fail.getErrorsAndWarnings().stream()
                .filter(problem -> problem.getLevel().equals(PluginProblem.Level.ERROR))
                .map(PluginProblem::getMessage)
                .collect(Collectors.toList());
            throw new GradleException("Cannot upload plugin. " + String.join(", ", problems));
        } else {
            throw new GradleException("Cannot upload plugin.");
        }
    }

    public PluginCreationResult<TeamcityPlugin> createPlugin(File distributionFile) {
        Path extractDir = FileUtils.getTempDirectory().toPath().resolve("extracted-plugins");
        return TeamcityPluginManager.Companion.createManager(extractDir, true).createPlugin(distributionFile.toPath());
    }

    public PluginUploader createRepositoryUploader(String host, String token) {
        return PluginRepositoryFactory.create(host, token).getUploader();
    }
}
