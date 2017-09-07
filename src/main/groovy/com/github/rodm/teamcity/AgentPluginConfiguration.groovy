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

import org.gradle.api.Action
import org.gradle.api.Project

/**
 * Agent-side plugin configuration
 */
class AgentPluginConfiguration extends PluginConfiguration {

    AgentPluginConfiguration(Project project) {
        super(project.copySpec())
    }

    /**
     * Configures the agent-side plugin descriptor for the TeamCity plugin.
     *
     * <p>The given action is executed to configure the agent-side plugin descriptor.</p>
     *
     * @param configuration The action.
     */
    def descriptor(Action<AgentPluginDescriptor> configuration) {
        descriptor = extensions.create('descriptor', AgentPluginDescriptor)
        configuration.execute(descriptor)
    }
}
