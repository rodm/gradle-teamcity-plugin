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

import com.github.rodm.teamcity.internal.DefaultTeamCityEnvironments
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

/**
 * TeamCity Plugin extension.
 */
class TeamCityPluginExtension {

    private String version

    private boolean defaultRepositories = true
    private boolean allowSnapshotVersions = false
    private ValidationMode validateBeanDefinition = ValidationMode.WARN

    private AgentPluginConfiguration agent

    private ServerPluginConfiguration server

    private TeamCityEnvironments environments

    private Project project

    TeamCityPluginExtension(Project project) {
        this.project = project
    }

    void init() {
        def extensions = (this as ExtensionAware).extensions
        this.environments = extensions.create(TeamCityEnvironments, 'environments', DefaultTeamCityEnvironments, project)
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
    }

    String getVersion() {
        if (!version) {
            if (!isRootProject(project)) {
                def rootExtension = project.rootProject.extensions.findByType(TeamCityPluginExtension)
                if (rootExtension) {
                    version = rootExtension.version
                }
            }
        }
        (version) ?: '9.0'
    }

    /**
     * Use default repositories to resolve dependencies.
     *
     * @param useDefaultRepositories
     */
    void setDefaultRepositories(boolean useDefaultRepositories) {
        defaultRepositories = useDefaultRepositories
    }

    boolean getDefaultRepositories() {
        return defaultRepositories
    }

    /**
     * Allow version to include snapshot versions.
     *
     * @param allowSnapshots
     */
    void setAllowSnapshotVersions(boolean allowSnapshots) {
        allowSnapshotVersions = allowSnapshots
    }

    boolean getAllowSnapshotVersions() {
        return allowSnapshotVersions
    }

    /**
     * Set the validation mode for validating plugin bean definition files
     *
     * @param mode
     */
    void setValidateBeanDefinition(ValidationMode mode) {
        validateBeanDefinition = mode
    }

    void setValidateBeanDefinition(String mode) {
        validateBeanDefinition = ValidationMode.valueOf(mode.toUpperCase())
    }

    ValidationMode getValidateBeanDefinition() {
        return validateBeanDefinition
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

    private static boolean isRootProject(Project project) {
        project.rootProject == project
    }
}
