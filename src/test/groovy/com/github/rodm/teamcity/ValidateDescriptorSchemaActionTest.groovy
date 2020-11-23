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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.MatcherAssert.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ValidateDescriptorSchemaActionTest {

    private final ResettableOutputEventListener outputEventListener = new ResettableOutputEventListener()

    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder()

    @Rule
    public final ConfigureLogging logging = new ConfigureLogging(outputEventListener)

    private Project project
    private Task stubTask
    private File descriptorFile

    @Before
    void setup() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.root).build()
        stubTask = mock(Task)
        when(stubTask.getProject()).thenReturn(project)
        when(stubTask.getLogger()).thenReturn(project.logger)

        descriptorFile = project.file('teamcity-plugin.xml')
        descriptorFile << '''<?xml version="1.0" encoding="UTF-8"?>
        <teamcity-plugin>
            <info>
                <name>name</name>
                <display-name>display name</display-name>
                <description>plugin description</description>
                <version>version</version>
                <vendor>
                    <name>vendor name</name>
                    <url>http://example.com</url>
                </vendor>
            </info>
            <deployment allow-runtime-reload='true' node-responsibilities-aware='true'/>
        </teamcity-plugin>
        '''
    }

    private validationAction(String schema, File descriptorFile) {
        def file = project.objects.fileProperty().fileValue(descriptorFile)
        new TeamCityPlugin.PluginDescriptorValidationAction(schema, file)
    }

    @Test
    void 'warn about allow-reload-plugin attribute when using schema for TeamCity 2018_1 and earlier'() {
        def schema = 'teamcity-server-plugin-descriptor.xsd'
        Action<Task> validationAction = validationAction(schema, descriptorFile)

        validationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), containsString(warningFor('allow-runtime-reload', 'deployment')))
    }

    @Test
    void 'no warnings when using schema for TeamCity 2018_2 and later'() {
        def schema = '2018.2/teamcity-server-plugin-descriptor.xsd'
        Action<Task> validationAction = validationAction(schema, descriptorFile)

        validationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), not(containsString(warningFor('allow-runtime-reload', 'deployment'))))
    }

    @Test
    void 'warn about node-responsibilities-aware attribute when using schema for TeamCity 2019_2 and earlier'() {
        def schema = 'teamcity-server-plugin-descriptor.xsd'
        Action<Task> validationAction = validationAction(schema, descriptorFile)

        validationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), containsString(warningFor('node-responsibilities-aware', 'deployment')))
    }

    @Test
    void 'no warning about node-responsibilities-aware when using schema for TeamCity 2020_1 and later'() {
        def schema = '2020.1/teamcity-server-plugin-descriptor.xsd'
        Action<Task> validationAction = validationAction(schema, descriptorFile)

        validationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), not(containsString(warningFor('node-responsibilities-aware', 'deployment'))))
    }

    private static String warningFor(String attribute, String element) {
        return String.format("Attribute '%s' is not allowed to appear in element '%s'", attribute, element)
    }
}
