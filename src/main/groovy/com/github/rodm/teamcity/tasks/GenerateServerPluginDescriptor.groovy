/*
 * Copyright 2015 Rod MacKenzie
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

import com.github.rodm.teamcity.ServerPluginDescriptor
import com.github.rodm.teamcity.ServerPluginDescriptorGenerator
import com.github.rodm.teamcity.TeamCityVersion
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2020_1
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_9_0

@CompileStatic
@CacheableTask
class GenerateServerPluginDescriptor extends DefaultTask {

    @Input
    final Property<String> version = project.objects.property(String)

    @Nested
    final Property<ServerPluginDescriptor> descriptor = project.objects.property(ServerPluginDescriptor)

    @OutputFile
    final RegularFileProperty destination = project.objects.fileProperty()

    GenerateServerPluginDescriptor() {
        onlyIf { descriptor.isPresent() }
    }

    @TaskAction
    void generateDescriptor() {
        TeamCityVersion teamcityVersion = TeamCityVersion.version(version.get())
        if (teamcityVersion < VERSION_9_0 && descriptor.get().dependencies.hasDependencies()) {
            project.logger.warn("${path}: Plugin descriptor does not support dependencies for version ${version.get()}")
        }
        if (teamcityVersion < VERSION_2018_2 && descriptor.get().allowRuntimeReload != null) {
            project.logger.warn("${path}: Plugin descriptor does not support allowRuntimeReload for version ${version.get()}")
        }
        if (teamcityVersion < VERSION_2020_1 && descriptor.get().nodeResponsibilitiesAware != null) {
            project.logger.warn("${path}: Plugin descriptor does not support nodeResponsibilitiesAware for version ${version.get()}")
        }
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor.get(), version.get())
        destination.get().asFile.withPrintWriter('UTF-8') { writer -> generator.writeTo(writer) }
    }
}
