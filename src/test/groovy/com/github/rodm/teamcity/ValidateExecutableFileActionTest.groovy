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

import com.github.rodm.teamcity.internal.AbstractPluginTask
import com.github.rodm.teamcity.internal.PluginExecutableFilesValidationAction
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCopyDetails
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.not
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ValidateExecutableFileActionTest {

    private static final String AGENT_PLUGIN_DESCRIPTOR = '''<?xml version="1.0" encoding="UTF-8"?>
        <teamcity-agent-plugin>
          <tool-deployment>
            <layout>
              <executable-files>
                <include name="test1"/>
                <include name="bin/test2"/>
              </executable-files>
            </layout>
          </tool-deployment>
        </teamcity-agent-plugin>
    '''

    private static final String MISSING_EXECUTABLE_FILE_WARNING = PluginExecutableFilesValidationAction.MISSING_EXECUTABLE_FILE_WARNING.substring(4)

    private final ResettableOutputEventListener outputEventListener = new ResettableOutputEventListener()

    @RegisterExtension
    public final ConfigureLogging logging = new ConfigureLogging(outputEventListener)

    private Project project
    private File descriptorFile
    private AbstractPluginTask stubTask

    static abstract class DummyPluginTask extends AbstractPluginTask {}

    @BeforeEach
    void setup(@TempDir File projectDir) {
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        descriptorFile = project.file('teamcity-plugin.xml')
        stubTask = project.tasks.create('dummy', DummyPluginTask)
        stubTask.descriptor.set(descriptorFile)
    }

    private static FileCopyDetails fileCopyDetails(String path) {
        FileCopyDetails fileCopyDetails = mock(FileCopyDetails)
        when(fileCopyDetails.getPath()).thenReturn(path)
        return fileCopyDetails
    }

    private validationAction(Set<FileCopyDetails> files) {
        new PluginExecutableFilesValidationAction(files)
    }

    @Test
    void 'output warning when executable file is missing'() {
        descriptorFile << AGENT_PLUGIN_DESCRIPTOR
        Set<FileCopyDetails> files = [fileCopyDetails('test1')]
        Action<Task> validationAction = validationAction(files)
        outputEventListener.reset()

        validationAction.execute(stubTask)

        String message = MISSING_EXECUTABLE_FILE_WARNING.replace('{}', 'bin/test2')
        assertThat(outputEventListener.toString(), containsString(message))
    }

    @Test
    void 'does not output warning when executable file is present'() {
        descriptorFile << AGENT_PLUGIN_DESCRIPTOR
        Set<FileCopyDetails> files = [fileCopyDetails('test1'), fileCopyDetails('bin/test2')]
        Action<Task> validationAction =  validationAction(files)
        outputEventListener.reset()

        validationAction.execute(stubTask)

        String message = String.format(MISSING_EXECUTABLE_FILE_WARNING, 'bin/test2')
        assertThat(outputEventListener.toString(), not(containsString(message)))
    }
}
