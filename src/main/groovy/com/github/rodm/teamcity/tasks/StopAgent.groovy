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
package com.github.rodm.teamcity.tasks

import com.github.rodm.teamcity.internal.TeamCityTask
import org.gradle.process.ExecSpec

class StopAgent extends TeamCityTask {

    StopAgent() {
        description = 'Stops the TeamCity Agent'
    }

    @Override
    void configure(ExecSpec execSpec) {
        def name = isWindows() ? 'agent.bat' : 'agent.sh'
        execSpec.executable("${getHomeDir().get()}/buildAgent/bin/${name}")
        execSpec.environment('JAVA_HOME', getJavaHome().get())
        execSpec.args('stop')
    }
}
