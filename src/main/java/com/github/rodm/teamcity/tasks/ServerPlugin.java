/*
 * Copyright 2020 Rod MacKenzie
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

import com.github.rodm.teamcity.internal.AbstractPluginTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;

public abstract class ServerPlugin extends AbstractPluginTask {

    public ServerPlugin() {
        setDescription("Package TeamCity plugin");
        onlyIf(task -> getDescriptor().get().getAsFile().exists());
        into("server", copySpec ->
            copySpec.from(getServer())
        );
        into("agent", copySpec ->
            copySpec.from(getAgent())
        );
        into("", copySpec -> {
            copySpec.from(getDescriptor());
            copySpec.rename(PLUGIN_DESCRIPTOR_RENAMER);
        });
    }

    @InputFiles
    public abstract ConfigurableFileCollection getServer();

    @InputFiles
    public abstract ConfigurableFileCollection getAgent();
}
