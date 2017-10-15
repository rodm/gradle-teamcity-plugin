/*
 * Copyright 2016 Rod MacKenzie
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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.MatcherAssert.assertThat
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.Mockito.verify

class ValidateDefinitionActionTest {

    public static final String BEAN_DEFINITION_FILE = """<?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN//EN" "http://www.springframework.org/dtd/spring-beans.dtd">
            <beans default-autowire="constructor">
                <bean id="examplePlugin" class="example.Plugin"/>
            </beans>
        """

    public static final String EMPTY_BEAN_DEFINITION_FILE = """<?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN//EN" "http://www.springframework.org/dtd/spring-beans.dtd">
            <beans default-autowire="constructor">
            </beans>
        """

    private static final String NO_DEFINITION_WARNING = TeamCityPlugin.NO_DEFINITION_WARNING_MESSAGE.substring(4)
    private static final String NO_BEAN_CLASS_WARNING = TeamCityPlugin.NO_BEAN_CLASS_WARNING_MESSAGE.substring(4)
    private static final String NO_BEAN_CLASSES_WARNING = TeamCityPlugin.NO_BEAN_CLASSES_WARNING_MESSAGE.substring(4)

    private final ResettableOutputEventListener outputEventListener = new ResettableOutputEventListener()

    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder()

    @Rule
    public final ConfigureLogging logging = new ConfigureLogging(outputEventListener)

    private Project project
    private Task stubTask
    private List<TeamCityPlugin.PluginDefinition> definitions
    private Set<String> classes

    @Before
    void setup() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.root).build()
        stubTask = mock(Task)
        definitions = []
        classes = new HashSet<String>()
        when(stubTask.getProject()).thenReturn(project)
    }

    @Test
    void logWarningMessageForMissingPluginDefinitionFiles() {
        Action<Task> pluginValidationAction = new TeamCityPlugin.PluginDefinitionValidationAction(definitions, classes)

        pluginValidationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), containsString(NO_DEFINITION_WARNING))
    }

    @Test
    void noWarningMessageWithPluginDefinition() {
        File definitionFile = project.file('build-server-plugin.xml')
        definitionFile << BEAN_DEFINITION_FILE
        definitions.add(new TeamCityPlugin.PluginDefinition(definitionFile))
        Action<Task> pluginValidationAction = new TeamCityPlugin.PluginDefinitionValidationAction(definitions, classes)
        outputEventListener.reset()

        pluginValidationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), not(containsString(NO_DEFINITION_WARNING)))
    }

    @Test
    void logWarningMessageForEmptyDefinitionFile() {
        File definitionFile = project.file('build-server-plugin.xml')
        definitionFile << EMPTY_BEAN_DEFINITION_FILE
        definitions.add(new TeamCityPlugin.PluginDefinition(definitionFile))
        Action<Task> pluginValidationAction = new TeamCityPlugin.PluginDefinitionValidationAction(definitions, classes)
        outputEventListener.reset()

        pluginValidationAction.execute(stubTask)

        String expectedMessage = String.format(NO_BEAN_CLASSES_WARNING, 'build-server-plugin.xml')
        assertThat(outputEventListener.toString(), containsString(expectedMessage))
    }

    @Test
    void logWarningMessageForMissingClass() {
        File definitionFile = project.file('build-server-plugin.xml')
        definitionFile << BEAN_DEFINITION_FILE
        definitions.add(new TeamCityPlugin.PluginDefinition(definitionFile))
        Action<Task> pluginValidationAction = new TeamCityPlugin.PluginDefinitionValidationAction(definitions, classes)
        outputEventListener.reset()

        pluginValidationAction.execute(stubTask)

        String expectedMessage = String.format(NO_BEAN_CLASS_WARNING, 'build-server-plugin.xml', 'example.Plugin')
        assertThat(outputEventListener.toString(), containsString(expectedMessage))
    }

    @Test
    void noWarningMessageWithClass() {
        File definitionFile = project.file('build-server-plugin.xml')
        definitionFile << BEAN_DEFINITION_FILE
        definitions.add(new TeamCityPlugin.PluginDefinition(definitionFile))
        classes.add('example/Plugin.class')
        Action<Task> pluginValidationAction = new TeamCityPlugin.PluginDefinitionValidationAction(definitions, classes)
        outputEventListener.reset()

        pluginValidationAction.execute(stubTask)

        String expectedMessage = String.format(NO_BEAN_CLASS_WARNING, 'build-server-plugin.xml', 'example.Plugin')
        assertThat(outputEventListener.toString(), not(containsString(expectedMessage)))
    }

    @Test
    void 'server plugin apply configures filesMatching actions on jar spec'() {
        project.pluginManager.apply(JavaPlugin)
        Jar mockJarTask = mockJar(project)

        project.pluginManager.apply(TeamCityServerPlugin)

        verify(mockJarTask).filesMatching(eq('META-INF/build-server-plugin*.xml') as String, any(TeamCityPlugin.PluginDefinitionCollectorAction))
        verify(mockJarTask).filesMatching(eq('**/*.class') as String, any(TeamCityPlugin.ClassCollectorAction))
    }

    @Test
    void 'server plugin apply configures doLast action on jar task'() {
        project.pluginManager.apply(JavaPlugin)
        Jar mockJarTask = mockJar(project)

        project.pluginManager.apply(TeamCityServerPlugin)

        verify(mockJarTask).doLast(any(TeamCityPlugin.PluginDefinitionValidationAction))
    }

    @Test
    void 'agent plugin apply configures filesMatching actions on jar spec'() {
        project.pluginManager.apply(JavaPlugin)
        Jar mockJarTask = mockJar(project)

        project.pluginManager.apply(TeamCityAgentPlugin)

        verify(mockJarTask).filesMatching(eq('META-INF/build-agent-plugin*.xml') as String, any(TeamCityPlugin.PluginDefinitionCollectorAction))
        verify(mockJarTask).filesMatching(eq('**/*.class') as String, any(TeamCityPlugin.ClassCollectorAction))
    }

    @Test
    void 'agent plugin apply configures doLast action on jar task'() {
        project.pluginManager.apply(JavaPlugin)
        Jar mockJarTask = mockJar(project)

        project.pluginManager.apply(TeamCityAgentPlugin)

        verify(mockJarTask).doLast(any(TeamCityPlugin.PluginDefinitionValidationAction))
    }

    @Test
    void 'PluginDefinitionCollector collects plugin definition files'() {
        List<TeamCityPlugin.PluginDefinition> definitions = new ArrayList<TeamCityPlugin.PluginDefinition>()
        Action<FileCopyDetails> collectorAction = new TeamCityPlugin.PluginDefinitionCollectorAction(definitions)
        File definitionFile = project.file('build-server-plugin.xml')
        FileCopyDetails stubDetails = mock(FileCopyDetails)
        when(stubDetails.getFile()).thenReturn(definitionFile)

        collectorAction.execute(stubDetails)

        assertThat(definitions.size(), equalTo(1))
        assertThat(definitions.get(0).name, equalTo('build-server-plugin.xml'))
    }

    @Test
    void "ClassCollector collects classes"() {
        Set<String> classList = new HashSet<String>()
        Action<FileCopyDetails> classManifestCollector = new TeamCityPlugin.ClassCollectorAction(classList)
        FileCopyDetails stubDetails = mock(FileCopyDetails)
        when(stubDetails.getRelativePath()).thenReturn(new RelativePath(true, 'com', 'example', 'Plugin.class'))

        classManifestCollector.execute(stubDetails)

        assertThat(classList.size(), equalTo(1))
        assertThat(classList, hasItem('com/example/Plugin.class'))
    }

    private static Jar mockJar(Project project) {
        Jar mockJar = mock(Jar)
        when(mockJar.getName()).thenReturn(JavaPlugin.JAR_TASK_NAME)
        when(mockJar.getConventionMapping()).thenReturn(mock(ConventionMapping))
        project.tasks.remove(project.tasks.getByName(JavaPlugin.JAR_TASK_NAME))
        project.tasks.add(mockJar)
        return mockJar
    }
}
