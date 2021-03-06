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

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
class ProcessDescriptor extends DefaultTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    final RegularFileProperty descriptor = project.objects.fileProperty()

    @Input
    final Provider tokens = project.objects.mapProperty(String, Object).convention([:])

    @OutputFile
    final RegularFileProperty destination = project.objects.fileProperty()

    ProcessDescriptor() {
        description = "Processes the plugin descriptor"
        onlyIf { descriptor.isPresent() }
    }

    @TaskAction
    void process() {
        project.copy {
            into destination.get().asFile.parentFile
            from descriptor.get().asFile
            rename { destination.get().asFile.name }
            if (!tokens.get().isEmpty()) {
                filter(ReplaceTokens, tokens: tokens.get())
            }
        }
    }
}
