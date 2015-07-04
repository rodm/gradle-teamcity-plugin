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
package com.github.rodm.teamcity

import groovy.xml.XmlUtil

class AgentPluginDescriptorGenerator {

    private AgentPluginDescriptor descriptor

    AgentPluginDescriptorGenerator(AgentPluginDescriptor descriptor) {
        this.descriptor = descriptor
    }

    public void writeTo(Writer writer) {
        Node root = new Node(null, 'teamcity-agent-plugin')
        buildDeploymentNode(root)
        buildDependenciesNode(root)
        XmlUtil.serialize(root, writer)
    }

    private void buildDeploymentNode(Node root) {
        if (descriptor.deployment) {
            if (descriptor.deployment instanceof PluginDeployment) {
                buildPluginDeploymentNode(root)
            } else {
                buildToolDeploymentNode(root)
            }
        }
    }

    private void buildPluginDeploymentNode(Node root) {
        Map<String, Object> attributes = [:]
        if (descriptor.deployment.useSeparateClassloader != null)
            attributes << ['use-separate-classloader': descriptor.deployment.useSeparateClassloader]
        Node deployment = root.appendNode('plugin-deployment', attributes)
        buildLayoutNode(deployment)
    }

    private void buildToolDeploymentNode(Node root) {
        Node tool = root.appendNode('tool-deployment')
        buildLayoutNode(tool)
    }

    private void buildLayoutNode(Node deployment) {
        Node layout = deployment.appendNode('layout')
        Node executableFiles = layout.appendNode('executable-files')
        descriptor.deployment.getExecutableFiles().includes.each { name ->
            executableFiles.appendNode('include', ['name': name])
        }
    }

    private void buildDependenciesNode(Node root) {
        if (descriptor.getDependencies().hasDependencies()) {
            Node dependencies = root.appendNode('dependencies')
            descriptor.getDependencies().plugins.each { name ->
                dependencies.appendNode('plugin', ['name': name])
            }
            descriptor.getDependencies().tools.each { name ->
                dependencies.appendNode('tool', ['name': name])
            }
        }
    }
}
