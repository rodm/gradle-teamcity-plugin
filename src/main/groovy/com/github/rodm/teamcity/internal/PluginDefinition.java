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
import org.gradle.api.GradleException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PluginDefinition {

    private final File definitionFile;

    public PluginDefinition(File file) {
        this.definitionFile = file;
    }

    public String getName() {
        return this.definitionFile.getName();
    }

    public List<PluginBean> getBeans(boolean offline) throws IOException {
        XmlParser parser = ValidationSupport.createXmlParser(offline);
        try {
            Node beans = parser.parse(definitionFile);
            List<PluginBean> pluginBeans = ((List<Node>) beans.children()).stream()
                .filter((Node node) -> node.name().equals("bean"))
                .map((Node node) -> new PluginBean((String) node.attribute("id"), (String) node.attribute("class")))
                .collect(Collectors.toList());
            return pluginBeans;
        }
        catch (SAXException e) {
            throw new GradleException("Failure parsing bean definition file", e);
        }
    }
}
