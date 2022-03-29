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

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

/**
 * Server-side plugin descriptor
 */
public class ServerPluginDescriptor {

    private String name;
    private String displayName;
    private String version;

    /**
     * The description of the plugin. The description is shown in TeamCity's Plugins List.
     */
    @Input
    @Optional
    private String description;

    /**
     * The download URL for the plugin.
     */
    @Input
    @Optional
    private String downloadUrl;

    /**
     * The email of the plugin's developer.
     */
    @Input
    @Optional
    private String email;

    /**
     * The vendor name of the plugin.
     */
    @Input
    private String vendorName;

    /**
     * The URL for the vendor's plugin page.
     */
    @Input
    @Optional
    private String vendorUrl;

    /**
     * The image for vendor's logo.
     */
    @Input
    @Optional
    private String vendorLogo;

    /**
     * The minimum build of TeamCity that the plugin supports.
     */
    @Input
    @Optional
    private String minimumBuild;

    /**
     * The maximum build of TeamCity that the plugin supports.
     */
    @Input
    @Optional
    private String maximumBuild;

    /**
     * Use a separate classloader to load the server-side plugin's classes.
     */
    @Input
    @Optional
    private Boolean useSeparateClassloader;

    /**
     * Plugin supports being reloaded at runtime without restarting the TeamCity Server.
     */
    @Input
    @Optional
    private Boolean allowRuntimeReload;

    /**
     * Plugin is aware of node responsibilities. When set to true the plugin will be loaded by secondary nodes.
     */
    @Input
    @Optional
    private Boolean nodeResponsibilitiesAware;

    @Nested
    private Parameters parameters;

    @Nested
    private Dependencies dependencies;

    private final Project project;

    public ServerPluginDescriptor(Project project) {
        this.project = project;
    }

    public void init() {
        parameters = ((ExtensionAware) this).getExtensions().create("parameters", Parameters.class);
        dependencies = ((ExtensionAware) this).getExtensions().create("dependencies", Dependencies.class);
    }

    /**
     * The internal name of the plugin.
     */
    @Input
    public String getName() {
        return (name != null) ? name : project.getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The display name of the plugin. The display name is shown in TeamCity's Plugins List.
     */
    @Input
    public String getDisplayName() {
        return (displayName != null) ? displayName : project.getName();
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * The version of the plugin.
     */
    @Input
    public String getVersion() {
        return (version != null) ? version : String.valueOf(project.getVersion());
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Configures the parameters for the plugin.
     *
     * <p>The given action is executed to configure the plugin's parameters.</p>
     *
     * @param configuration The action
     */
    public void parameters(Action<Parameters> configuration) {
        configuration.execute(parameters);
    }

    /**
     * Configures the dependencies for the plugin.
     *
     * <p>The given action is executed to configure the plugin's dependencies.</p>
     *
     * @param configuration The action
     */
    public void dependencies(Action<Dependencies> configuration) {
        configuration.execute(dependencies);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getVendorUrl() {
        return vendorUrl;
    }

    public void setVendorUrl(String vendorUrl) {
        this.vendorUrl = vendorUrl;
    }

    public String getVendorLogo() {
        return vendorLogo;
    }

    public void setVendorLogo(String vendorLogo) {
        this.vendorLogo = vendorLogo;
    }

    public String getMinimumBuild() {
        return minimumBuild;
    }

    public void setMinimumBuild(String minimumBuild) {
        this.minimumBuild = minimumBuild;
    }

    public String getMaximumBuild() {
        return maximumBuild;
    }

    public void setMaximumBuild(String maximumBuild) {
        this.maximumBuild = maximumBuild;
    }

    public Boolean getUseSeparateClassloader() {
        return useSeparateClassloader;
    }

    public void setUseSeparateClassloader(Boolean useSeparateClassloader) {
        this.useSeparateClassloader = useSeparateClassloader;
    }

    public Boolean getAllowRuntimeReload() {
        return allowRuntimeReload;
    }

    public void setAllowRuntimeReload(Boolean allowRuntimeReload) {
        this.allowRuntimeReload = allowRuntimeReload;
    }

    public Boolean getNodeResponsibilitiesAware() {
        return nodeResponsibilitiesAware;
    }

    public void setNodeResponsibilitiesAware(Boolean nodeResponsibilitiesAware) {
        this.nodeResponsibilitiesAware = nodeResponsibilitiesAware;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public Dependencies getDependencies() {
        return dependencies;
    }
}
