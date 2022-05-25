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
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecSpec;

import javax.inject.Inject;

import static com.github.rodm.teamcity.internal.DockerSupport.getDebugPort;

public abstract class StartDockerServer extends DockerTask {

    @Inject
    public StartDockerServer(ExecOperations execOperations) {
        super(execOperations);
        setDescription("Starts the TeamCity Server using Docker");
    }

    @Input
    public abstract Property<String> getVersion();

    @Input
    public abstract Property<String> getDataDir();

    @Input
    public abstract Property<String> getServerOptions();

    @Input
    public abstract Property<String> getImageName();

    @Input
    public abstract Property<String> getPort();

    @Override
    protected void configure(ExecSpec execSpec) {
        execSpec.args("run");
        execSpec.args("--detach", "--rm");
        execSpec.args("--name", getContainerName().get());
        execSpec.args("-v", getDataDir().get() + ":/data/teamcity_server/datadir");
        execSpec.args("-v", getDataDir().get() + "/logs:/opt/teamcity/logs");
        execSpec.args("-e", "TEAMCITY_SERVER_OPTS=" + getServerOptions().get());
        execSpec.args("-p", getPort().get() + ":8111");
        getDebugPort(getServerOptions().get()).ifPresent(port -> execSpec.args("-p", port + ":" + port));
        execSpec.args(getImageName().get() + ":" + getVersion().get());
    }
}
