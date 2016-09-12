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
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar

abstract class TeamCityPlugin implements Plugin<Project> {

    private static final Logger LOGGER = Logging.getLogger(TeamCityPlugin.class);

    public static final String PLUGIN_DESCRIPTOR_FILENAME = 'teamcity-plugin.xml'

    public static final String PLUGIN_DESCRIPTOR_DIR = 'descriptor'

    static final String TEAMCITY_EXTENSION_NAME = 'teamcity'

    static final String JETBRAINS_MAVEN_REPOSITORY = 'http://download.jetbrains.com/teamcity-repository'

    static final String CLASSES_PATTERN = "**/*.class";

    static final String NO_BEAN_CLASS_WARNING_MESSAGE = "%s: Plugin definition file %s defines a bean but the implementation class %s was not found in the jar.";

    static final String NO_DEFINITION_WARNING_MESSAGE = "%s: No valid plugin definition files were found in META-INF";

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
            def environments = project.container(TeamCityEnvironment)
            extension = project.extensions.create(TEAMCITY_EXTENSION_NAME, TeamCityPluginExtension, project, environments)
        }
        configureRepositories(project, extension)
        configureConfigurations(project)
        configureTasks(project, extension)
    }

    private configureRepositories(Project project, TeamCityPluginExtension extension) {
        project.plugins.withType(JavaPlugin) {
            project.afterEvaluate {
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

    void configureJarTask(Project project, String pattern) {
        Jar jarTask = (Jar) project.tasks.findByName(JavaPlugin.JAR_TASK_NAME)
        if (jarTask) {
            List<PluginDefinition> pluginDefinitions = []
            jarTask.filesMatching pattern, { fileCopyDetails ->
                pluginDefinitions << new PluginDefinition(fileCopyDetails.file)
            }
            Set<String> classes = []
            jarTask.filesMatching CLASSES_PATTERN, { fileCopyDetails ->
                classes << fileCopyDetails.relativePath.toString()
            }
            jarTask.doLast { task ->
                if (pluginDefinitions.isEmpty()) {
                    LOGGER.warn(String.format(NO_DEFINITION_WARNING_MESSAGE, task.getPath()));
                } else {
                    for (PluginDefinition definition : pluginDefinitions) {
                        for (PluginBean bean : definition.getBeans()) {
                            def fqcn = bean.className.replaceAll('\\.', '/') + '.class'
                            if (!classes.contains(fqcn)) {
                                LOGGER.warn(String.format(NO_BEAN_CLASS_WARNING_MESSAGE, task.getPath(), definition.name, bean.className));
                            }
                        }
                    }
                }
            }
        }
    }

    static class PluginDefinition {

        private File definitionFile

        PluginDefinition(File file) {
            this.definitionFile = file
        }

        String getName() {
            return this.definitionFile.name
        }

        def getBeans() {
            List<PluginBean> pluginBeans = []
            def parser = new XmlParser()
            parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
            parser.setProperty("http://javax.xml.XMLConstants/property/accessExternalDTD", "file,http");
            def beans = parser.parse(definitionFile)
            beans.bean.each { bean ->
                pluginBeans << new PluginBean(id: bean.attribute('id'), className: bean.attribute('class'))
            }
            return pluginBeans
        }
    }

    static class PluginBean {
        String id
        String className
    }
}
