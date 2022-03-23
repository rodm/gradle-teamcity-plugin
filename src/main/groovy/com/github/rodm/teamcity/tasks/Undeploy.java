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

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.Internal;

import java.io.File;
import java.util.stream.Collectors;

public class Undeploy extends Delete {

    @Internal
    private final ConfigurableFileCollection plugins = getProject().getObjects().fileCollection();

    @Internal
    private final DirectoryProperty pluginsDir = getProject().getObjects().directoryProperty();

    public Undeploy() {
        setDescription("Un-deploys plugins from the TeamCity Server");
        delete(getProject().fileTree(getPluginsDir(), files -> {
            files.include(getPlugins().getFiles().stream()
                .map(File::getName)
                .collect(Collectors.toList()));
        }));
    }

    public final ConfigurableFileCollection getPlugins() {
        return plugins;
    }

    public final DirectoryProperty getPluginsDir() {
        return pluginsDir;
    }
}
