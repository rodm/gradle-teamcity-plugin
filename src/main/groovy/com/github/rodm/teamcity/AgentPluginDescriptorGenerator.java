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
package com.github.rodm.teamcity;

import groovy.util.Node;
import groovy.xml.XmlUtil;

import java.io.Writer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AgentPluginDescriptorGenerator {

    private final AgentPluginDescriptor descriptor;

    public AgentPluginDescriptorGenerator(AgentPluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public void writeTo(Writer writer) {
        Map<String, String> attributes = new LinkedHashMap<>(2);
        attributes.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        attributes.put("xsi:noNamespaceSchemaLocation", "urn:schemas-jetbrains-com:teamcity-agent-plugin-v1-xml");
        Node root = new Node(null, "teamcity-agent-plugin", attributes);
        buildDeploymentNode(root);
        buildDependenciesNode(root);
        XmlUtil.serialize(root, writer);
    }

    private void buildDeploymentNode(Node root) {
        if (descriptor.getDeployment() != null) {
            if (descriptor.getDeployment() instanceof PluginDeployment) {
                buildPluginDeploymentNode(root);
            } else {
                buildToolDeploymentNode(root);
            }
        }
    }

    private void buildPluginDeploymentNode(Node root) {
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        PluginDeployment deployment = (PluginDeployment) descriptor.getDeployment();
        if (deployment.getUseSeparateClassloader() != null) {
            attributes.put("use-separate-classloader", deployment.getUseSeparateClassloader());
        }
        Node deploymentNode = root.appendNode("plugin-deployment", attributes);
        buildLayoutNode(deploymentNode);
    }

    private void buildToolDeploymentNode(Node root) {
        Node tool = root.appendNode("tool-deployment");
        buildLayoutNode(tool);
    }

    private void buildLayoutNode(Node deployment) {
        ExecutableFiles executableFiles;
        if (descriptor.getDeployment() instanceof PluginDeployment) {
            executableFiles = ((PluginDeployment) descriptor.getDeployment()).getExecutableFiles();
        } else {
            executableFiles = ((ToolDeployment) descriptor.getDeployment()).getExecutableFiles();
        }
        if (executableFiles.hasFiles()) {
            Node layout = deployment.appendNode("layout");
            final Node executableFilesNode = layout.appendNode("executable-files");
            executableFiles.getIncludes().forEach(name -> {
                executableFilesNode.appendNode("include", Collections.singletonMap("name", name));
            });
        }
    }

    private void buildDependenciesNode(Node root) {
        if (descriptor.getDependencies().hasDependencies()) {
            Node dependencies = root.appendNode("dependencies");
            descriptor.getDependencies().getPlugins().forEach(name -> {
                dependencies.appendNode("plugin", Collections.singletonMap("name", name));
            });
            descriptor.getDependencies().getTools().forEach(name -> {
                dependencies.appendNode("tool", Collections.singletonMap("name", name));
            });
        }
    }
}
