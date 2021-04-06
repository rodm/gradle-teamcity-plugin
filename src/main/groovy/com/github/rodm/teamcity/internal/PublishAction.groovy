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
package com.github.rodm.teamcity.internal

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.teamcity.TeamcityPlugin
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory
import org.jetbrains.intellij.pluginRepository.PluginUploader

abstract class PublishAction implements WorkAction<Parameters> {

    private final static Logger LOGGER = Logging.getLogger(PublishAction)

    interface Parameters extends WorkParameters {
        String getHost()
        ListProperty<String> getChannels()
        Property<String> getToken()
        Property<String> getNotes()
        RegularFileProperty getDistributionFile()
    }

    @Override
    void execute() {
        Parameters parameters = getParameters()

        def distributionFile = parameters.distributionFile.get().asFile
        def creationResult = createPlugin(distributionFile)
        if (creationResult instanceof PluginCreationSuccess) {
            def pluginId = creationResult.plugin.pluginId
            LOGGER.info("Uploading plugin ${pluginId} from $distributionFile.absolutePath to ${parameters.host}")
            for (String channel : parameters.channels.get()) {
                LOGGER.info("Uploading plugin to ${channel} channel")
                try {
                    def uploadChannel = channel && 'default' != channel ? channel : ''
                    def uploader = createRepositoryUploader(parameters.host, parameters.token.orNull)
                    uploader.uploadPlugin(pluginId, distributionFile, uploadChannel, parameters.notes.orNull)
                    LOGGER.info("Uploaded successfully")
                }
                catch (exception) {
                    throw new GradleException('Failed to upload plugin', exception)
                }
            }
        } else if (creationResult instanceof PluginCreationFail) {
            def problems = creationResult.errorsAndWarnings.findAll { it.level == PluginProblem.Level.ERROR }.join(", ")
            throw new GradleException("Cannot upload plugin. $problems")
        } else {
            throw new GradleException("Cannot upload plugin.")
        }
    }

    PluginCreationResult<TeamcityPlugin> createPlugin(File distributionFile) {
        TeamcityPluginManager.@Companion.createManager(true).createPlugin(distributionFile)
    }

    PluginUploader createRepositoryUploader(String host, String token) {
        PluginRepositoryFactory.create(host, token).uploader
    }
}
