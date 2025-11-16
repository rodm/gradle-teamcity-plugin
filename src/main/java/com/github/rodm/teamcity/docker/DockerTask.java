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

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

@UntrackedTask(because = "Should always run the Docker task")
public abstract class DockerTask extends DefaultTask {

    @Inject
    public abstract WorkerExecutor getExecutor();

    @Classpath
    public abstract FileCollection getClasspath();

    public abstract void setClasspath(FileCollection classpath);

    @Input
    public abstract Property<String> getContainerName();
}
