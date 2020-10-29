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

import com.github.rodm.teamcity.tasks.GenerateAgentPluginDescriptor
import com.github.rodm.teamcity.tasks.ProcessDescriptor
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Zip

import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_DIR
import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME
import static com.github.rodm.teamcity.TeamCityPlugin.PluginDescriptorValidationAction
import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP
import static com.github.rodm.teamcity.TeamCityPlugin.configureJarTask
import static com.github.rodm.teamcity.TeamCityPlugin.configurePluginArchiveTask
import static com.github.rodm.teamcity.TeamCityPlugin.createXmlParser

class TeamCityAgentPlugin implements Plugin<Project> {

    private static final Logger LOGGER = Logging.getLogger(TeamCityAgentPlugin)

    public static final String PLUGIN_DEFINITION_PATTERN = "META-INF/build-agent-plugin*.xml"

    public static final String AGENT_PLUGIN_DESCRIPTOR_DIR = PLUGIN_DESCRIPTOR_DIR + '/agent'

    static final String MISSING_EXECUTABLE_FILE_WARNING = "%s: Executable file %s is missing."

    void apply(Project project) {
        project.plugins.apply(TeamCityPlugin)

        project.plugins.withType(JavaPlugin) {
            if (project.plugins.hasPlugin(TeamCityServerPlugin)) {
                throw new GradleException("Cannot apply both the teamcity-agent and teamcity-server plugins with the Java plugin")
            }
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

                    testImplementation "org.jetbrains.teamcity:tests-support:${extension.version}"
                }
            }
        }
    }

    void configureTasks(Project project, TeamCityPluginExtension extension) {
        configureJarTask(project, PLUGIN_DEFINITION_PATTERN)

        def layout = project.layout
        def descriptorFile = layout.buildDirectory.file(AGENT_PLUGIN_DESCRIPTOR_DIR + '/' + PLUGIN_DESCRIPTOR_FILENAME)

        def processDescriptor = project.tasks.register('processAgentDescriptor', ProcessDescriptor) {
            descriptor.set(extension.agent.descriptorFile)
            tokens.set(project.providers.provider({ extension.agent.tokens }))
            destination.set(layout.buildDirectory.file(AGENT_PLUGIN_DESCRIPTOR_DIR + '/' + PLUGIN_DESCRIPTOR_FILENAME))
        }

        def generateDescriptor = project.tasks.register('generateAgentDescriptor', GenerateAgentPluginDescriptor) {
            version.set(project.providers.provider({ extension.version }))
            descriptor.set(project.providers.provider({ extension.agent.descriptor }))
            destination.set(descriptorFile)
        }

        def packagePlugin = project.tasks.register('agentPlugin', Zip) {
            description = 'Package TeamCity Agent plugin'
            group = TEAMCITY_GROUP
            into("lib") {
                project.plugins.withType(JavaPlugin) {
                    def jar = project.tasks[JavaPlugin.JAR_TASK_NAME]
                    from(jar)
                    from(project.configurations.runtimeClasspath)
                }
                from(project.configurations.agent)
            }
            into('') {
                from {
                    descriptorFile
                }
                rename {
                    PLUGIN_DESCRIPTOR_FILENAME
                }
            }
            doLast(new PluginDescriptorValidationAction('teamcity-agent-plugin-descriptor.xsd', descriptorFile))
            with(extension.agent.files)
            onlyIf { extension.agent.descriptor != null || extension.agent.descriptorFile.isPresent() }
            Set<FileCopyDetails> files = []
            filesMatching('**/*', new FileCollectorAction(files))
            doLast(new PluginExecutableFilesValidationAction(descriptorFile, files))
            dependsOn processDescriptor, generateDescriptor
        }

        project.plugins.withType(TeamCityServerPlugin) {
            packagePlugin.configure {archiveAppendix.convention('agent') }
        }

        def assemble = project.tasks['assemble']
        assemble.dependsOn packagePlugin

        project.artifacts.add('plugin', packagePlugin)

        project.afterEvaluate {
            project.tasks.named('agentPlugin') { Zip task ->
                configurePluginArchiveTask(task, extension.agent.archiveName)
            }
        }
    }

    static class FileCollectorAction implements Action<FileCopyDetails> {

        private Set<FileCopyDetails> files

        FileCollectorAction(Set<FileCopyDetails> files) {
            this.files = files
        }

        @Override
        void execute(FileCopyDetails fileCopyDetails) {
            files << fileCopyDetails
        }
    }

    static class PluginExecutableFilesValidationAction implements Action<Task> {

        private Provider<RegularFile> descriptor
        private Set<FileCopyDetails> files

        PluginExecutableFilesValidationAction(Provider<RegularFile> descriptor, Set<FileCopyDetails> files) {
            this.descriptor = descriptor
            this.files = files
        }

        @Override
        void execute(Task task) {
            def paths = files.flatten { fileCopyDetails -> fileCopyDetails.path }
            List<String> executableFiles = getExecutableFiles()
            for (String executableFile : executableFiles) {
                if (!paths.contains(executableFile)) {
                    LOGGER.warn(String.format(MISSING_EXECUTABLE_FILE_WARNING, task.getPath(), executableFile))
                }
            }
        }

        def getExecutableFiles() {
            def parser = createXmlParser()
            def descriptor = parser.parse(descriptor.get().asFile)
            return descriptor.breadthFirst().findAll { node -> node.name() == 'include' }.flatten { node -> node['@name'] }
        }
    }
}
