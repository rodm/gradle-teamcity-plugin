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
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion

class ProcessDescriptor extends DefaultTask {

    private RegularFileProperty descriptor

    private DirectoryProperty destinationDir

    private Provider tokens

    ProcessDescriptor() {
        descriptor = project.objects.fileProperty()
        destinationDir = project.objects.directoryProperty()
        tokens = project.objects.mapProperty(String, Object)
        tokens.set([:])
    }

    @InputFile
    RegularFileProperty getDescriptor() {
        return descriptor
    }

    @OutputDirectory
    DirectoryProperty getDestinationDir() {
        return destinationDir
    }

    void setDestinationDir(File dir) {
        destinationDir.set(dir)
    }

    @Input
    Provider<Map<String, Object>> getTokens() {
        return tokens
    }

    @TaskAction
    public void process() {
        project.copy {
            into destinationDir.get().asFile
            from descriptor.get().asFile
            rename { TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME }
            if (!tokens.get().isEmpty()) {
                filter(ReplaceTokens, tokens: tokens.get())
            }
        }
    }
}
