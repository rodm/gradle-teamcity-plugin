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

package com.github.rodm.teamcity.tasks;

import com.github.rodm.teamcity.internal.PublishAction;
import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.ClassLoaderWorkerSpec;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

public class PublishPlugin extends DefaultTask {

    private static final String DEFAULT_HOST = "https://plugins.jetbrains.com";

    private String host = DEFAULT_HOST;

    /**
     * The list of channel names that the plugin will be published to on the plugin repository
     */
    private ListProperty<String> channels;

    /**
     * The JetBrains Hub token for uploading the plugin to the plugin repository
     */
    private Property<String> token;

    /**
     * The notes describing the changes made to the plugin
     */
    private Property<String> notes;

    private RegularFileProperty distributionFile;

    private FileCollection classpath;
    private final WorkerExecutor executor;

    @Inject
    public PublishPlugin(WorkerExecutor executor) {
        setDescription("Publishes the plugin to the TeamCity plugin repository");
        setEnabled(!getProject().getGradle().getStartParameter().isOffline());
        channels = getProject().getObjects().listProperty(String.class);
        token = getProject().getObjects().property(String.class);
        notes = getProject().getObjects().property(String.class);
        distributionFile = getProject().getObjects().fileProperty();
        this.executor = executor;
    }

    @Classpath
    public FileCollection getClasspath() {
        return classpath;
    }

    public void setClasspath(FileCollection classpath) {
        this.classpath = classpath;
    }

    @Input
    public String getHost() {
        return host;
    }

    /**
     * @return the list of channels the plugin will be published to on the plugin repository
     */
    @Input
    public ListProperty<String> getChannels() {
        return channels;
    }

    public void setChannels(ListProperty<String> channels) {
        this.channels.set(channels);
    }

    /**
     * @return the token used for uploading the plugin to the plugin repository
     */
    @Input
    public Property<String> getToken() {
        return token;
    }

    /**
     * @return the notes describing the changes made to the plugin
     */
    @Input
    @Optional
    public Property<String> getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes.set(notes);
    }

    /**
     * @return the plugin distribution file
     */
    @InputFile
    public RegularFileProperty getDistributionFile() {
        return distributionFile;
    }

    @TaskAction
    protected void publishPlugin() {
        if (getChannels().get().isEmpty()) {
            throw new InvalidUserDataException("Channels list can't be empty");
        }

        WorkQueue queue = executor.classLoaderIsolation(spec -> spec.getClasspath().from(classpath));
        queue.submit(PublishAction.class, params -> {
            params.getHost().set(getHost());
            params.getChannels().set(getChannels());
            params.getToken().set(getToken());
            params.getNotes().set(getNotes());
            params.getDistributionFile().set(getDistributionFile());
        });
        queue.await();
    }
}
