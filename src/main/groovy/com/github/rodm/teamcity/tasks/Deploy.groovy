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
package com.github.rodm.teamcity.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory

class Deploy extends Copy {

    private ConfigurableFileCollection plugins
    private DirectoryProperty pluginsDir

    Deploy() {
        plugins = project.objects.fileCollection()
        pluginsDir = project.objects.directoryProperty()
        from { getPlugins() }
        into { getPluginsDir() }
    }

    @InputFiles
    ConfigurableFileCollection getPlugins() {
        this.plugins
    }

    @OutputDirectory
    DirectoryProperty getPluginsDir() {
        this.pluginsDir
    }
}
