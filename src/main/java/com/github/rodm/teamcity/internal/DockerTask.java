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

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecSpec;

import java.io.ByteArrayOutputStream;

public abstract class DockerTask extends DefaultTask {

    protected final ExecOperations execOperations;

    protected DockerTask(ExecOperations execOperations) {
        this.execOperations = execOperations;
    }

    @Internal
    public ExecOperations getExecOperations() {
        return execOperations;
    }

    @Input
    public abstract Property<String> getContainerName();

    @TaskAction
    public void exec() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        execOperations.exec(execSpec -> {
            execSpec.executable("docker");
            configure(execSpec);
            execSpec.setStandardOutput(out);
            execSpec.setErrorOutput(out);
        });
        getLogger().info(out.toString());
    }

    protected abstract void configure(ExecSpec execSpec);
}
