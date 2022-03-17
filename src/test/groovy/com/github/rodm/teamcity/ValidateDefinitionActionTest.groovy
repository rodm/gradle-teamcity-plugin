/*
 * Copyright 2016 Rod MacKenzie
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
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir

import static com.github.rodm.teamcity.GradleMatchers.hasAction
import static com.github.rodm.teamcity.ValidationMode.FAIL
import static com.github.rodm.teamcity.ValidationMode.IGNORE
import static com.github.rodm.teamcity.ValidationMode.WARN
import static com.github.rodm.teamcity.internal.PluginDefinitionValidationAction.NO_BEAN_CLASSES_WARNING_MESSAGE
import static com.github.rodm.teamcity.internal.PluginDefinitionValidationAction.NO_BEAN_CLASS_WARNING_MESSAGE
import static com.github.rodm.teamcity.internal.PluginDefinitionValidationAction.NO_DEFINITION_WARNING_MESSAGE
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasKey
import static org.junit.jupiter.api.Assertions.fail
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

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

    private static final String NO_DEFINITION_WARNING = NO_DEFINITION_WARNING_MESSAGE.substring(4)
    private static final String NO_BEAN_CLASS_WARNING = NO_BEAN_CLASS_WARNING_MESSAGE.substring(4)
    private static final String NO_BEAN_CLASSES_WARNING = NO_BEAN_CLASSES_WARNING_MESSAGE.substring(4)

    private final ResettableOutputEventListener outputEventListener = new ResettableOutputEventListener()

    @RegisterExtension
    public final ConfigureLogging logging = new ConfigureLogging(outputEventListener)

    private Project project
    private Task stubTask
    private List<PluginDefinition> definitions
    private Set<String> classes

    @BeforeEach
    void setup(@TempDir File projectDir) {
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        stubTask = mock(Task)
        definitions = []
        classes = new HashSet<String>()
        when(stubTask.getProject()).thenReturn(project)
        when(stubTask.getLogger()).thenReturn(project.logger)
    }

    private PluginDefinitionValidationAction createValidationAction(ValidationMode mode = WARN) {
        new PluginDefinitionValidationAction(mode, definitions, classes)
    }

    @Test
    void logWarningMessageForMissingPluginDefinitionFiles() {
        Action<Task> pluginValidationAction = createValidationAction()

        pluginValidationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), containsString(NO_DEFINITION_WARNING))
    }

    @Test
    void noWarningMessageWithPluginDefinition() {
        File definitionFile = project.file('build-server-plugin.xml')
        definitionFile << BEAN_DEFINITION_FILE
        definitions.add(new PluginDefinition(definitionFile))
        Action<Task> pluginValidationAction = createValidationAction()
        outputEventListener.reset()

        pluginValidationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), not(containsString(NO_DEFINITION_WARNING)))
    }

    @Test
    void logWarningMessageForEmptyDefinitionFile() {
        File definitionFile = project.file('build-server-plugin.xml')
        definitionFile << EMPTY_BEAN_DEFINITION_FILE
        definitions.add(new PluginDefinition(definitionFile))
        Action<Task> pluginValidationAction = createValidationAction()
        outputEventListener.reset()

        pluginValidationAction.execute(stubTask)

        String expectedMessage = String.format(NO_BEAN_CLASSES_WARNING, 'build-server-plugin.xml')
        assertThat(outputEventListener.toString(), containsString(expectedMessage))
    }

    @Test
    void logWarningMessageForMissingClass() {
        File definitionFile = project.file('build-server-plugin.xml')
        definitionFile << BEAN_DEFINITION_FILE
        definitions.add(new PluginDefinition(definitionFile))
        Action<Task> pluginValidationAction = createValidationAction()
        outputEventListener.reset()

        pluginValidationAction.execute(stubTask)

        String expectedMessage = String.format(NO_BEAN_CLASS_WARNING, 'build-server-plugin.xml', 'example.Plugin')
        assertThat(outputEventListener.toString(), containsString(expectedMessage))
    }

    @Test
    void noWarningMessageWithClass() {
        File definitionFile = project.file('build-server-plugin.xml')
        definitionFile << BEAN_DEFINITION_FILE
        definitions.add(new PluginDefinition(definitionFile))
        classes.add('example/Plugin.class')
        Action<Task> pluginValidationAction = createValidationAction()
        outputEventListener.reset()

        pluginValidationAction.execute(stubTask)

        String expectedMessage = String.format(NO_BEAN_CLASS_WARNING, 'build-server-plugin.xml', 'example.Plugin')
        assertThat(outputEventListener.toString(), not(containsString(expectedMessage)))
    }

    @Test
    void 'no warning message for missing plugin definition files with validation mode set to ignore'() {
        Action<Task> pluginValidationAction = createValidationAction(IGNORE)

        pluginValidationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), not(containsString(NO_DEFINITION_WARNING)))
    }

    @Test
    void 'no warning message for invalid definition files with validation mode set to ignore'() {
        File emptyDefinitionFile = project.file('build-server-plugin1.xml')
        emptyDefinitionFile << EMPTY_BEAN_DEFINITION_FILE
        definitions.add(new PluginDefinition(emptyDefinitionFile))
        File definitionFile = project.file('build-server-plugin2.xml')
        definitionFile << BEAN_DEFINITION_FILE
        definitions.add(new PluginDefinition(definitionFile))
        outputEventListener.reset()

        Action<Task> pluginValidationAction = createValidationAction(IGNORE)
        pluginValidationAction.execute(stubTask)

        String noBeanClassesMessage = String.format(NO_BEAN_CLASSES_WARNING, 'build-server-plugin1.xml')
        assertThat(outputEventListener.toString(), not(containsString(noBeanClassesMessage)))
        String noBeanClassMessage = String.format(NO_BEAN_CLASS_WARNING, 'build-server-plugin2.xml', 'example.Plugin')
        assertThat(outputEventListener.toString(), not(containsString(noBeanClassMessage)))
    }

    @Test
    void 'throws exception for missing plugin definition files with validation mode set to fail'() {
        Action<Task> pluginValidationAction = createValidationAction(FAIL)

        try {
            pluginValidationAction.execute(stubTask)
            fail("Should throw exception when plugin definition is missing and mode is set to fail")
        }
        catch (GradleException e) {
            assertThat(e.message, equalTo('Plugin definition validation failed'))
        }

        assertThat(outputEventListener.toString(), containsString(NO_DEFINITION_WARNING))
    }

    @Test
    void 'throws exception for invalid plugin definition files with validation mode set to fail'() {
        File emptyDefinitionFile = project.file('build-server-plugin1.xml')
        emptyDefinitionFile << EMPTY_BEAN_DEFINITION_FILE
        definitions.add(new PluginDefinition(emptyDefinitionFile))
        File definitionFile = project.file('build-server-plugin2.xml')
        definitionFile << BEAN_DEFINITION_FILE
        definitions.add(new PluginDefinition(definitionFile))
        outputEventListener.reset()

        try {
            Action<Task> pluginValidationAction = createValidationAction(FAIL)
            pluginValidationAction.execute(stubTask)
            fail("Should throw exception when plugin definitions are invalid and mode is set to fail")
        }
        catch (GradleException e) {
            assertThat(e.message, equalTo('Plugin definition validation failed'))
        }

        String noBeanClassesMessage = String.format(NO_BEAN_CLASSES_WARNING, 'build-server-plugin1.xml')
        assertThat(outputEventListener.toString(), containsString(noBeanClassesMessage))
        String noBeanClassMessage = String.format(NO_BEAN_CLASS_WARNING, 'build-server-plugin2.xml', 'example.Plugin')
        assertThat(outputEventListener.toString(), containsString(noBeanClassMessage))
    }

    @Test
    void 'server plugin apply configures filesMatching actions on jar spec'() {
        project.pluginManager.apply(JavaPlugin)
        Jar jar = replaceJar(project)

        project.pluginManager.apply(TeamCityServerPlugin)
        project.evaluate()

        def descriptorPattern = 'META-INF/build-server-plugin*.xml'
        assertThat(jar.filesMatching, hasKey(descriptorPattern))
        assertThat(jar.filesMatching.get(descriptorPattern).getClass().name, equalTo(PluginDefinitionCollectorAction.name))
        def classPattern = '**/*.class'
        assertThat(jar.filesMatching, hasKey(classPattern))
        assertThat(jar.filesMatching.get(classPattern).getClass().name, equalTo(ClassCollectorAction.name))
    }

    @Test
    void 'server plugin apply configures doLast action on jar task'() {
        project.pluginManager.apply(JavaPlugin)
        project.pluginManager.apply(TeamCityServerPlugin)
        project.evaluate()

        Jar jar = project.tasks.getByName('jar') as Jar
        assertThat(jar, hasAction(PluginDefinitionValidationAction))
    }

    @Test
    void 'applying java plugin after server plugin configures jar task'() {
        project.pluginManager.apply(TeamCityServerPlugin)
        project.pluginManager.apply(JavaPlugin)
        project.evaluate()

        Jar jar = project.tasks.getByName('jar') as Jar
        assertThat(jar, hasAction(PluginDefinitionValidationAction))
    }

    @Test
    void 'agent plugin apply configures filesMatching actions on jar spec'() {
        project.pluginManager.apply(JavaPlugin)
        SpyJar mockJarTask = replaceJar(project)

        project.pluginManager.apply(TeamCityAgentPlugin)
        project.evaluate()

        def descriptorPattern = 'META-INF/build-agent-plugin*.xml'
        assertThat(mockJarTask.filesMatching, hasKey(descriptorPattern))
        assertThat(mockJarTask.filesMatching.get(descriptorPattern).getClass().name, equalTo(PluginDefinitionCollectorAction.name))
        def classPattern = '**/*.class'
        assertThat(mockJarTask.filesMatching, hasKey(classPattern))
        assertThat(mockJarTask.filesMatching.get(classPattern).getClass().name, equalTo(ClassCollectorAction.name))
    }

    @Test
    void 'agent plugin apply configures doLast action on jar task'() {
        project.pluginManager.apply(JavaPlugin)
        project.pluginManager.apply(TeamCityAgentPlugin)
        project.evaluate()

        Jar jar = project.tasks.getByName('jar') as Jar
        assertThat(jar, hasAction(PluginDefinitionValidationAction))
    }

    @Test
    void 'applying java plugin after agent plugin configures jar task'() {
        project.pluginManager.apply(TeamCityAgentPlugin)
        project.pluginManager.apply(JavaPlugin)
        project.evaluate()

        Jar jar = project.tasks.getByName('jar') as Jar
        assertThat(jar, hasAction(PluginDefinitionValidationAction))
    }

    @Test
    void 'PluginDefinitionCollector collects plugin definition files'() {
        List<PluginDefinition> definitions = new ArrayList<PluginDefinition>()
        Action<FileCopyDetails> collectorAction = new PluginDefinitionCollectorAction(definitions)
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
        Action<FileCopyDetails> classManifestCollector = new ClassCollectorAction(classList)
        FileCopyDetails stubDetails = mock(FileCopyDetails)
        when(stubDetails.getRelativePath()).thenReturn(new RelativePath(true, 'com', 'example', 'Plugin.class'))

        classManifestCollector.execute(stubDetails)

        assertThat(classList.size(), equalTo(1))
        assertThat(classList, hasItem('com/example/Plugin.class'))
    }

    private static SpyJar replaceJar(Project project) {
        SpyJar task = project.tasks.replace(JavaPlugin.JAR_TASK_NAME, SpyJar.class)
        return task
    }

    private static class SpyJar extends Jar {

        Map<String, Action> filesMatching = new HashMap()

        @Override
        AbstractCopyTask filesMatching(String pattern, Action<? super FileCopyDetails> action) {
            filesMatching.put(pattern, action)
            return super.filesMatching(pattern, action)
        }
    }
}
