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
package com.github.rodm.teamcity.tasks;

import com.github.rodm.teamcity.internal.SignAction;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

public abstract class SignPlugin extends DefaultTask {

    private final WorkerExecutor executor;
    private final ProjectLayout layout;

    @Inject
    public SignPlugin(WorkerExecutor executor, ProjectLayout layout) {
        setDescription("Signs the plugin");
        getSignedPluginFile().convention(getPluginFile().map(this::signedName));
        this.executor = executor;
        this.layout = layout;
    }

    @Classpath
    public abstract FileCollection getClasspath();

    public abstract void setClasspath(FileCollection classpath);

    /**
     * @return the plugin file that will be signed
     */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getPluginFile();

    /**
     * @return signed plugin file
     */
    @OutputFile
    public abstract RegularFileProperty getSignedPluginFile();

    @Input
    public abstract Property<String> getCertificateChain();

    @Input
    public abstract Property<String> getPrivateKey();

    @Input
    @Optional
    public abstract Property<String> getPassword();

    @TaskAction
    protected void signPlugin() {
        WorkQueue queue = executor.classLoaderIsolation(spec -> spec.getClasspath().from(getClasspath()));
        queue.submit(SignAction.class, params -> {
            params.getPluginFile().set(getPluginFile());
            params.getCertificateChain().set(getCertificateChain());
            params.getPrivateKey().set(getPrivateKey());
            params.getPassword().set(getPassword());
            params.getSignedPluginFile().set(getSignedPluginFile());
        });
        queue.await();
    }

    public RegularFile signedName(RegularFile file) {
        final String path = FilenameUtils.removeExtension(file.getAsFile().getPath());
        final String extension = FilenameUtils.getExtension(file.getAsFile().getName());
        return layout.getProjectDirectory().file(path + "-signed." + extension);
    }
}
