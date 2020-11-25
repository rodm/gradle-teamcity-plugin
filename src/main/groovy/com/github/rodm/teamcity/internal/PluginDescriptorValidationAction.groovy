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

import com.github.rodm.teamcity.PluginDescriptorErrorHandler
import org.gradle.api.Action
import org.gradle.api.Task

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

class PluginDescriptorValidationAction implements Action<Task> {

    private String schema

    PluginDescriptorValidationAction(String schema) {
        this.schema = schema
    }

    @Override
    void execute(Task task) {
        AbstractPluginTask pluginTask = (AbstractPluginTask) task
        def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        URL url = this.getClass().getResource('/schema/' + schema)
        def schema = factory.newSchema(url)
        def validator = schema.newValidator()
        def errorHandler = new PluginDescriptorErrorHandler(task)
        validator.setErrorHandler(errorHandler)
        validator.validate(new StreamSource(new FileReader(pluginTask.descriptor.get().asFile)))
    }
}
