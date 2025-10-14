/*
 * Copyright 2015 the original author or authors.
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

import com.github.rodm.teamcity.internal.TeamCityTask;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecSpec;

import javax.inject.Inject;

public abstract class StartAgent extends TeamCityTask {

    private final FileSystemOperations fileOperations;

    @Inject
    public StartAgent(ExecOperations execOperations, FileSystemOperations fileOperations) {
        super(execOperations);
        this.fileOperations = fileOperations;
        setDescription("Starts the TeamCity Agent");
    }

    @Input
    public abstract Property<String> getAgentOptions();

    @Input
    public abstract Property<String> getConfigDir();

    @Input
    public abstract Property<String> getLogsDir();

    @Internal
    public FileSystemOperations getFileOperations() {
        return fileOperations;
    }

    @Override
    public void configure(ExecSpec execSpec) {
        final String name = TeamCityTask.isWindows() ? "agent.bat" : "agent.sh";
        execSpec.executable(getHomeDir().get() + "/buildAgent/bin/" + name);
        execSpec.environment("JAVA_HOME", getJavaHome().get());
        execSpec.environment("CONFIG_FILE", getConfigDir().get() + "/buildAgent.properties");
        execSpec.environment("LOG_DIR", getLogsDir().get());
        execSpec.environment("TEAMCITY_AGENT_OPTS", getAgentOptions().get());
        execSpec.args("start");
    }
}
