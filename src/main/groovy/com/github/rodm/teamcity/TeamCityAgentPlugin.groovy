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
package com.github.rodm.teamcity

import com.github.rodm.teamcity.internal.FileCollectorAction
import com.github.rodm.teamcity.internal.PluginDescriptorValidationAction
import com.github.rodm.teamcity.internal.PluginExecutableFilesValidationAction
import com.github.rodm.teamcity.tasks.AgentPlugin
import com.github.rodm.teamcity.tasks.GenerateAgentPluginDescriptor
import com.github.rodm.teamcity.tasks.ProcessDescriptor
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Zip

import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_DIR
import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME
import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP
import static com.github.rodm.teamcity.TeamCityPlugin.configureJarTask
import static com.github.rodm.teamcity.TeamCityPlugin.configurePluginArchiveTask
import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME
import static org.gradle.language.base.plugins.LifecycleBasePlugin.ASSEMBLE_TASK_NAME

class TeamCityAgentPlugin implements Plugin<Project> {

    public static final String PLUGIN_DEFINITION_PATTERN = "META-INF/build-agent-plugin*.xml"

    public static final String AGENT_PLUGIN_DESCRIPTOR_DIR = PLUGIN_DESCRIPTOR_DIR + '/agent'

    public static final String PROCESS_AGENT_DESCRIPTOR_TASK_NAME = 'processAgentDescriptor'
    public static final String GENERATE_AGENT_DESCRIPTOR_TASK_NAME = 'generateAgentDescriptor'
    public static final String AGENT_PLUGIN_TASK_NAME = 'agentPlugin'

    void apply(Project project) {
        project.plugins.apply(TeamCityPlugin)

        project.plugins.withType(JavaPlugin) {
            if (project.plugins.hasPlugin(TeamCityServerPlugin)) {
                throw new GradleException("Cannot apply both the teamcity-agent and teamcity-server plugins with the Java plugin")
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        configureDependencies(project, extension)
        configureJarTask(project, extension, PLUGIN_DEFINITION_PATTERN)
        configureTasks(project, extension)
    }

    void configureDependencies(Project project, TeamCityPluginExtension extension) {
        project.plugins.withType(JavaPlugin) {
            project.afterEvaluate {
                project.dependencies {
                    provided "org.jetbrains.teamcity:agent-api:${extension.version}"

                    testImplementation "org.jetbrains.teamcity:tests-support:${extension.version}"
                }
            }
        }
    }

    @SuppressWarnings('GrMethodMayBeStatic')
    void configureTasks(Project project, TeamCityPluginExtension extension) {
        def descriptorFile = project.layout.buildDirectory.file(AGENT_PLUGIN_DESCRIPTOR_DIR + '/' + PLUGIN_DESCRIPTOR_FILENAME)

        def processDescriptor = project.tasks.register(PROCESS_AGENT_DESCRIPTOR_TASK_NAME, ProcessDescriptor) {
            descriptor.set(extension.agent.descriptorFile)
            tokens.set(project.providers.provider({ extension.agent.tokens }))
            destination.set(descriptorFile)
        }

        def generateDescriptor = project.tasks.register(GENERATE_AGENT_DESCRIPTOR_TASK_NAME, GenerateAgentPluginDescriptor) {
            version.set(project.providers.provider({ extension.version }))
            descriptor.set(project.providers.provider({ extension.agent.descriptor }))
            destination.set(descriptorFile)
        }

        def packagePlugin = project.tasks.register(AGENT_PLUGIN_TASK_NAME, AgentPlugin) {
            group = TEAMCITY_GROUP
            onlyIf { extension.agent.descriptor != null || extension.agent.descriptorFile.isPresent() }
            descriptor.set(descriptorFile)
            lib.from(project.configurations.agent)
            project.plugins.withType(JavaPlugin) {
                lib.from(project.tasks.named(JAR_TASK_NAME))
                lib.from(project.configurations.runtimeClasspath)
            }
            with(extension.agent.files)
            dependsOn processDescriptor, generateDescriptor
        }

        project.tasks.withType(AgentPlugin).configureEach {
            doLast(new PluginDescriptorValidationAction('teamcity-agent-plugin-descriptor.xsd'))
            Set<FileCopyDetails> files = []
            filesMatching('**/*', new FileCollectorAction(files))
            doLast(new PluginExecutableFilesValidationAction(files))
        }

        project.plugins.withType(TeamCityServerPlugin) {
            packagePlugin.configure {archiveAppendix.convention('agent') }
        }

        project.tasks.named(ASSEMBLE_TASK_NAME) {
            dependsOn packagePlugin
        }

        project.artifacts.add('plugin', packagePlugin)

        project.afterEvaluate {
            project.tasks.named(AGENT_PLUGIN_TASK_NAME) { Zip task ->
                configurePluginArchiveTask(task, extension.agent.archiveName)
            }
        }
    }
}
