/*
 * Copyright 2018 Rod MacKenzie
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

import com.github.rodm.teamcity.internal.PluginDefinition
import com.github.rodm.teamcity.internal.PluginDefinitionValidationAction
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir

import static com.github.rodm.teamcity.ValidationMode.WARN
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.MatcherAssert.assertThat

@SuppressWarnings('ConfigurationAvoidance')
class ValidateDefinitionActionOfflineTest {

    public static final String BEAN_DEFINITION_FILE = """<?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN//EN" "http://www.springframework.org/dtd/spring-beans.dtd">
            <beans default-autowire="constructor">
                <bean id="examplePlugin" class="example.Plugin"/>
            </beans>
        """
    private static final String NO_BEANS_PARSING_MESSAGE = 'Failed to parse plugin definition file '

    private final ResettableOutputEventListener outputEventListener = new ResettableOutputEventListener()

    @RegisterExtension
    public final ConfigureLogging logging = new ConfigureLogging(outputEventListener)

    private Project project
    private Task stubTask
    private List<PluginDefinition> definitions = []
    private Set<String> classes = new HashSet<>()

    @BeforeEach
    void setup(@TempDir File projectDir) {
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        stubTask = project.tasks.create('stub')

        File definitionFile = project.file('build-server-plugin.xml')
        definitionFile << BEAN_DEFINITION_FILE
        definitions.add(new PluginDefinition(definitionFile))
        outputEventListener.reset()
    }

    @BeforeEach
    void disableNetworking() {
        // Using the proxy properties causes the XML parser to fail to download DTDs
        System.setProperty("socksProxyHost", "127.0.0.1")
        System.setProperty("socksProxyPort", "8080")
    }

    @AfterEach
    void enableNetworking() {
        System.clearProperty("socksProxyHost")
        System.clearProperty("socksProxyPort")
    }

    private PluginDefinitionValidationAction createValidationAction() {
        new PluginDefinitionValidationAction(WARN, definitions, classes)
    }

    @Test
    void 'output warning message on failed bean definition parsing failure'() {
        Action<Task> validationAction = createValidationAction()

        validationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), containsString(NO_BEANS_PARSING_MESSAGE))
    }

    @Test
    void 'no warning message on failed bean definition parsing failure with offline option'() {
        Action<Task> validationAction = createValidationAction()

        stubTask.inputs.property('gradle-offline', true)
        validationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), not(containsString(NO_BEANS_PARSING_MESSAGE)))
    }
}
