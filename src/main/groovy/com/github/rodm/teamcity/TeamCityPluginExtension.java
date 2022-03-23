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
package com.github.rodm.teamcity;

import com.github.rodm.teamcity.internal.DefaultTeamCityEnvironments;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;

import java.util.Map;

/**
 * TeamCity Plugin extension.
 */
public class TeamCityPluginExtension {

    private String version;

    private Boolean defaultRepositories;
    private Boolean allowSnapshotVersions;
    private ValidationMode validateBeanDefinition;

    private AgentPluginConfiguration agent;

    private ServerPluginConfiguration server;

    private TeamCityEnvironments environments;

    private Project project;

    public TeamCityPluginExtension(Project project) {
        this.project = project;
    }

    public void init() {
        ExtensionContainer extensions = ((ExtensionAware) this).getExtensions();
        this.environments = extensions.create(TeamCityEnvironments.class, "environments", DefaultTeamCityEnvironments.class, project);
        this.agent = extensions.create("agent", AgentPluginConfiguration.class, project);
        this.server = extensions.create("server", ServerPluginConfiguration.class, project, environments);
    }

    /**
     * The version of the TeamCity API.
     *
     * @param version The API version.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        if (version == null) {
            TeamCityPluginExtension rootExtension = getRootExtension(project);
            if (rootExtension != null) {
                version = rootExtension.getVersion();
            }
        }
        return (version != null) ? version : "9.0";
    }

    /**
     * Use default repositories to resolve dependencies.
     *
     * @param useDefaultRepositories
     */
    public void setDefaultRepositories(boolean useDefaultRepositories) {
        defaultRepositories = useDefaultRepositories;
    }

    public boolean getDefaultRepositories() {
        if (defaultRepositories == null) {
            TeamCityPluginExtension rootExtension = getRootExtension(project);
            if (rootExtension != null) {
                defaultRepositories = rootExtension.getDefaultRepositories();
            }
        }
        return (defaultRepositories != null) ? defaultRepositories : true;
    }

    /**
     * Allow version to include snapshot versions.
     *
     * @param allowSnapshots
     */
    public void setAllowSnapshotVersions(boolean allowSnapshots) {
        allowSnapshotVersions = allowSnapshots;
    }

    public boolean getAllowSnapshotVersions() {
        if (allowSnapshotVersions == null) {
            TeamCityPluginExtension rootExtension = getRootExtension(project);
            if (rootExtension != null) {
                allowSnapshotVersions = rootExtension.getAllowSnapshotVersions();
            }
        }
        return (allowSnapshotVersions != null) ? allowSnapshotVersions : false;
    }

    /**
     * Set the validation mode for validating plugin bean definition files
     *
     * @param mode
     */
    public void setValidateBeanDefinition(ValidationMode mode) {
        validateBeanDefinition = mode;
    }

    public void setValidateBeanDefinition(String mode) {
        validateBeanDefinition = ValidationMode.valueOf(mode.toUpperCase());
    }

    public ValidationMode getValidateBeanDefinition() {
        if (validateBeanDefinition == null) {
            TeamCityPluginExtension rootExtension = getRootExtension(project);
            if (rootExtension != null) {
                validateBeanDefinition = rootExtension.getValidateBeanDefinition();
            }
        }
        return (validateBeanDefinition != null) ? validateBeanDefinition : ValidationMode.WARN;
    }

    public AgentPluginConfiguration getAgent() {
        return agent;
    }

    /**
     * Configures the agent-side plugin.
     *
     * <p>The given action is executed to configure the agent-side plugin configuration.</p>
     *
     * @param configuration The action.
     */
    public void agent(Action<AgentPluginConfiguration> configuration) {
        if (!project.getPlugins().hasPlugin(TeamCityAgentPlugin.class))
            throw new InvalidUserDataException("Agent plugin configuration is invalid for a project without the teamcity-agent plugin");
        configuration.execute(agent);
    }

    public ServerPluginConfiguration getServer() {
        return server;
    }

    /**
     * Configures the server-side plugin.
     *
     * <p>The given action is executed to configure the server-side plugin configuration.</p>
     *
     * @param configuration The action.
     */
    public void server(Action<ServerPluginConfiguration> configuration) {
        if (!project.getPlugins().hasPlugin(TeamCityServerPlugin.class))
            throw new InvalidUserDataException("Server plugin configuration is invalid for a project without the teamcity-server plugin");
        configuration.execute(server);
    }

    public Object setDescriptor(Object descriptor) {
        project.getLogger().warn("descriptor property is deprecated. Please use the descriptor property in the agent or server configuration.");
        if (project.getPlugins().hasPlugin(TeamCityAgentPlugin.class)) {
            agent.setDescriptor(descriptor);
            return descriptor;
        } else {
            server.setDescriptor(descriptor);
            return descriptor;
        }
    }

    public void descriptor(Action<?> configuration) {
        project.getLogger().warn("descriptor property is deprecated. Please use the descriptor property in the agent or server configuration.");
        if (project.getPlugins().hasPlugin(TeamCityAgentPlugin.class)) {
            agent.descriptor((Action<AgentPluginDescriptor>) configuration);
        } else {
            server.descriptor((Action<ServerPluginDescriptor>) configuration);
        }
    }

    public CopySpec files(Action<CopySpec> configuration) {
        project.getLogger().warn("files property is deprecated. Please use the files property in the agent or server configuration.");
        if (project.getPlugins().hasPlugin(TeamCityAgentPlugin.class)) {
            return agent.files(configuration);
        } else {
            return server.files(configuration);
        }
    }

    public Map<String, Object> setTokens(Map<String, Object> tokens) {
        project.getLogger().warn("tokens property is deprecated. Please use the tokens property in the agent or server configuration.");
        if (project.getPlugins().hasPlugin(TeamCityAgentPlugin.class)) {
            agent.setTokens(tokens);
            return tokens;
        } else {
            server.setTokens(tokens);
            return tokens;
        }
    }

    public Map<String, Object> tokens(Map<String, Object> tokens) {
        project.getLogger().warn("tokens property is deprecated. Please use the tokens property in the agent or server configuration.");
        if (project.getPlugins().hasPlugin(TeamCityAgentPlugin.class)) {
            return agent.tokens(tokens);
        } else {
            return server.tokens(tokens);
        }
    }

    public TeamCityEnvironments getEnvironments() {
        return environments;
    }

    /**
     * Configures the TeamCity environments.
     *
     * <p>The given action is executed to configure the environments.</p>
     *
     * @param configuration The action.
     */
    public void environments(Action<TeamCityEnvironments> configuration) {
        if (!project.getPlugins().hasPlugin(TeamCityEnvironmentsPlugin.class)) {
            project.getLogger().warn("Configuring environments with the teamcity-server plugin is deprecated. Please use the teamcity-environments plugin.");
        }
        configuration.execute(environments);
    }

    private static TeamCityPluginExtension getRootExtension(Project project) {
        return !isRootProject(project) ? project.getRootProject().getExtensions().findByType(TeamCityPluginExtension.class) : null;
    }

    private static boolean isRootProject(Project project) {
        return project.getRootProject().equals(project);
    }
}
