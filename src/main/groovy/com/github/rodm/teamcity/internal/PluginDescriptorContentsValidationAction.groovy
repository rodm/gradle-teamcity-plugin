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

import static ValidationSupport.createXmlParser

class PluginDescriptorContentsValidationAction implements Action<Task> {

    static final String EMPTY_VALUE_WARNING_MESSAGE = "%s: Plugin descriptor value for %s must not be empty."

    @Override
    void execute(Task task) {
        AbstractPluginTask pluginTask = (AbstractPluginTask) task
        def parser = createXmlParser()
        def descriptor = parser.parse(pluginTask.descriptor.get().asFile)

        if (descriptor.info.name.text().trim().isEmpty()) {
            task.logger.warn(String.format(EMPTY_VALUE_WARNING_MESSAGE, task.getPath(), 'name'))
        }
        if (descriptor.info.'display-name'.text().trim().isEmpty()) {
            task.logger.warn(String.format(EMPTY_VALUE_WARNING_MESSAGE, task.getPath(), 'display name'))
        }
        if (descriptor.info.version.text().trim().isEmpty()) {
            task.logger.warn(String.format(EMPTY_VALUE_WARNING_MESSAGE, task.getPath(), 'version'))
        }
        if (descriptor.info.vendor.name.text().trim().isEmpty()) {
            task.logger.warn(String.format(EMPTY_VALUE_WARNING_MESSAGE, task.getPath(), 'vendor name'))
        }
        if (descriptor.info.description.text().trim().isEmpty()) {
            task.logger.warn(String.format(EMPTY_VALUE_WARNING_MESSAGE, task.getPath(), 'description'))
        }
        if (descriptor.info.vendor.url.text().trim().isEmpty()) {
            task.logger.warn(String.format(EMPTY_VALUE_WARNING_MESSAGE, task.getPath(), 'vendor url'))
        }
    }
}
