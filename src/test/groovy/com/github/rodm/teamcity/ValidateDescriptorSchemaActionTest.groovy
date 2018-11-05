/*
 * Copyright 2018 Rod MacKenzie
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
            <deployment allow-runtime-reload='true'/>
        </teamcity-plugin>
        '''
    }

    @Test
    void 'warn about allow-reload-plugin attribute when using schema for TeamCity 2018.1 and earlier'() {
        def schema = 'teamcity-server-plugin-descriptor.xsd'
        Action<Task> validationAction = new TeamCityPlugin.PluginDescriptorValidationAction(schema, descriptorFile)

        validationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), containsString(warningFor('allow-runtime-reload', 'deployment')))
    }

    @Test
    void 'no warnings when using schema for TeamCity 2018.2 and later'() {
        def schema = 'teamcity-server-plugin-descriptor.xsd'
        Action<Task> validationAction = new TeamCityPlugin.PluginDescriptorValidationAction(schema, descriptorFile)

        validationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), not(containsString(warningFor('allow', 'deployment'))))
    }

    private static String warningFor(String attribute, String element) {
        return String.format("Attribute '%s' is not allowed to appear in element '%s'", attribute, element)
    }
}
