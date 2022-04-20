/*
 * Copyright 2015 Rod MacKenzie
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
        buildDeploymentNode(root, descriptor.getDeployment());
        buildDependenciesNode(root, descriptor.getDependencies());
        XmlUtil.serialize(root, writer);
    }

    private void buildDeploymentNode(Node root, Deployment deployment) {
        if (deployment != null) {
            if (deployment instanceof PluginDeployment) {
                buildPluginDeploymentNode(root, (PluginDeployment) deployment);
            } else {
                buildToolDeploymentNode(root, deployment);
            }
        }
    }

    private void buildPluginDeploymentNode(Node root, PluginDeployment deployment) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (deployment.getUseSeparateClassloader() != null) {
            attributes.put("use-separate-classloader", deployment.getUseSeparateClassloader());
        }
        Node deploymentNode = root.appendNode("plugin-deployment", attributes);
        buildLayoutNode(deploymentNode, deployment.getExecutableFiles());
    }

    private void buildToolDeploymentNode(Node root, Deployment deployment) {
        Node toolNode = root.appendNode("tool-deployment");
        buildLayoutNode(toolNode, deployment.getExecutableFiles());
    }

    private void buildLayoutNode(Node deploymentNode, ExecutableFiles executableFiles) {
        if (executableFiles.hasFiles()) {
            Node layoutNode = deploymentNode.appendNode("layout");
            final Node executableFilesNode = layoutNode.appendNode("executable-files");
            executableFiles.getIncludes().forEach(name ->
                executableFilesNode.appendNode("include", Collections.singletonMap("name", name)));
        }
    }

    private void buildDependenciesNode(Node root, Dependencies dependencies) {
        if (dependencies.hasDependencies()) {
            Node dependenciesNode = root.appendNode("dependencies");
            dependencies.getPlugins().forEach(name ->
                dependenciesNode.appendNode("plugin", Collections.singletonMap("name", name)));
            dependencies.getTools().forEach(name ->
                dependenciesNode.appendNode("tool", Collections.singletonMap("name", name)));
        }
    }
}
