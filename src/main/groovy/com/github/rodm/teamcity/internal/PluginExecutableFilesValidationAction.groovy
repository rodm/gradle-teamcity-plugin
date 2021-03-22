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
package com.github.rodm.teamcity.internal

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileCopyDetails

import static ValidationSupport.createXmlParser

class PluginExecutableFilesValidationAction implements Action<Task> {

    static final String MISSING_EXECUTABLE_FILE_WARNING = "%s: Executable file %s is missing."

    private Set<FileCopyDetails> files

    PluginExecutableFilesValidationAction(Set<FileCopyDetails> files) {
        this.files = files
    }

    @Override
    void execute(Task task) {
        AbstractPluginTask pluginTask = (AbstractPluginTask) task
        def paths = files.flatten { fileCopyDetails -> fileCopyDetails.path }
        def executableFiles = getExecutableFiles(pluginTask.descriptor.get().asFile)
        for (String executableFile : executableFiles) {
            if (!paths.contains(executableFile)) {
                task.logger.warn(String.format(MISSING_EXECUTABLE_FILE_WARNING, task.getPath(), executableFile))
            }
        }
    }

    static def getExecutableFiles(File descriptorFile) {
        def parser = createXmlParser()
        def descriptor = parser.parse(descriptorFile)
        return descriptor.breadthFirst().findAll { node -> node.name() == 'include' }.flatten { node -> node['@name'] }
    }
}
