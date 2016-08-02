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

import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

class TeamCityPluginExtension {

    String version = '9.0'

    boolean defaultRepositories = true

    private AgentPluginConfiguration agent

    private ServerPluginConfiguration server

    private Project project

    TeamCityPluginExtension(Project project, NamedDomainObjectContainer<TeamCityEnvironment> environments) {
        this.project = project
        this.agent = new AgentPluginConfiguration(project.copySpec {})
        this.server = new ServerPluginConfiguration(project, environments)
    }

    def getAgent() {
        return agent
    }

    def agent(Closure closure) {
        if (!project.plugins.hasPlugin(TeamCityAgentPlugin))
            throw new InvalidUserDataException('Agent plugin configuration is invalid for a project without the teamcity-agent plugin')
        ConfigureUtil.configure(closure, agent)
    }

    def getServer() {
        return server
    }

    def server(Closure closure) {
        if (!project.plugins.hasPlugin(TeamCityServerPlugin))
            throw new InvalidUserDataException('Server plugin configuration is invalid for a project without the teamcity-server plugin')
        ConfigureUtil.configure(closure, server)
    }

    def setDescriptor(Object descriptor) {
        if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
            agent.descriptor = descriptor
        } else {
            server.descriptor = descriptor
        }
    }

    def getDescriptor() {
        if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
            return agent.descriptor
        } else {
            return server.descriptor
        }
    }

    def descriptor(Closure closure) {
        if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
            agent.descriptor(closure)
        } else {
            server.descriptor(closure)
        }
    }

    def files(Closure closure) {
        if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
            agent.files(closure)
        } else {
            server.files(closure)
        }
    }

    def setTokens(Map<String, Object> tokens) {
        if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
            agent.tokens = tokens
        } else {
            server.tokens = tokens
        }
    }

    def tokens(Map<String, Object> tokens) {
        if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
            agent.tokens tokens
        } else {
            server.tokens tokens
        }
    }
}
