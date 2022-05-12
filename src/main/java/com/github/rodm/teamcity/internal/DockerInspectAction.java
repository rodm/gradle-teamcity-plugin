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
package com.github.rodm.teamcity.internal;

import com.github.rodm.teamcity.tasks.StartDockerAgent;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.process.ExecOperations;

import java.io.ByteArrayOutputStream;

public class DockerInspectAction implements Action<Task> {

    @Override
    public void execute(Task task) {
        StartDockerAgent startAgent = (StartDockerAgent) task;
        ExecOperations execOperations = startAgent.getExecOperations();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        execOperations.exec(execSpec -> {
            execSpec.executable("docker");
            execSpec.args("inspect");
            execSpec.args("--format", "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}");
            execSpec.args(startAgent.getServerContainerName().get());
            execSpec.setStandardOutput(out);
            execSpec.setErrorOutput(out);
        });
        String ipAddress = out.toString().replace("\n", "");
        startAgent.getIpAddress().set(ipAddress);
    }
}
