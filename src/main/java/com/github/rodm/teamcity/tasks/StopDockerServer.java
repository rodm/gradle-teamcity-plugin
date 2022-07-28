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
package com.github.rodm.teamcity.tasks;

import com.github.rodm.teamcity.internal.DockerTask;
import com.github.rodm.teamcity.internal.StopContainerAction;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

public abstract class StopDockerServer extends DockerTask {

    private final WorkerExecutor executor;

    @Inject
    public StopDockerServer(WorkerExecutor executor) {
        setDescription("Stops the TeamCity Server using Docker");
        this.executor = executor;
    }

    @TaskAction
    void stopServer() {
        WorkQueue queue = executor.classLoaderIsolation(spec -> spec.getClasspath().from(getClasspath()));
        queue.submit(StopContainerAction.class, params -> {
            params.getContainerName().set(getContainerName());
            params.getDescription().set("TeamCity Server");
        });
        queue.await();
    }
}
