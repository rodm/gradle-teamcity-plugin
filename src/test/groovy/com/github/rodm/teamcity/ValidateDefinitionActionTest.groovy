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
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.logging.events.LogEvent
import org.gradle.internal.logging.events.OutputEvent
import org.gradle.internal.logging.events.OutputEventListener
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static org.hamcrest.CoreMatchers.containsString
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
    public final ConfigureLogging logging = new ConfigureLogging(outputEventListener)

    private Task stubTask
    private List<TeamCityPlugin.PluginDefinition> definitions
    private Set<String> classes

    @Before
    public void setup() {
        stubTask = mock(Task)
        definitions = []
        classes = new HashSet<String>()
    }

    @Test
    public void logWarningMessageForMissingPluginDefinitionFiles() {
        Action<Task> pluginValidationAction = new TeamCityPlugin.PluginDefinitionValidationAction(definitions, classes)

        pluginValidationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), containsString(NO_DEFINITION_WARNING))
    }

    @Test
    public void noWarningMessageWithPluginDefinition() {
        Project project = ProjectBuilder.builder().build()
        File definitionFile = project.file('build-server-plugin.xml')
        definitionFile << BEAN_DEFINITION_FILE
        definitions.add(new TeamCityPlugin.PluginDefinition(definitionFile))
        Action<Task> pluginValidationAction = new TeamCityPlugin.PluginDefinitionValidationAction(definitions, classes)
        outputEventListener.reset()

        pluginValidationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), not(containsString(NO_DEFINITION_WARNING)))
    }

    @Test
    public void logWarningMessageForEmptyDefinitionFile() {
        Project project = ProjectBuilder.builder().build()
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
    public void logWarningMessageForMissingClass() {
        Project project = ProjectBuilder.builder().build()
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
    public void noWarningMessageWithClass() {
        Project project = ProjectBuilder.builder().build()
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
    public void 'server plugin apply configures filesMatching actions on jar spec'() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply(JavaPlugin)
        Jar mockJarTask = mockJar(project)

        project.pluginManager.apply(TeamCityServerPlugin)

        verify(mockJarTask).filesMatching(eq('META-INF/build-server-plugin*.xml'), any(Action))
        verify(mockJarTask).filesMatching(eq('**/*.class'), any(Action))
    }

    @Test
    public void 'server plugin apply configures doLast action on jar task'() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply(JavaPlugin)
        Jar mockJarTask = mockJar(project)

        project.pluginManager.apply(TeamCityServerPlugin)

        verify(mockJarTask).doLast(any(TeamCityPlugin.PluginDefinitionValidationAction))
    }

    @Test
    public void 'agent plugin apply configures filesMatching actions on jar spec'() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply(JavaPlugin)
        Jar mockJarTask = mockJar(project)

        project.pluginManager.apply(TeamCityAgentPlugin)

        verify(mockJarTask).filesMatching(eq('META-INF/build-agent-plugin*.xml'), any(Action))
        verify(mockJarTask).filesMatching(eq('**/*.class'), any(Action))
    }

    @Test
    public void 'agent plugin apply configures doLast action on jar task'() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply(JavaPlugin)
        Jar mockJarTask = mockJar(project)

        project.pluginManager.apply(TeamCityAgentPlugin)

        verify(mockJarTask).doLast(any(TeamCityPlugin.PluginDefinitionValidationAction))
    }

    private Jar mockJar(Project project) {
        Jar mockJar = mock(Jar)
        when(mockJar.getName()).thenReturn(JavaPlugin.JAR_TASK_NAME)
        when(mockJar.getConventionMapping()).thenReturn(mock(ConventionMapping))
        project.tasks.remove(project.tasks.getByName(JavaPlugin.JAR_TASK_NAME))
        project.tasks.add(mockJar)
        return mockJar
    }

    static class ResettableOutputEventListener implements OutputEventListener {
        final StringBuffer buffer = new StringBuffer()

        void reset() {
            buffer.delete(0, buffer.size())
        }

        @Override
        String toString() {
            return buffer.toString()
        }

        @Override
        synchronized void onOutput(OutputEvent event) {
            LogEvent logEvent = event as LogEvent
            buffer.append(logEvent.message)
        }
    }
}
