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

import com.github.rodm.teamcity.AgentPluginDescriptor
import com.github.rodm.teamcity.AgentPluginDescriptorGenerator
import com.github.rodm.teamcity.TeamCityAgentPlugin
import com.github.rodm.teamcity.TeamCityPlugin
import com.github.rodm.teamcity.TeamCityPluginExtension
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CompileStatic
class GenerateAgentPluginDescriptor extends DefaultTask {

    private String version

    private AgentPluginDescriptor descriptor

    private File destination

    @Input
    String getVersion() {
        return version
    }

    @Nested
    AgentPluginDescriptor getDescriptor() {
        return descriptor
    }

    @OutputFile
    File getDestination() {
        if (destination == null) {
            destination = new File(project.getBuildDir(), TeamCityAgentPlugin.AGENT_PLUGIN_DESCRIPTOR_DIR + "/" + TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME)
        }
        return destination
    }

    @TaskAction
    void generateDescriptor() {
        TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension)

        def majorVersion = extension.getMajorVersion()
        if (majorVersion != null && majorVersion < 9 && getDescriptor().dependencies.hasDependencies()) {
            project.logger.warn("${path}: Plugin descriptor does not support dependencies for version ${getVersion()}")
        }
        AgentPluginDescriptorGenerator generator = new AgentPluginDescriptorGenerator(getDescriptor())
        getDestination().withPrintWriter { writer -> generator.writeTo(writer) }
    }
}
