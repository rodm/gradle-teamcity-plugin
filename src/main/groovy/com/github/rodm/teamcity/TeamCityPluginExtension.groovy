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
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project

/**
 * TeamCity Plugin extension.
 */
class TeamCityPluginExtension {

    private String version = '9.0'

    boolean defaultRepositories = true

    private AgentPluginConfiguration agent

    private ServerPluginConfiguration server

    private TeamCityEnvironments environments

    private Project project

    TeamCityPluginExtension(Project project) {
        this.project = project
        this.environments = extensions.create('environments', TeamCityEnvironments, project)
        this.agent = extensions.create('agent', AgentPluginConfiguration, project)
        this.server = extensions.create('server', ServerPluginConfiguration, project, environments)
    }

    /**
     * The version of the TeamCity API.
     *
     * @param configuration The action.
     */
     void setVersion(String version) {
        this.version = version
        TeamCityVersion.version(version)
    }

    String getVersion() {
        return version
    }

    def getAgent() {
        return agent
    }

    /**
     * Configures the agent-side plugin.
     *
     * <p>The given action is executed to configure the agent-side plugin configuration.</p>
     *
     * @param configuration The action.
     */
    def agent(Action<? extends AgentPluginConfiguration> configuration) {
        if (!project.plugins.hasPlugin(TeamCityAgentPlugin))
            throw new InvalidUserDataException('Agent plugin configuration is invalid for a project without the teamcity-agent plugin')
        configuration.execute(agent)
    }

    def getServer() {
        return server
    }

    /**
     * Configures the server-side plugin.
     *
     * <p>The given action is executed to configure the server-side plugin configuration.</p>
     *
     * @param configuration The action.
     */
    def server(Action<? extends ServerPluginConfiguration> configuration) {
        if (!project.plugins.hasPlugin(TeamCityServerPlugin))
            throw new InvalidUserDataException('Server plugin configuration is invalid for a project without the teamcity-server plugin')
        configuration.execute(server)
    }

    def setDescriptor(Object descriptor) {
        project.logger.warn('descriptor property is deprecated. Please use the descriptor property in the agent or server configuration.')
        if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
            agent.descriptor = descriptor
        } else {
            server.descriptor = descriptor
        }
    }

    def descriptor(Closure closure) {
        project.logger.warn('descriptor property is deprecated. Please use the descriptor property in the agent or server configuration.')
        if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
            agent.descriptor(closure)
        } else {
            server.descriptor(closure)
        }
    }

    def files(Closure closure) {
        project.logger.warn('files property is deprecated. Please use the files property in the agent or server configuration.')
        if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
            agent.files(closure)
        } else {
            server.files(closure)
        }
    }

    def setTokens(Map<String, Object> tokens) {
        project.logger.warn('tokens property is deprecated. Please use the tokens property in the agent or server configuration.')
        if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
            agent.tokens = tokens
        } else {
            server.tokens = tokens
        }
    }

    def tokens(Map<String, Object> tokens) {
        project.logger.warn('tokens property is deprecated. Please use the tokens property in the agent or server configuration.')
        if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
            agent.tokens tokens
        } else {
            server.tokens tokens
        }
    }

    TeamCityEnvironments getEnvironments() {
        return environments
    }

    /**
     * Configures the TeamCity environments.
     *
     * <p>The given action is executed to configure the environments.</p>
     *
     * @param configuration The action.
     */
    void environments(Action<TeamCityEnvironments> configuration) {
        if (!project.plugins.hasPlugin(TeamCityEnvironmentsPlugin)) {
            project.logger.warn('Configuring environments with the teamcity-server plugin is deprecated. Please use the teamcity-environments plugin.')
        }
        configuration.execute(environments)
    }
}
