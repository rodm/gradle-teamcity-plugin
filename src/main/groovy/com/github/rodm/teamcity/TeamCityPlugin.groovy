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

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.util.GradleVersion
import org.xml.sax.SAXNotRecognizedException

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

class TeamCityPlugin implements Plugin<Project> {

    private static final Logger LOGGER = Logging.getLogger(TeamCityPlugin.class)

    public static final String PLUGIN_DESCRIPTOR_FILENAME = 'teamcity-plugin.xml'

    public static final String PLUGIN_DESCRIPTOR_DIR = 'descriptor'

    static final String TEAMCITY_EXTENSION_NAME = 'teamcity'

    static final String TEAMCITY_GROUP = 'TeamCity'

    static final String JETBRAINS_MAVEN_REPOSITORY = 'https://download.jetbrains.com/teamcity-repository'

    static final String CLASSES_PATTERN = "**/*.class"

    static final String NO_BEAN_CLASS_WARNING_MESSAGE = "%s: Plugin definition file %s defines a bean but the implementation class %s was not found in the jar."

    static final String NO_BEAN_CLASSES_WARNING_MESSAGE = "%s: Plugin definition file %s contains no beans."

    static final String NO_BEAN_CLASSES_NON_PARSED_WARNING_MESSAGE = "%s: Failed to parse plugin definition file %s: %s"

    static final String NO_DEFINITION_WARNING_MESSAGE = "%s: No valid plugin definition files were found in META-INF"

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

    static void configureJarTask(Project project, String pattern) {
        project.afterEvaluate {
            Jar jarTask = (Jar) project.tasks.findByName(JavaPlugin.JAR_TASK_NAME)
            if (jarTask) {
                List<PluginDefinition> pluginDefinitions = []
                Set<String> classes = []
                jarTask.filesMatching(pattern, new PluginDefinitionCollectorAction(pluginDefinitions))
                jarTask.filesMatching(CLASSES_PATTERN, new ClassCollectorAction(classes))
                jarTask.doLast new PluginDefinitionValidationAction(pluginDefinitions, classes)
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

    static class PluginDefinitionCollectorAction implements Action<FileCopyDetails> {

        List<PluginDefinition> pluginDefinitions

        PluginDefinitionCollectorAction(List<PluginDefinition> pluginDefinitions) {
            this.pluginDefinitions = pluginDefinitions
        }
        @Override
        void execute(FileCopyDetails fileCopyDetails) {
            pluginDefinitions << new PluginDefinition(fileCopyDetails.file)
        }
    }

    static class ClassCollectorAction implements Action<FileCopyDetails> {

        private Set<String> classes

        ClassCollectorAction(Set<String> classes) {
            this.classes = classes
        }

        @Override
        void execute(FileCopyDetails fileCopyDetails) {
            classes << fileCopyDetails.relativePath.toString()
        }
    }

    static class PluginDefinitionValidationAction implements Action<Task> {

        private final List<PluginDefinition> definitions
        private final Set<String> classes

        PluginDefinitionValidationAction(List<PluginDefinition> definitions, Set<String> classes) {
            this.definitions = definitions
            this.classes = classes
        }

        @Override
        void execute(Task task) {
            if (definitions.isEmpty()) {
                LOGGER.warn(String.format(NO_DEFINITION_WARNING_MESSAGE, task.getPath()))
            } else {
                for (PluginDefinition definition : definitions) {
                    validateDefinition(definition, task)
                }
            }
        }

        private void validateDefinition(PluginDefinition definition, Task task) {
            boolean offline = task.project.gradle.startParameter.isOffline()
            List<PluginBean> beans
            try {
                beans = definition.getBeans(offline)
            }
            catch (IOException e) {
                LOGGER.warn(String.format(NO_BEAN_CLASSES_NON_PARSED_WARNING_MESSAGE, task.getPath(), definition.name, e.message), e)
                return
            }
            if (beans.isEmpty()) {
                LOGGER.warn(String.format(NO_BEAN_CLASSES_WARNING_MESSAGE, task.getPath(), definition.name))
            } else {
                for (PluginBean bean : beans) {
                    def fqcn = bean.className.replaceAll('\\.', '/') + '.class'
                    if (!classes.contains(fqcn)) {
                        LOGGER.warn(String.format(NO_BEAN_CLASS_WARNING_MESSAGE, task.getPath(), definition.name, bean.className))
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

        def getBeans(boolean offline) {
            List<PluginBean> pluginBeans = []
            def parser = createXmlParser(offline)
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

    static class PluginDescriptorValidationAction implements Action<Task> {

        private String schema
        private Provider<RegularFile> descriptor

        PluginDescriptorValidationAction(String schema, Provider<RegularFile> descriptor) {
            this.schema = schema
            this.descriptor = descriptor
        }

        @Override
        void execute(Task task) {
            Project project = task.getProject()
            def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            URL url = this.getClass().getResource('/schema/' + schema)
            def schema = factory.newSchema(url)
            def validator = schema.newValidator()
            def errorHandler = new PluginDescriptorErrorHandler(project, task.getPath())
            validator.setErrorHandler(errorHandler)
            validator.validate(new StreamSource(new FileReader(descriptor.get().asFile)))
        }
    }
}
