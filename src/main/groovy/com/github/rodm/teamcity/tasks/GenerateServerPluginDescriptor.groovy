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

import com.github.rodm.teamcity.ServerPluginDescriptor
import com.github.rodm.teamcity.ServerPluginDescriptorGenerator
import com.github.rodm.teamcity.TeamCityVersion
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import static com.github.rodm.teamcity.TeamCityVersion.VERSION_9_0

@CompileStatic
class GenerateServerPluginDescriptor extends DefaultTask {

    private String version

    private ServerPluginDescriptor descriptor

    private File destination

    @Input
    String getVersion() {
        return version
    }

    @Nested
    ServerPluginDescriptor getDescriptor() {
        return descriptor
    }

    @OutputFile
    File getDestination() {
        return destination
    }

    @TaskAction
    void generateDescriptor() {
        if (TeamCityVersion.version(getVersion()) < VERSION_9_0 && getDescriptor().dependencies.hasDependencies()) {
            project.logger.warn("${path}: Plugin descriptor does not support dependencies for version ${getVersion()}")
        }
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(getDescriptor(), getVersion())
        getDestination().withPrintWriter { writer -> generator.writeTo(writer) }
    }
}
