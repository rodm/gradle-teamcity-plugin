/*
 * Copyright 2022 Rod MacKenzie
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
package com.github.rodm.teamcity;

import org.gradle.api.Action;

public interface Deployment {

    /**
     * Configures the executable files for the agent-side tool plugin.
     *
     * <p>The given action is executed to configure the executable files.</p>
     *
     * @param configuration The configuration action
     */
    void executableFiles(Action<ExecutableFiles> configuration);

    ExecutableFiles getExecutableFiles();
}
