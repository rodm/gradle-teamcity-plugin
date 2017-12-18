/*
 * Copyright 2017 Rod MacKenzie
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

import static com.github.rodm.teamcity.TeamCityServerPlugin.PluginDescriptorContentsValidationAction.EMPTY_VALUE_WARNING_MESSAGE
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.MatcherAssert.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ValidateDescriptorActionTest {

    private static final String EMPTY_VALUE_WARNING = EMPTY_VALUE_WARNING_MESSAGE.substring(4)

    private final ResettableOutputEventListener outputEventListener = new ResettableOutputEventListener()

    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder()

    @Rule
    public final ConfigureLogging logging = new ConfigureLogging(outputEventListener)

    private Project project
    private Task stubTask

    @Before
    void setup() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.root).build()
        stubTask = mock(Task)
        when(stubTask.getProject()).thenReturn(project)
    }

    @Test
    void 'warn about required descriptor values being empty'() {
        File descriptorFile = project.file('teamcity-plugin.xml')
        descriptorFile << '''<?xml version="1.0" encoding="UTF-8"?>
        <teamcity-plugin>
            <info>
                <name> </name>
                <display-name> </display-name>
                <version> </version>
                <vendor>
                    <name> </name>
                </vendor>
            </info>
        </teamcity-plugin>
        '''
        Action<Task> validationAction = new TeamCityServerPlugin.PluginDescriptorContentsValidationAction(descriptorFile)

        validationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), containsString(warningFor('name')))
        assertThat(outputEventListener.toString(), containsString(warningFor('display name')))
        assertThat(outputEventListener.toString(), containsString(warningFor('version')))
        assertThat(outputEventListener.toString(), containsString(warningFor('vendor name')))
    }

    @Test
    void 'warn about empty descriptor values required for publishing a plugin'() {
        // publishing a plugin to the JetBrains TeamCity Plugin Repository, https://plugins.jetbrains.com/teamcity
        // requires the description and vendor url descriptor elements to be populated
        File descriptorFile = project.file('teamcity-plugin.xml')
        descriptorFile << '''<?xml version="1.0" encoding="UTF-8"?>
        <teamcity-plugin>
            <info>
                <name>plugin </name>
                <display-name>Example plugin</display-name>
                <version>1.0</version>
                <vendor>
                    <name>example</name>
                </vendor>
            </info>
        </teamcity-plugin>
        '''

        Action<Task> validationAction = new TeamCityServerPlugin.PluginDescriptorContentsValidationAction(descriptorFile)
        validationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), containsString(warningFor('description')))
        assertThat(outputEventListener.toString(), containsString(warningFor('vendor url')))
    }

    @Test
    void 'no warnings when required descriptor values are not empty'() {
        File descriptorFile = project.file('teamcity-plugin.xml')
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
        </teamcity-plugin>
        '''
        Action<Task> validationAction = new TeamCityServerPlugin.PluginDescriptorContentsValidationAction(descriptorFile)
        outputEventListener.reset()

        validationAction.execute(stubTask)

        assertThat(outputEventListener.toString(), not(containsString(warningFor('name'))))
        assertThat(outputEventListener.toString(), not(containsString(warningFor('display name'))))
        assertThat(outputEventListener.toString(), not(containsString(warningFor('version'))))
        assertThat(outputEventListener.toString(), not(containsString(warningFor('vendor name'))))
        assertThat(outputEventListener.toString(), not(containsString(warningFor('description'))))
        assertThat(outputEventListener.toString(), not(containsString(warningFor('vendor url'))))
    }

    private static String warningFor(String name) {
        return String.format(EMPTY_VALUE_WARNING, name)
    }
}
