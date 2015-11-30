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

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin

abstract class TeamCityPlugin implements Plugin<Project> {

    public static final String PLUGIN_DESCRIPTOR_FILENAME = 'teamcity-plugin.xml'

    public static final String PLUGIN_DESCRIPTOR_DIR = 'descriptor'

    static final String TEAMCITY_EXTENSION_NAME = 'teamcity'

    static final String JETBRAINS_MAVEN_REPOSITORY = 'http://repository.jetbrains.com/all'

    void apply(Project project) {
        project.plugins.apply(BasePlugin)

        if (project.plugins.hasPlugin(JavaPlugin) &&
                (project.plugins.hasPlugin(TeamCityAgentPlugin) || project.plugins.hasPlugin(TeamCityServerPlugin))) {
            throw new GradleException("Cannot apply both the teamcity-agent and teamcity-server plugins with the Java plugin")
        }

        TeamCityPluginExtension extension
        if (project.extensions.findByType(TeamCityPluginExtension)) {
            extension = project.extensions.getByType(TeamCityPluginExtension)
        } else {
            extension = project.extensions.create(TEAMCITY_EXTENSION_NAME, TeamCityPluginExtension, project)
        }
        configureRepositories(project)
        configureConfigurations(project)
        configureTasks(project, extension)
    }

    private configureRepositories(Project project) {
        project.plugins.withType(JavaPlugin) {
            project.repositories {
                mavenCentral()
                maven {
                    url = JETBRAINS_MAVEN_REPOSITORY
                }
            }
        }
    }

    void configureConfigurations(final Project project) {
        ConfigurationContainer configurations = project.getConfigurations();
        configurations.maybeCreate('agent')
                .setVisible(false)
                .setTransitive(false)
                .setDescription("Configuration for agent plugin.");
        configurations.maybeCreate('server')
                .setVisible(false)
                .setTransitive(false)
                .setDescription("Configuration for server plugin.");
        configurations.maybeCreate('plugin')
                .setVisible(false)
                .setTransitive(false)
                .setDescription('Configuration for plugin artifact.')
        project.plugins.withType(JavaPlugin) {
            Configuration providedConfiguration = configurations.maybeCreate('provided')
                    .setVisible(false)
                    .setDescription('Additional compile classpath for TeamCity libraries that will not be part of the plugin archive.')
            configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME).extendsFrom(providedConfiguration)
        }
    }

    abstract void configureTasks(final Project project, TeamCityPluginExtension extension)
}
