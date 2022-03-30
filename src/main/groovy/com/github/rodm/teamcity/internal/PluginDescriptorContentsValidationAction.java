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

import groovy.namespace.QName;
import groovy.util.NodeList;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;

import groovy.util.Node;
import groovy.xml.XmlParser;
import org.xml.sax.SAXException;

import java.io.IOException;

public class PluginDescriptorContentsValidationAction implements Action<Task> {

    private static final String EMPTY_VALUE_WARNING_MESSAGE = "{}: Plugin descriptor value for {} must not be empty.";

    @Override
    public void execute(Task task) {
        AbstractPluginTask pluginTask = (AbstractPluginTask) task;
        XmlParser parser = ValidationSupport.createXmlParser();
        try {
            Node descriptor = parser.parse(pluginTask.getDescriptor().get().getAsFile());
            NodeList info = descriptor.getAt(QName.valueOf("info"));

            final String path = task.getPath();
            NodeList name = info.getAt("name");
            if (name.text().trim().isEmpty()) {
                task.getLogger().warn(EMPTY_VALUE_WARNING_MESSAGE, path, "name");
            }
            NodeList display = info.getAt("display-name");
            if (display.text().trim().isEmpty()) {
                task.getLogger().warn(EMPTY_VALUE_WARNING_MESSAGE, path, "display name");
            }
            NodeList version = info.getAt("version");
            if (version.text().trim().isEmpty()) {
                task.getLogger().warn(EMPTY_VALUE_WARNING_MESSAGE, path, "version");
            }

            NodeList vendor = info.getAt("vendor");
            NodeList vendorName = vendor.getAt("name");
            if (vendorName.text().trim().isEmpty()) {
                task.getLogger().warn(EMPTY_VALUE_WARNING_MESSAGE, path, "vendor name");
            }

            NodeList description = info.getAt("description");
            if (description.text().trim().isEmpty()) {
                task.getLogger().warn(EMPTY_VALUE_WARNING_MESSAGE, path, "description");
            }

            NodeList vendorUrl = vendor.getAt("url");
            if (vendorUrl.text().trim().isEmpty()) {
                task.getLogger().warn(EMPTY_VALUE_WARNING_MESSAGE, path, "vendor url");
            }
        }
        catch (IOException | SAXException e) {
            throw new GradleException("Failure parsing descriptor", e);
        }
    }
}
