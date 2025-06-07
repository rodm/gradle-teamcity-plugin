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

import com.github.rodm.teamcity.internal.ServerConfiguration;
import com.github.rodm.teamcity.internal.TeamCityTask;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecSpec;

import javax.inject.Inject;

@UntrackedTask(because = "Should always run the TeamCity task")
public abstract class StopServer extends TeamCityTask implements ServerConfiguration {

    @Inject
    public StopServer(ExecOperations execOperations) {
        super(execOperations);
        setDescription("Stops the TeamCity Server");
        getServerHost().convention("localhost");
        getServerPort().convention("8111");
    }

    @Override
    public void configure(ExecSpec execSpec) {
        String name = TeamCityTask.isWindows() ? "teamcity-server.bat" : "teamcity-server.sh";
        execSpec.executable(getHomeDir().get() + "/bin/" + name);
        execSpec.environment("JAVA_HOME", getJavaHome().get());
        execSpec.args("stop");
    }
}
