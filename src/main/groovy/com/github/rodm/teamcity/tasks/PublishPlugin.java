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
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

public abstract class PublishPlugin extends DefaultTask {

    private static final String DEFAULT_HOST = "https://plugins.jetbrains.com";

    private String host = DEFAULT_HOST;

    private final WorkerExecutor executor;

    @Inject
    public PublishPlugin(WorkerExecutor executor) {
        setDescription("Publishes the plugin to the TeamCity plugin repository");
        setEnabled(!getProject().getGradle().getStartParameter().isOffline());
        this.executor = executor;
    }

    @Classpath
    public abstract FileCollection getClasspath();

    public abstract void setClasspath(FileCollection classpath);

    @Input
    public String getHost() {
        return host;
    }

    /**
     * @return the list of channels the plugin will be published to on the plugin repository
     */
    @Input
    public abstract ListProperty<String> getChannels();

    /**
     * @return the JetBrains Hub token used for uploading the plugin to the plugin repository
     */
    @Input
    public abstract Property<String> getToken();

    /**
     * @return the notes describing the changes made to the plugin
     */
    @Input
    @Optional
    public abstract Property<String> getNotes();

    /**
     * @return the plugin distribution file
     */
    @InputFile
    public abstract RegularFileProperty getDistributionFile();

    @TaskAction
    protected void publishPlugin() {
        if (getChannels().get().isEmpty()) {
            throw new InvalidUserDataException("Channels list can't be empty");
        }

        WorkQueue queue = executor.classLoaderIsolation(spec -> spec.getClasspath().from(getClasspath()));
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
