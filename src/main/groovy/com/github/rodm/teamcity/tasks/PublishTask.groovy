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
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.jetbrains.intellij.pluginRepository.PluginRepositoryInstance

class PublishTask extends DefaultTask {

    private static final Logger LOGGER = Logging.getLogger(PublishTask.class)

    private static final String DEFAULT_HOST = 'https://plugins.jetbrains.com'

    @Input
    public String host = DEFAULT_HOST

    /**
     * The username for uploading the plugin to the plugin repository
     */
    private String username

    /**
     * The password for uploading the plugin to the plugin repository
     */
    private String password

    /**
     * The list of channel names that the plugin will be published to on the plugin repository
     */
    @Input
    public List<String> channels = ['default']

    public File distributionFile

    /**
     * @return the username used for uploading the plugin to the plugin repository
     */
    @Input
    String getUsername() {
        return username
    }

    void setUsername(String username) {
        this.username = username
    }

    /**
     * @return the password used for uploading the plugin to the plugin repository
     */
    @Input
    String getPassword() {
        return password
    }

    void setPassword(String password) {
        this.password = password
    }

    /**
     * @return the plugin distribution file
     */
    @InputFile
    File getDistributionFile() {
        return distributionFile
    }

    PublishTask() {
        enabled = !project.gradle.startParameter.offline
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    @TaskAction
    protected void publishPlugin() {
        def distributionFile = getDistributionFile()
        if (!channels) {
            throw new TaskExecutionException(this, new GradleException("Channels list can't be empty"))
        }

        def creationResult = TeamcityPluginManager.@Companion.createManager(true).createPlugin(distributionFile)
        if (creationResult instanceof PluginCreationSuccess) {
            def pluginId = creationResult.plugin.pluginId
            for (String channel : channels) {
                LOGGER.info("Uploading plugin ${pluginId} from $distributionFile.absolutePath to $host, channel: $channel")
                try {
                    def repoClient = new PluginRepositoryInstance(host, username, password)
                    repoClient.uploadPlugin(pluginId, distributionFile, channel && 'default' != channel ? channel : '')
                    LOGGER.info("Uploaded successfully")
                }
                catch (exception) {
                    throw new TaskExecutionException(this, new RuntimeException("Failed to upload plugin", exception))
                }
            }
        } else if (creationResult instanceof PluginCreationFail) {
            def problems = creationResult.errorsAndWarnings.findAll { it.level == PluginProblem.Level.ERROR }.join(", ")
            throw new TaskExecutionException(this, new GradleException("Cannot upload plugin. $problems"))
        } else {
            throw new TaskExecutionException(this, new GradleException("Cannot upload plugin."))
        }
    }
}
