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
import org.gradle.api.InvalidUserDataException
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

/**
 * Agent-side plugin descriptor
 */
@CompileStatic
class AgentPluginDescriptor {

    @Nested
    @Optional
    PluginDeployment pluginDeployment

    @Nested
    @Optional
    ToolDeployment toolDeployment

    /**
     * Configures the agent-side plugin for plugin deployment.
     *
     * <p>The given action is executed to configure the agent-side plugin deployment.</p>

     * @param configuration The action
     * @return
     */
    def pluginDeployment(Action<PluginDeployment> configuration) {
        if (toolDeployment)
            throw new InvalidUserDataException('Agent plugin cannot be configured for plugin deployment and tool deployment')
        if (!pluginDeployment) {
            pluginDeployment = (this as ExtensionAware).extensions.create('deployment', PluginDeployment)
            pluginDeployment.init()
        }
        configuration.execute(pluginDeployment)
    }

    /**
     * Configures the agent-side plugin for tool deployment.
     *
     * <p>The given action is executed to configure the agent-side plugin deployment.</p>

     * @param configuration The action
     * @return
     */
    def toolDeployment(Action<ToolDeployment> configuration) {
        if (pluginDeployment)
            throw new InvalidUserDataException('Agent plugin cannot be configured for plugin deployment and tool deployment')
        if (!toolDeployment) {
            toolDeployment = (this as ExtensionAware).extensions.create('deployment', ToolDeployment)
            toolDeployment.init()
        }
        configuration.execute(toolDeployment)
    }

    @Internal
    def getDeployment() {
        return (pluginDeployment != null) ? pluginDeployment : toolDeployment
    }

    @Nested
    Dependencies dependencies

    void init() {
        dependencies = (this as ExtensionAware).extensions.create('dependencies', Dependencies)
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
