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

class PluginDescriptor {

    String name

    String displayName

    String version

    String description

    String downloadUrl

    String email

    String vendorName

    String vendorUrl

    String vendorLogo

    def writeTo(Writer writer) {
        Node root = new Node(null, "teamcity-plugin", [
                "xmlns:xsi": "http://www.w3.org/2001/XMLSchema-instance",
                "xsi:noNamespaceSchemaLocation": "urn:schemas-jetbrains-com:teamcity-plugin-v1-xml"
        ])
        buildInfoNode(root)
        XmlUtil.serialize(root, writer)
    }

    private void buildInfoNode(Node root) {
        Node info = new Node(root, "info")
        new Node(info, "name", getName())
        new Node(info, "display-name", getDisplayName())
        new Node(info, "version", getVersion())
        if (getDescription())
            new Node(info, "description", getDescription())
        if (getDownloadUrl())
            new Node(info, "download-url", getDownloadUrl())
        if (getEmail())
            new Node(info, "email", getEmail())
        buildVendorNode(info)
    }

    private void buildVendorNode(Node info) {
        Node vendor = new Node(info, "vendor")
        new Node(vendor, "name", getVendorName());
        if (getVendorUrl())
            new Node(vendor, "url", getVendorUrl())
        if (getVendorLogo())
            new Node(vendor, "logo", getVendorLogo())
    }
}
