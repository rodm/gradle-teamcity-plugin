/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rodm.teamcity.tasks

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory

class PublishTask extends DefaultTask {

    private static final Logger LOGGER = Logging.getLogger(PublishTask.class)

    private static final String DEFAULT_HOST = 'https://plugins.jetbrains.com'

    @Input
    public String host = DEFAULT_HOST

    /**
     * The list of channel names that the plugin will be published to on the plugin repository
     */
    private ListProperty<String> channels

    /**
     * The JetBrains Hub token for uploading the plugin to the plugin repository
     */
    private Property<String> token

    /**
     * The notes describing the changes made to the plugin
     */
    private Property<String> notes

    private RegularFileProperty distributionFile

    PublishTask() {
        enabled = !project.gradle.startParameter.offline
        channels = project.objects.listProperty(String)
        token = project.objects.property(String)
        notes = project.objects.property(String)
        distributionFile = project.objects.fileProperty()
    }

    /**
     * @return the list of channels the plugin will be published to on the plugin repository
     */
    @Input
    ListProperty<String> getChannels() {
        return channels
    }

    void setChannels(ListProperty<String> channels) {
        this.channels.set(channels)
    }

    /**
     * @return the token used for uploading the plugin to the plugin repository
     */
    @Input
    @Optional
    Property<String> getToken() {
        return token
    }

    void setToken(Property<String> token) {
        this.token.set(token)
    }

    /**
     * @return the notes describing the changes made to the plugin
     */
    @Input
    @Optional
    Property<String> getNotes() {
        return notes
    }

    void setNotes(String notes) {
        this.notes.set(notes)
    }

    /**
     * @return the plugin distribution file
     */
    @InputFile
    RegularFileProperty getDistributionFile() {
        return distributionFile
    }

    void setDistributionFile(RegularFileProperty distributionFile) {
        this.distributionFile.set(distributionFile)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    @TaskAction
    protected void publishPlugin() {
        if (!getChannels()) {
            throw new InvalidUserDataException("Channels list can't be empty")
        }

        def distributionFile = getDistributionFile().get().asFile
        def creationResult = TeamcityPluginManager.@Companion.createManager(true).createPlugin(distributionFile)
        if (creationResult instanceof PluginCreationSuccess) {
            def pluginId = creationResult.plugin.pluginId
            for (String channel : getChannels().get()) {
                LOGGER.info("Uploading plugin ${pluginId} from $distributionFile.absolutePath to $host, channel: $channel")
                try {
                    def uploadChannel = channel && 'default' != channel ? channel : ''
                    def repoClient = PluginRepositoryFactory.create(host, token.orNull)
                    repoClient.uploader.uploadPlugin(pluginId, distributionFile, uploadChannel, notes.orNull)
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
}
