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
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;

/**
 * Agent-side plugin descriptor
 */
public class AgentPluginDescriptor {

    @Nested
    @Optional
    private PluginDeployment pluginDeployment;

    @Nested
    @Optional
    private ToolDeployment toolDeployment;

    @Nested
    private final Dependencies dependencies;

    private final ObjectFactory objects;

    @Inject
    public AgentPluginDescriptor(ObjectFactory objects) {
        this.objects = objects;
        this.dependencies = objects.newInstance(Dependencies.class);
    }

    /**
     * Configures the agent-side plugin for plugin deployment.
     *
     * <p>The given action is executed to configure the agent-side plugin deployment.</p>
     *
     * @param configuration The action
     */
    public void pluginDeployment(Action<PluginDeployment> configuration) {
        if (toolDeployment != null)
            throw new InvalidUserDataException("Agent plugin cannot be configured for plugin deployment and tool deployment");
        if (pluginDeployment == null) {
            pluginDeployment = objects.newInstance(PluginDeployment.class);
        }
        configuration.execute(pluginDeployment);
    }

    public PluginDeployment getPluginDeployment() {
        return pluginDeployment;
    }

    /**
     * Configures the agent-side plugin for tool deployment.
     *
     * <p>The given action is executed to configure the agent-side plugin deployment.</p>
     *
     * @param configuration The action
     */
    public void toolDeployment(Action<ToolDeployment> configuration) {
        if (pluginDeployment != null)
            throw new InvalidUserDataException("Agent plugin cannot be configured for plugin deployment and tool deployment");
        if (toolDeployment == null) {
            toolDeployment = objects.newInstance(ToolDeployment.class);
        }
        configuration.execute(toolDeployment);
    }

    public ToolDeployment getToolDeployment() {
        return toolDeployment;
    }

    @Internal
    public Deployment getDeployment() {
        return (pluginDeployment != null) ? pluginDeployment : toolDeployment;
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

    public Dependencies getDependencies() {
        return dependencies;
    }
}
