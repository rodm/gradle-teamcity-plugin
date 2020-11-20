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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.bundling.Zip

import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME

class AgentPlugin extends Zip {

    @InputFile
    final RegularFileProperty descriptor = project.objects.fileProperty()

    @InputFiles
    final ConfigurableFileCollection lib = project.objects.fileCollection()

    AgentPlugin() {
        description = 'Package TeamCity Agent plugin'
        into("lib") {
            from(getLib())
        }
        into('') {
            from { getDescriptor() }
            rename { PLUGIN_DESCRIPTOR_FILENAME }
        }
    }
}
