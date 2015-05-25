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

import org.gradle.api.DefaultTask
import org.gradle.api.specs.Specs
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class GeneratePluginDescriptor extends DefaultTask {

    private File destination

    GeneratePluginDescriptor() {
        getOutputs().upToDateWhen(Specs.satisfyNone())
    }

    @OutputFile
    File getDestination() {
        if (destination == null) {
            destination = new File(project.getBuildDir(), TeamCityPlugin.PLUGIN_DESCRIPTOR_DIR + "/" + TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME)
        }
        return destination
    }

    @TaskAction
    void generateDescriptor() {
        PluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()

        if (descriptor == null) {
            descriptor = new PluginDescriptor()
        }
        getDestination().withPrintWriter { writer -> descriptor.writeTo(writer) }
    }
}
