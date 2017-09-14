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

import com.github.rodm.teamcity.tasks.GenerateAgentPluginDescriptor
import com.github.rodm.teamcity.tasks.ProcessDescriptor
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Zip

import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_DIR
import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME
import static com.github.rodm.teamcity.TeamCityPlugin.PluginDescriptorValidationAction
import static com.github.rodm.teamcity.TeamCityPlugin.configureJarTask
import static com.github.rodm.teamcity.TeamCityPlugin.configurePluginArchiveTask

class TeamCityAgentPlugin implements Plugin<Project> {

    public static final String PLUGIN_DEFINITION_PATTERN = "META-INF/build-agent-plugin*.xml"

    public static final String AGENT_PLUGIN_DESCRIPTOR_DIR = PLUGIN_DESCRIPTOR_DIR + '/agent'

    void apply(Project project) {
        project.plugins.apply(TeamCityPlugin)

        if (project.plugins.hasPlugin(JavaPlugin) && project.plugins.hasPlugin(TeamCityServerPlugin)) {
            throw new GradleException("Cannot apply both the teamcity-agent and teamcity-server plugins with the Java plugin")
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        configureDependencies(project, extension)
        configureTasks(project, extension)
    }

    void configureDependencies(Project project, TeamCityPluginExtension extension) {
        project.plugins.withType(JavaPlugin) {
            project.afterEvaluate {
                project.dependencies {
                    provided "org.jetbrains.teamcity:agent-api:${extension.version}"
                }
            }
        }
    }

    void configureTasks(Project project, TeamCityPluginExtension extension) {
        configureJarTask(project, PLUGIN_DEFINITION_PATTERN)

        def packagePlugin = project.tasks.create('agentPlugin', Zip)
        packagePlugin.description = 'Package TeamCity Agent plugin'
        packagePlugin.group = 'TeamCity'
        packagePlugin.with {
            baseName = "${project.rootProject.name}-agent"
            into("lib") {
                project.plugins.withType(JavaPlugin) {
                    def jar = project.tasks[JavaPlugin.JAR_TASK_NAME]
                    from(jar)
                    from(project.configurations.runtime - project.configurations.provided)
                }
                from(project.configurations.agent)
            }
            into('') {
                from {
                    new File(project.getBuildDir(), AGENT_PLUGIN_DESCRIPTOR_DIR + "/" + PLUGIN_DESCRIPTOR_FILENAME)
                }
                rename {
                    PLUGIN_DESCRIPTOR_FILENAME
                }
            }
        }
        def descriptorFile = new File(project.getBuildDir(), AGENT_PLUGIN_DESCRIPTOR_DIR + '/' + PLUGIN_DESCRIPTOR_FILENAME)
        packagePlugin.doLast(new PluginDescriptorValidationAction('teamcity-agent-plugin-descriptor.xsd', descriptorFile))
        packagePlugin.with(extension.agent.files)
        packagePlugin.onlyIf { extension.agent.descriptor != null }

        def assemble = project.tasks['assemble']
        assemble.dependsOn packagePlugin

        def processDescriptor = project.tasks.create('processAgentDescriptor', ProcessDescriptor) {
            conventionMapping.descriptor = { extension.agent.descriptor instanceof File ? extension.agent.descriptor : null }
            conventionMapping.tokens = { extension.agent.tokens }
        }
        processDescriptor.destinationDir = new File(project.buildDir, AGENT_PLUGIN_DESCRIPTOR_DIR)
        processDescriptor.onlyIf { extension.agent.descriptor != null && extension.agent.descriptor instanceof File}
        packagePlugin.dependsOn processDescriptor

        def generateDescriptor = project.tasks.create('generateAgentDescriptor', GenerateAgentPluginDescriptor) {
            conventionMapping.version = { extension.version }
            conventionMapping.descriptor = { extension.agent.descriptor instanceof AgentPluginDescriptor ? extension.agent.descriptor : null }
        }
        generateDescriptor.onlyIf { extension.agent.descriptor != null && extension.agent.descriptor instanceof AgentPluginDescriptor }
        packagePlugin.dependsOn generateDescriptor

        project.artifacts.add('plugin', packagePlugin)

        project.afterEvaluate {
            Zip agentPlugin = (Zip) project.tasks.getByPath('agentPlugin')
            configurePluginArchiveTask(agentPlugin, extension.agent.archiveName)
        }
    }
}
