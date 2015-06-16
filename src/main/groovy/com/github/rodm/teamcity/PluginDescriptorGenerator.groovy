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

class PluginDescriptorGenerator {

    private PluginDescriptor descriptor

    PluginDescriptorGenerator(PluginDescriptor descriptor) {
        this.descriptor = descriptor
    }

    public void writeTo(Writer writer) {
        Node root = new Node(null, "teamcity-plugin", [
                "xmlns:xsi": "http://www.w3.org/2001/XMLSchema-instance",
                "xsi:noNamespaceSchemaLocation": "urn:schemas-jetbrains-com:teamcity-plugin-v1-xml"
        ])
        buildInfoNode(root)
        buildDeploymentNode(root)
        buildParametersNode(root)
        XmlUtil.serialize(root, writer)
    }

    private void buildInfoNode(Node root) {
        Node info = new Node(root, "info")
        new Node(info, "name", descriptor.getName())
        new Node(info, "display-name", descriptor.getDisplayName())
        new Node(info, "version", descriptor.getVersion())
        if (descriptor.getDescription())
            new Node(info, "description", descriptor.getDescription())
        if (descriptor.getDownloadUrl())
            new Node(info, "download-url", descriptor.getDownloadUrl())
        if (descriptor.getEmail())
            new Node(info, "email", descriptor.getEmail())
        buildVendorNode(info)
    }

    private void buildVendorNode(Node info) {
        Node vendor = new Node(info, "vendor")
        new Node(vendor, "name", descriptor.getVendorName());
        if (descriptor.getVendorUrl())
            new Node(vendor, "url", descriptor.getVendorUrl())
        if (descriptor.getVendorLogo())
            new Node(vendor, "logo", descriptor.getVendorLogo())
    }

    private void buildDeploymentNode(Node root) {
        if (descriptor.getUseSeparateClassloader() != null)
            new Node(root, "deployment", ['use-separate-classloader': descriptor.getUseSeparateClassloader()])
    }

    private void buildParametersNode(Node root) {
        if (descriptor.getParameters().parameters.size() > 0) {
            Node parameters = new Node(root, "parameters")
            descriptor.getParameters().parameters.each { name, value ->
                new Node(parameters, "parameter", ['name': name], value)
            }
        }
    }
}
