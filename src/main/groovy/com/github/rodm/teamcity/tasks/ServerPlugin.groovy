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

import com.github.rodm.teamcity.TeamCityAgentPlugin
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.bundling.Zip

import static com.github.rodm.teamcity.TeamCityAgentPlugin.AGENT_PLUGIN_TASK_NAME
import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME
import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME

class ServerPlugin extends Zip {

    private RegularFileProperty descriptor

    ServerPlugin() {
        description = 'Package TeamCity plugin'
        descriptor = project.objects.fileProperty()
        into('server') {
            project.plugins.withType(JavaPlugin) {
                from(project.tasks.named(JAR_TASK_NAME))
                from(project.configurations.runtimeClasspath)
            }
            from(project.configurations.server)
        }
        into('agent') {
            if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
                from(project.tasks.named(AGENT_PLUGIN_TASK_NAME))
            } else {
                from(project.configurations.agent)
            }
        }
        into('') {
            from { getDescriptor() }
            rename { PLUGIN_DESCRIPTOR_FILENAME }
        }
    }

    @InputFile
    RegularFileProperty getDescriptor() {
        this.descriptor
    }
}
