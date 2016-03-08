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

import org.gradle.api.DefaultTask
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class Unpack extends DefaultTask {

    @InputFile
    File source

    @OutputDirectory
    File target

    @TaskAction
    public void unpack() {
        logger.info("Unpacking TeamCity installer from {} into {}", getSource(), getTarget())
        String targetName = getTarget().name
        project.copy {
            from(project.tarTree(getSource())) {
                includeEmptyDirs = false
                eachFile { file ->
                    String[] segments = file.relativePath.segments[0..-1] as String[]
                    segments[0] = targetName
                    file.relativePath = new RelativePath(file.relativePath.endsWithFile, segments)
                }
            }
            into getTarget().getParentFile()
        }
    }
}
