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

import com.github.rodm.teamcity.internal.ClassCollectorAction
import com.github.rodm.teamcity.internal.PluginDefinition
import com.github.rodm.teamcity.internal.PluginDefinitionCollectorAction
import com.github.rodm.teamcity.internal.PluginDefinitionValidationAction
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.util.GradleVersion
import org.xml.sax.SAXNotRecognizedException

import javax.xml.XMLConstants

class TeamCityPlugin implements Plugin<Project> {

    public static final String PLUGIN_DESCRIPTOR_FILENAME = 'teamcity-plugin.xml'

    public static final String PLUGIN_DESCRIPTOR_DIR = 'descriptor'

    static final String TEAMCITY_EXTENSION_NAME = 'teamcity'

    static final String TEAMCITY_GROUP = 'TeamCity'

    static final String JETBRAINS_MAVEN_REPOSITORY = 'https://download.jetbrains.com/teamcity-repository'

    static final String CLASSES_PATTERN = "**/*.class"

    private final String MINIMUM_SUPPORTED_VERSION = '6.0'

    void apply(Project project) {
        project.plugins.apply(BasePlugin)

        if (GradleVersion.current() < GradleVersion.version(MINIMUM_SUPPORTED_VERSION)) {
            throw new GradleException("Gradle TeamCity plugin requires Gradle version ${MINIMUM_SUPPORTED_VERSION} or later")
        }

        if (project.plugins.hasPlugin(JavaPlugin) &&
                (project.plugins.hasPlugin(TeamCityAgentPlugin) || project.plugins.hasPlugin(TeamCityServerPlugin))) {
            throw new GradleException("Cannot apply both the teamcity-agent and teamcity-server plugins with the Java plugin")
        }

        TeamCityPluginExtension extension
        if (project.extensions.findByType(TeamCityPluginExtension)) {
            extension = project.extensions.getByType(TeamCityPluginExtension)
        } else {
            extension = project.extensions.create(TEAMCITY_EXTENSION_NAME, TeamCityPluginExtension, project)
            extension.init()
        }
        configureRepositories(project, extension)
        configureConfigurations(project)
        validateVersion(project, extension)
    }

    private static configureRepositories(Project project, TeamCityPluginExtension extension) {
        project.afterEvaluate new ConfigureRepositories(extension)
    }

    static void configureConfigurations(final Project project) {
        ConfigurationContainer configurations = project.getConfigurations()
        configurations.maybeCreate('agent')
                .setVisible(false)
                .setDescription("Configuration for agent plugin.")
        configurations.maybeCreate('server')
                .setVisible(false)
                .setDescription("Configuration for server plugin.")
        configurations.maybeCreate('plugin')
                .setVisible(false)
                .setTransitive(false)
                .setDescription('Configuration for plugin artifact.')
        project.plugins.withType(JavaPlugin) {
            Configuration providedConfiguration = configurations.maybeCreate('provided')
                    .setVisible(false)
                    .setDescription('Additional compile classpath for TeamCity libraries that will not be part of the plugin archive.')
            configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).extendsFrom(providedConfiguration)
            configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(providedConfiguration)
        }
    }

    static void validateVersion(Project project, TeamCityPluginExtension extension) {
        project.afterEvaluate {
            TeamCityVersion.version(extension.version, extension.allowSnapshotVersions.get())
        }
    }

    static void configureJarTask(Project project, TeamCityPluginExtension extension, String pattern) {
        project.afterEvaluate {
            Jar jarTask = (Jar) project.tasks.findByName(JavaPlugin.JAR_TASK_NAME)
            if (jarTask) {
                ValidationMode mode = extension.validateBeanDefinition.get()
                List<PluginDefinition> pluginDefinitions = []
                Set<String> classes = []
                jarTask.filesMatching(pattern, new PluginDefinitionCollectorAction(pluginDefinitions))
                jarTask.filesMatching(CLASSES_PATTERN, new ClassCollectorAction(classes))
                jarTask.doLast new PluginDefinitionValidationAction(mode, pluginDefinitions, classes)
            }
        }
    }

    static void configurePluginArchiveTask(Zip task, String archiveName) {
        if (archiveName) {
            task.archiveFileName = archiveName.endsWith('.zip') ? archiveName : archiveName + '.zip'
        }
    }

    static XmlParser createXmlParser() {
        return createXmlParser(false)
    }

    static XmlParser createXmlParser(boolean offline) {
        def parser = new XmlParser(false, true, true)
        if (offline) {
            parser.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)
        }
        setParserProperty(parser, XMLConstants.ACCESS_EXTERNAL_DTD, "file,http")
        setParserProperty(parser, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,http")
        return parser
    }

    private static void setParserProperty(XmlParser parser, String uri, Object value) {
        try {
            parser.setProperty(uri, value)
        }
        catch (SAXNotRecognizedException ignore) { }
    }

    static class ConfigureRepositories implements Action<Project> {

        private TeamCityPluginExtension extension

        ConfigureRepositories(TeamCityPluginExtension extension) {
            this.extension = extension
        }

        @Override
        void execute(Project project) {
            project.plugins.withType(JavaPlugin) {
                if (extension.defaultRepositories) {
                    project.repositories {
                        mavenCentral()
                        maven {
                            url = JETBRAINS_MAVEN_REPOSITORY
                        }
                    }
                }
            }
        }
    }
}
