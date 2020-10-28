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

import com.github.rodm.teamcity.tasks.GenerateServerPluginDescriptor
import com.github.rodm.teamcity.tasks.ProcessDescriptor
import com.github.rodm.teamcity.tasks.PublishTask
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Zip

import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_DIR
import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME
import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP
import static com.github.rodm.teamcity.TeamCityPlugin.configureJarTask
import static com.github.rodm.teamcity.TeamCityPlugin.configurePluginArchiveTask
import static com.github.rodm.teamcity.TeamCityPlugin.PluginDescriptorValidationAction
import static com.github.rodm.teamcity.TeamCityPlugin.createXmlParser
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2020_1
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_9_0

class TeamCityServerPlugin implements Plugin<Project> {

    public static final String PLUGIN_DEFINITION_PATTERN = "META-INF/build-server-plugin*.xml"

    public static final String SERVER_PLUGIN_DESCRIPTOR_DIR = PLUGIN_DESCRIPTOR_DIR + '/server'

    void apply(Project project) {
        project.plugins.apply(TeamCityPlugin)

        project.plugins.withType(JavaPlugin) {
            if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
                throw new GradleException("Cannot apply both the teamcity-agent and teamcity-server plugins with the Java plugin")
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        configureDependencies(project, extension)
        configureServerPluginTasks(project, extension)
        configurePublishPluginTask(project, extension)
        configureEnvironmentTasks(project, extension)
    }

    void configureDependencies(Project project, TeamCityPluginExtension extension) {
        project.plugins.withType(JavaPlugin) {
            project.afterEvaluate {
                project.dependencies {
                    provided "org.jetbrains.teamcity:server-api:${extension.version}"
                    if (TeamCityVersion.version(extension.version) >= VERSION_9_0) {
                        provided "org.jetbrains.teamcity:server-web-api:${extension.version}"
                    }

                    testImplementation "org.jetbrains.teamcity:tests-support:${extension.version}"
                }
            }
        }
    }

    void configureServerPluginTasks(Project project, TeamCityPluginExtension extension) {
        configureJarTask(project, PLUGIN_DEFINITION_PATTERN)

        def descriptorFile = project.layout.buildDirectory.file(SERVER_PLUGIN_DESCRIPTOR_DIR + '/' + PLUGIN_DESCRIPTOR_FILENAME)

        def processDescriptor = project.tasks.register('processServerDescriptor', ProcessDescriptor) {
            descriptor.set(extension.server.descriptorFile)
            tokens.set(project.providers.provider({ extension.server.tokens }))
            destination.set(descriptorFile)
        }

        def generateDescriptor = project.tasks.register('generateServerDescriptor', GenerateServerPluginDescriptor) {
            version.set(project.providers.provider({ extension.version }))
            descriptor.set(project.providers.provider({ extension.server.descriptor }))
            destination.set(descriptorFile)
        }

        def packagePlugin = project.tasks.register('serverPlugin', Zip) {
            description = 'Package TeamCity plugin'
            group = TEAMCITY_GROUP
            into('server') {
                project.plugins.withType(JavaPlugin) {
                    from(project.tasks.named(JavaPlugin.JAR_TASK_NAME))
                    from(project.configurations.runtimeClasspath)
                }
                from(project.configurations.server)
            }
            into('agent') {
                if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
                    from(project.tasks.named('agentPlugin'))
                } else {
                    from(project.configurations.agent)
                }
            }
            into('') {
                from {
                    descriptorFile
                }
                rename {
                    PLUGIN_DESCRIPTOR_FILENAME
                }
            }
            with(extension.server.files)
            onlyIf { extension.server.descriptor != null || extension.server.descriptorFile.isPresent() }
            dependsOn processDescriptor, generateDescriptor
        }

        def assemble = project.tasks['assemble']
        assemble.dependsOn packagePlugin

        project.artifacts.add('plugin', packagePlugin)

        project.afterEvaluate {
            project.tasks.named('serverPlugin').configure { Zip task ->
                configurePluginArchiveTask(task, extension.server.archiveName)
                doLast(new PluginDescriptorValidationAction(getSchemaPath(extension.version), descriptorFile))
                doLast(new PluginDescriptorContentsValidationAction(descriptorFile))
            }
        }
    }

    private static String getSchemaPath(String version) {
        if (TeamCityVersion.version(version) >= VERSION_2020_1) {
            '2020.1/teamcity-server-plugin-descriptor.xsd'
        } else if (TeamCityVersion.version(version) >= VERSION_2018_2) {
            '2018.2/teamcity-server-plugin-descriptor.xsd'
        } else {
            'teamcity-server-plugin-descriptor.xsd'
        }
    }

    private static void configureEnvironmentTasks(Project project, TeamCityPluginExtension extension) {
        project.afterEvaluate {
            if (!project.plugins.hasPlugin(TeamCityEnvironmentsPlugin)) {
                new TeamCityEnvironmentsPlugin.ConfigureEnvironmentTasksAction(extension)
            }
        }
    }

    private static void configurePublishPluginTask(Project project, TeamCityPluginExtension extension) {
        project.afterEvaluate {
            if (extension.server.publish) {
                Zip buildPluginTask = project.tasks.findByName('serverPlugin') as Zip
                project.tasks.create("publishPlugin", PublishTask) {
                    group = TEAMCITY_GROUP
                    description = "Publish plugin distribution on plugins.jetbrains.com."
                    channels.set(extension.server.publish.channels)
                    token.set(extension.server.publish.token)
                    notes.set(extension.server.publish.notes)
                    distributionFile.set(buildPluginTask.archiveFile)
                    dependsOn(buildPluginTask)
                }
            }
        }
    }

    static class PluginDescriptorContentsValidationAction implements Action<Task> {

        private static final Logger LOGGER = Logging.getLogger(TeamCityPlugin.class)

        static final String EMPTY_VALUE_WARNING_MESSAGE = "%s: Plugin descriptor value for %s must not be empty."

        private Provider<RegularFile> descriptorFile

        PluginDescriptorContentsValidationAction(Provider<RegularFile> descriptorFile) {
            this.descriptorFile = descriptorFile
        }

        @Override
        void execute(Task task) {
            def parser = createXmlParser()
            def descriptor = parser.parse(descriptorFile.get().asFile)

            if (descriptor.info.name.text().trim().isEmpty()) {
                LOGGER.warn(String.format(EMPTY_VALUE_WARNING_MESSAGE, task.getPath(), 'name'))
            }
            if (descriptor.info.'display-name'.text().trim().isEmpty()) {
                LOGGER.warn(String.format(EMPTY_VALUE_WARNING_MESSAGE, task.getPath(), 'display name'))
            }
            if (descriptor.info.version.text().trim().isEmpty()) {
                LOGGER.warn(String.format(EMPTY_VALUE_WARNING_MESSAGE, task.getPath(), 'version'))
            }
            if (descriptor.info.vendor.name.text().trim().isEmpty()) {
                LOGGER.warn(String.format(EMPTY_VALUE_WARNING_MESSAGE, task.getPath(), 'vendor name'))
            }
            if (descriptor.info.description.text().trim().isEmpty()) {
                LOGGER.warn(String.format(EMPTY_VALUE_WARNING_MESSAGE, task.getPath(), 'description'))
            }
            if (descriptor.info.vendor.url.text().trim().isEmpty()) {
                LOGGER.warn(String.format(EMPTY_VALUE_WARNING_MESSAGE, task.getPath(), 'vendor url'))
            }
        }
    }
}
