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
package com.github.rodm.teamcity.tasks

import com.github.rodm.teamcity.TeamCityPlugin
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.bundling.Zip

class PackageAgentPlugin extends Zip {

    private def descriptor

    private FileCollection agentComponents

    PackageAgentPlugin() {
        description = 'Package TeamCity Agent plugin'
        group = 'TeamCity'

        into('lib') {
            from {
                FileCollection agentComponents = getAgentComponents()
                agentComponents ? agentComponents.filter { File file -> file.isFile() } : []
            }
        }
        into('') {
            from {
                getDescriptor()
            }
            rename {
                TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME
            }
        }
    }

    @InputFiles
    FileCollection getAgentComponents() {
        return agentComponents
    }

    void setAgentComponents(Object components) {
        this.agentComponents = project.files(components)
    }

    @InputFile
    public File getDescriptor() {
        if (descriptor == null) {
            descriptor = new File(project.getBuildDir(), TeamCityPlugin.PLUGIN_DESCRIPTOR_DIR + "/" + TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME)
        }
        return descriptor
    }

    public void setDescriptor(File descriptor) {
        this.descriptor = descriptor
    }
}
