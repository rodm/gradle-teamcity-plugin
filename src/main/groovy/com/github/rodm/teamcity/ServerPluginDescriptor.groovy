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
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

import static groovy.transform.TypeCheckingMode.SKIP

/**
 * Server-side plugin descriptor
 */
@CompileStatic
class ServerPluginDescriptor {

    private String name
    private String displayName
    private String version

    /**
     * The description of the plugin. The description is shown in TeamCity's Plugins List.
     */
    @Input
    @Optional
    String description

    /**
     * The download URL for the plugin.
     */
    @Input
    @Optional
    String downloadUrl

    /**
     * The email of the plugin's developer.
     */
    @Input
    @Optional
    String email

    /**
     * The vendor name of the plugin.
     */
    @Input
    String vendorName

    /**
     * The URL for the vendor's plugin page.
     */
    @Input
    @Optional
    String vendorUrl

    /**
     * The image for vendor's logo.
     */
    @Input
    @Optional
    String vendorLogo

    /**
     * The minimum build of TeamCity that the plugin supports.
     */
    @Input
    @Optional
    String minimumBuild

    /**
     * The maximum build of TeamCity that the plugin supports.
     */
    @Input
    @Optional
    String maximumBuild

    /**
     * Use a separate classloader to load the server-side plugin's classes.
     */
    @Input
    @Optional
    Boolean useSeparateClassloader

    /**
     * Plugin supports being reloaded at runtime without restarting the TeamCity Server.
     */
    @Input
    @Optional
    Boolean allowRuntimeReload

    @Nested
    Parameters parameters

    @Nested
    Dependencies dependencies

    private Project project

    @CompileStatic(SKIP)
    ServerPluginDescriptor(Project project) {
        this.project = project
        this.parameters = extensions.create('parameters', Parameters)
        this.dependencies = extensions.create('dependencies', Dependencies)
    }

    /**
     * The internal name of the plugin.
     */
    @Input
    String getName() {
        return name ? name : project.name
    }

    void setName(String name) {
        this.name = name
    }

    /**
     * The display name of the plugin. The display name is shown in TeamCity's Plugins List.
     */
    @Input
    String getDisplayName() {
        return displayName ? displayName : project.name
    }

    void setDisplayName(String displayName) {
        this.displayName = displayName
    }


    /**
     * The version of the plugin.
     */
    @Input
    String getVersion() {
        return version ? version : project.version
    }

    void setVersion(String version) {
        this.version = version
    }

    /**
     * Configures the parameters for the plugin.
     *
     * <p>The given action is executed to configure the plugin's parameters.</p>

     * @param configuration The action
     * @return
     */
    def parameters(Action<Parameters> configuration) {
        configuration.execute(parameters)
    }

    /**
     * Configures the dependencies for the plugin.
     *
     * <p>The given action is executed to configure the plugin's dependencies.</p>

     * @param configuration The action
     * @return
     */
    def dependencies(Action<Dependencies> configuration) {
        configuration.execute(dependencies)
    }
}
