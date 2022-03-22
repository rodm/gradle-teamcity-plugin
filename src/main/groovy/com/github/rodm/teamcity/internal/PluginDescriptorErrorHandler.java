/*
 * Copyright 2016 Rod MacKenzie
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

import org.gradle.api.Task;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class PluginDescriptorErrorHandler implements ErrorHandler {

    private final Task task;

    public PluginDescriptorErrorHandler(Task task) {
        this.task = task;
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        outputMessage(exception);
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        outputMessage(exception);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        outputMessage(exception);
    }

    private void outputMessage(SAXParseException exception) {
        task.getLogger().warn(task.getPath() + ": Plugin descriptor is invalid: " + exception.getMessage());
    }
}
