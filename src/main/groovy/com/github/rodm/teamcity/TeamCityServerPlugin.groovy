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

import com.github.rodm.teamcity.tasks.GenerateServerPluginDescriptor
import com.github.rodm.teamcity.tasks.ProcessDescriptor
import com.github.rodm.teamcity.tasks.PublishTask
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.annotations.NotNull
import org.xml.sax.SAXNotRecognizedException

import javax.xml.XMLConstants

import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_DIR
import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME
import static com.github.rodm.teamcity.TeamCityPlugin.configureJarTask
import static com.github.rodm.teamcity.TeamCityPlugin.configurePluginArchiveTask
import static com.github.rodm.teamcity.TeamCityPlugin.PluginDescriptorValidationAction

class TeamCityServerPlugin implements Plugin<Project> {

    public static final String PLUGIN_DEFINITION_PATTERN = "META-INF/build-server-plugin*.xml"

    public static final String SERVER_PLUGIN_DESCRIPTOR_DIR = PLUGIN_DESCRIPTOR_DIR + '/server'

    void apply(Project project) {
        project.plugins.apply(TeamCityPlugin)

        if (project.plugins.hasPlugin(JavaPlugin) && project.plugins.hasPlugin(TeamCityAgentPlugin)) {
            throw new GradleException("Cannot apply both the teamcity-agent and teamcity-server plugins with the Java plugin")
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

                    testCompile "org.jetbrains.teamcity:tests-support:${extension.version}"
                }
            }
        }
    }

    void configureServerPluginTasks(Project project, TeamCityPluginExtension extension) {
        configureJarTask(project, PLUGIN_DEFINITION_PATTERN)

        def packagePlugin = project.tasks.create('serverPlugin', Zip)
        packagePlugin.description = 'Package TeamCity plugin'
        packagePlugin.group = 'TeamCity'
        packagePlugin.with {
            baseName = "${project.rootProject.name}"
            into('server') {
                project.plugins.withType(JavaPlugin) {
                    def jar = project.tasks[JavaPlugin.JAR_TASK_NAME]
                    from(jar)
                    from(project.configurations.runtime)
                }
                from(project.configurations.server)
            }
            into('agent') {
                if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
                    def agentPlugin = project.tasks['agentPlugin']
                    from(agentPlugin)
                } else {
                    from(project.configurations.agent)
                }
            }
            into('') {
                from {
                    new File(project.getBuildDir(), SERVER_PLUGIN_DESCRIPTOR_DIR + "/" + PLUGIN_DESCRIPTOR_FILENAME)
                }
                rename {
                    PLUGIN_DESCRIPTOR_FILENAME
                }
            }
        }
        def descriptorFile = new File(project.getBuildDir(), SERVER_PLUGIN_DESCRIPTOR_DIR + '/' + PLUGIN_DESCRIPTOR_FILENAME)
        packagePlugin.doLast(new PluginDescriptorValidationAction('teamcity-server-plugin-descriptor.xsd', descriptorFile))
        packagePlugin.doLast(new PluginDescriptorContentsValidationAction(descriptorFile))
        packagePlugin.with(extension.server.files)
        packagePlugin.onlyIf { extension.server.descriptor != null }

        def assemble = project.tasks['assemble']
        assemble.dependsOn packagePlugin

        def processDescriptor = project.tasks.create('processServerDescriptor', ProcessDescriptor) {
            conventionMapping.descriptor = { extension.server.descriptor instanceof File ? extension.server.descriptor : null }
            conventionMapping.tokens = { extension.server.tokens }
        }
        processDescriptor.destinationDir = new File(project.buildDir, SERVER_PLUGIN_DESCRIPTOR_DIR)
        processDescriptor.onlyIf { extension.server.descriptor != null && extension.server.descriptor instanceof File }
        packagePlugin.dependsOn processDescriptor

        def generateDescriptor = project.tasks.create('generateServerDescriptor', GenerateServerPluginDescriptor) {
            conventionMapping.version = { extension.version }
            conventionMapping.descriptor = { extension.server.descriptor instanceof ServerPluginDescriptor ? extension.server.descriptor : null }
            conventionMapping.destination = { descriptorFile }
        }
        generateDescriptor.onlyIf { extension.server.descriptor != null && extension.server.descriptor instanceof ServerPluginDescriptor }
        packagePlugin.dependsOn generateDescriptor

        project.artifacts.add('plugin', packagePlugin)

        project.afterEvaluate {
            Zip serverPlugin = (Zip) project.tasks.getByPath('serverPlugin')
            configurePluginArchiveTask(serverPlugin, extension.server.archiveName)
        }
    }

    private static void configureEnvironmentTasks(Project project, TeamCityPluginExtension extension) {
        project.afterEvaluate {
            if (!project.plugins.hasPlugin(TeamCityEnvironmentsPlugin)) {
                new TeamCityEnvironmentsPlugin.ConfigureEnvironmentTasksAction(extension)
            }
        }
    }

    private static void configurePublishPluginTask(@NotNull Project project, TeamCityPluginExtension extension) {
        project.afterEvaluate {
            if (extension.server.publish) {
                def buildPluginTask = project.tasks.findByName('serverPlugin') as Zip
                project.tasks.create("publishPlugin", PublishTask) {
                    group = TeamCityPlugin.GROUP_NAME
                    description = "Publish plugin distribution on plugins.jetbrains.com."
                    conventionMapping.username = { extension.server.publish.username }
                    conventionMapping.password = { extension.server.publish.password }
                    conventionMapping.distributionFile = {
                        def distributionFile = buildPluginTask?.archivePath
                        return distributionFile?.exists() ? distributionFile : null
                    }
                    dependsOn(buildPluginTask)
                }
            }
        }
    }

    static class PluginDescriptorContentsValidationAction implements Action<Task> {

        private static final Logger LOGGER = Logging.getLogger(TeamCityPlugin.class)

        static final String EMPTY_VALUE_WARNING_MESSAGE = "%s: Plugin descriptor value for %s must not be empty."

        private File descriptorFile

        PluginDescriptorContentsValidationAction(File descriptorFile) {
            this.descriptorFile = descriptorFile
        }

        @Override
        void execute(Task task) {
            def parser = new XmlParser()
            parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
            setParserProperty(parser, XMLConstants.ACCESS_EXTERNAL_DTD, "file,http")
            setParserProperty(parser, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,http")
            def descriptor = parser.parse(descriptorFile)

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

        private static void setParserProperty(XmlParser parser, String uri, Object value) {
            try {
                parser.setProperty(uri, value)
            }
            catch (SAXNotRecognizedException ignore) { }
        }
    }
}
