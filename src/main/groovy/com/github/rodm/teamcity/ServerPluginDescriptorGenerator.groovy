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

import groovy.transform.CompileStatic
import groovy.xml.XmlUtil

import static groovy.transform.TypeCheckingMode.SKIP

@CompileStatic
class ServerPluginDescriptorGenerator {

    private ServerPluginDescriptor descriptor

    private Integer version

    ServerPluginDescriptorGenerator(ServerPluginDescriptor descriptor, Integer version) {
        this.descriptor = descriptor
        this.version = version
    }

    void writeTo(Writer writer) {
        Node root = new Node(null, "teamcity-plugin", [
                "xmlns:xsi": "http://www.w3.org/2001/XMLSchema-instance",
                "xsi:noNamespaceSchemaLocation": "urn:schemas-jetbrains-com:teamcity-plugin-v1-xml"
        ])
        buildInfoNode(root)
        buildRequirementsNode(root)
        buildDeploymentNode(root)
        buildParametersNode(root)
        buildDependenciesNode(root)
        XmlUtil.serialize(root, writer)
    }

    private void buildInfoNode(Node root) {
        Node info = root.appendNode('info')
        def name = descriptor.getName()
        def displayName = descriptor.getDisplayName()
        def version = descriptor.getVersion()
        info.appendNode('name', name)
        info.appendNode('display-name', displayName)
        info.appendNode('version', version)
        if (descriptor.getDescription())
            info.appendNode('description', descriptor.getDescription())
        if (descriptor.getDownloadUrl())
            info.appendNode('download-url', descriptor.getDownloadUrl())
        if (descriptor.getEmail())
            info.appendNode('email', descriptor.getEmail())
        buildVendorNode(info)
    }

    private void buildVendorNode(Node info) {
        Node vendor = info.appendNode('vendor')
        vendor.appendNode('name', descriptor.getVendorName())
        if (descriptor.getVendorUrl())
            vendor.appendNode('url', descriptor.getVendorUrl())
        if (descriptor.getVendorLogo())
            vendor.appendNode('logo', descriptor.getVendorLogo())
    }

    private void buildRequirementsNode(Node root) {
        Map<String, String> attributes = [:]
        if (descriptor.getMinimumBuild())
            attributes << ['min-build': descriptor.getMinimumBuild()]
        if (descriptor.getMaximumBuild())
            attributes << ['max-build': descriptor.getMaximumBuild()]
        if (attributes.size() > 0)
            root.appendNode('requirements', attributes)
    }

    private void buildDeploymentNode(Node root) {
        if (descriptor.getUseSeparateClassloader() != null)
            root.appendNode('deployment', ['use-separate-classloader': descriptor.getUseSeparateClassloader()])
    }

    private void buildParametersNode(Node root) {
        if (descriptor.getParameters().parameters.size() > 0) {
            Node parameters = root.appendNode('parameters')
            descriptor.getParameters().parameters.each { name, value ->
                parameters.appendNode('parameter', ['name': name], value)
            }
        }
    }

    private void buildDependenciesNode(Node root) {
        if (version == null || version >= 9) {
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
}
