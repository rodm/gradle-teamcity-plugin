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

import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2;
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2020_1;
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_9_0;

public class ServerPluginDescriptorGenerator {

    private ServerPluginDescriptor descriptor;

    private TeamCityVersion version;

    public ServerPluginDescriptorGenerator(ServerPluginDescriptor descriptor, TeamCityVersion version) {
        this.descriptor = descriptor;
        this.version = version;
    }

    public void writeTo(Writer writer) {
        LinkedHashMap<String, String> attributes = new LinkedHashMap<String, String>(2);
        attributes.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        attributes.put("xsi:noNamespaceSchemaLocation", "urn:schemas-jetbrains-com:teamcity-plugin-v1-xml");
        Node root = new Node(null, "teamcity-plugin", attributes);
        buildInfoNode(root);
        buildRequirementsNode(root);
        buildDeploymentNode(root);
        buildParametersNode(root);
        buildDependenciesNode(root);
        XmlUtil.serialize(root, writer);
    }

    private void buildInfoNode(Node root) {
        Node info = root.appendNode("info");
        info.appendNode("name", descriptor.getName());
        info.appendNode("display-name", descriptor.getDisplayName());
        info.appendNode("version", descriptor.getVersion());
        if (descriptor.getDescription() != null) {
            info.appendNode("description", descriptor.getDescription());
        }
        if (descriptor.getDownloadUrl() != null) {
            info.appendNode("download-url", descriptor.getDownloadUrl());
        }
        if (descriptor.getEmail() != null) {
            info.appendNode("email", descriptor.getEmail());
        }
        buildVendorNode(info);
    }

    private void buildVendorNode(Node info) {
        Node vendor = info.appendNode("vendor");
        vendor.appendNode("name", descriptor.getVendorName());
        if (descriptor.getVendorUrl() != null) {
            vendor.appendNode("url", descriptor.getVendorUrl());
        }
        if (descriptor.getVendorLogo() != null) {
            vendor.appendNode("logo", descriptor.getVendorLogo());
        }
    }

    private void buildRequirementsNode(Node root) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (descriptor.getMinimumBuild() != null) {
            attributes.put("min-build", descriptor.getMinimumBuild());
        }
        if (descriptor.getMaximumBuild() != null) {
            attributes.put("max-build", descriptor.getMaximumBuild());
        }
        if (attributes.size() > 0) {
            root.appendNode("requirements", attributes);
        }
    }

    private void buildDeploymentNode(Node root) {
        Map<String, Boolean> attributes = new LinkedHashMap<>();
        if (descriptor.getUseSeparateClassloader() != null) {
            attributes.put("use-separate-classloader", descriptor.getUseSeparateClassloader());
        }
        if (version.equalOrGreaterThan(VERSION_2018_2) && descriptor.getAllowRuntimeReload() != null) {
            attributes.put("allow-runtime-reload", descriptor.getAllowRuntimeReload());
        }
        if (version.equalOrGreaterThan(VERSION_2020_1) && descriptor.getNodeResponsibilitiesAware() != null) {
            attributes.put("node-responsibilities-aware", descriptor.getNodeResponsibilitiesAware());
        }
        if (attributes.size() > 0) {
            root.appendNode("deployment", attributes);
        }
    }

    private void buildParametersNode(Node root) {
        if (descriptor.getParameters().getParameters().size() > 0) {
            final Node parameters = root.appendNode("parameters");
            descriptor.getParameters().getParameters().forEach((name, value) -> {
                parameters.appendNode("parameter", Collections.singletonMap("name", name),value);
            });
        }
    }

    private void buildDependenciesNode(Node root) {
        if (version.equalOrGreaterThan(VERSION_9_0)) {
            if (descriptor.getDependencies().hasDependencies()) {
                final Node dependencies = root.appendNode("dependencies");
                descriptor.getDependencies().getPlugins().forEach(name -> {
                    dependencies.appendNode("plugin", Collections.singletonMap("name", name));
                });
                descriptor.getDependencies().getTools().forEach(name -> {
                    dependencies.appendNode("tool", Collections.singletonMap("name", name));
                });
            }
        }
    }
}
