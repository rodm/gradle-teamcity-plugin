/*
 * Copyright 2017 the original author or authors.
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

package com.github.rodm.teamcity.tasks

import com.github.rodm.teamcity.internal.PublishAction
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

class PublishTask extends DefaultTask {

    static final String DEFAULT_HOST = 'https://plugins.jetbrains.com'

    private String host = DEFAULT_HOST

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

    private FileCollection classpath
    private final WorkerExecutor executor

    @Inject
    PublishTask(WorkerExecutor executor) {
        description = "Publishes the plugin to the TeamCity plugin repository"
        enabled = !project.gradle.startParameter.offline
        channels = project.objects.listProperty(String)
        token = project.objects.property(String)
        notes = project.objects.property(String)
        distributionFile = project.objects.fileProperty()
        this.executor = executor
    }

    @Classpath
    FileCollection getClasspath() {
        return classpath
    }

    void setClasspath(FileCollection classpath) {
        this.classpath = classpath
    }

    @Input
    String getHost() {
        return host
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
    Property<String> getToken() {
        return token
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

    @TaskAction
    protected void publishPlugin() {
        if (!getChannels().get()) {
            throw new InvalidUserDataException("Channels list can't be empty")
        }

        WorkQueue queue = executor.classLoaderIsolation { spec ->
            spec.classpath.from(this.classpath)
        }
        queue.submit(PublishAction, {params ->
            params.host = this.host
            params.channels = this.channels
            params.token = this.token
            params.notes = this.notes
            params.distributionFile = this.distributionFile
        })
        queue.await()
    }
}
