/*
 * Copyright 2015 Rod MacKenzie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rodm.teamcity

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

import static groovy.transform.TypeCheckingMode.SKIP

/**
 * Agent-side plugin deployment configuration
 */
@CompileStatic
class PluginDeployment {

    /**
     * Use a separate classloader to load the agent-side plugin's classes.
     */
    @Input
    @Optional
    Boolean useSeparateClassloader

    @Nested
    ExecutableFiles executableFiles

    void init() {
        executableFiles = (this as ExtensionAware).extensions.create('executableFiles', ExecutableFiles)
    }

    /**
     * Configures the executable files for the agent-side tool plugin.
     *
     * <p>The given action is executed to configure the executable files.</p>

     * @param configuration The action
     * @return
     */
    def executableFiles(Action<ExecutableFiles> configuration) {
        configuration.execute(executableFiles)
    }
}
