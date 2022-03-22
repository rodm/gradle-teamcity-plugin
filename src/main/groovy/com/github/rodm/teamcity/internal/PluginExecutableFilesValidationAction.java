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
package com.github.rodm.teamcity.internal;

import groovy.util.Node;
import groovy.xml.XmlParser;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.FileCopyDetails;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PluginExecutableFilesValidationAction implements Action<Task> {

    private static final String MISSING_EXECUTABLE_FILE_WARNING = "%s: Executable file %s is missing.";

    private final Set<FileCopyDetails> files;

    public PluginExecutableFilesValidationAction(Set<FileCopyDetails> files) {
        this.files = files;
    }

    @Override
    public void execute(Task task) {
        AbstractPluginTask pluginTask = (AbstractPluginTask) task;
        List<String> paths = files.stream()
            .map(FileCopyDetails::getPath)
            .collect(Collectors.toList());

        List<String> executableFiles = getExecutableFiles(pluginTask.getDescriptor().get().getAsFile());
        for (String executableFile : executableFiles) {
            if (!paths.contains(executableFile)) {
                task.getLogger().warn(String.format(MISSING_EXECUTABLE_FILE_WARNING, task.getPath(), executableFile));
            }
        }
    }

    public static List<String> getExecutableFiles(File descriptorFile) {
        XmlParser parser = ValidationSupport.createXmlParser();
        try {
            Node descriptor = parser.parse(descriptorFile);
            List<Node> nodes = descriptor.breadthFirst();
            return nodes.stream()
                .filter((node) -> node.name().equals("include"))
                .map((node) -> (String) node.attribute("name"))
                .collect(Collectors.toList());
        }
        catch (IOException | SAXException e) {
            throw new GradleException("Failure parsing descriptor", e);
        }
    }
}
