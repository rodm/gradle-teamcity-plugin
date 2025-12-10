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

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class PluginDescriptorValidationAction implements Action<Task> {

    private final String name;

    public PluginDescriptorValidationAction(String name) {
        this.name = name;
    }

    @Override
    public void execute(Task task) {
        AbstractPluginTask pluginTask = (AbstractPluginTask) task;
        Path descriptorPath = pluginTask.getDescriptor().getAsFile().get().toPath();
        try (Reader reader = Files.newBufferedReader(descriptorPath)) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            setProperty(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA);
            setProperty(factory, XMLConstants.ACCESS_EXTERNAL_DTD);
            URL url = this.getClass().getResource("/schema/" + name);
            Schema schema = factory.newSchema(url);
            Validator validator = schema.newValidator();
            PluginDescriptorErrorHandler errorHandler = new PluginDescriptorErrorHandler(task);
            validator.setErrorHandler(errorHandler);
            validator.validate(new StreamSource(reader));
        }
        catch (IOException | SAXException e) {
            throw new GradleException("Failure validating descriptor", e);
        }
    }

    private static void setProperty(SchemaFactory factory, String uri) {
        try {
            factory.setProperty(uri, "");
        }
        catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            // ignore
        }
    }
}
