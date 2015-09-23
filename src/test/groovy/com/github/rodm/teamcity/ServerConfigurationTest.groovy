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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.endsWith
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.isA
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat

class ServerConfigurationTest {

    private Project project;

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.rodm.teamcity-server'
    }

    @Test
    public void buildScriptPluginDescriptor() {
        project.teamcity {
            descriptor {
                name = 'test plugin'
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.descriptor, isA(ServerPluginDescriptor))
        assertThat(extension.descriptor.getName(), equalTo('test plugin'))
    }

    @Test
    public void filePluginDescriptor() {
        project.teamcity {
            descriptor = project.file('test-teamcity-plugin.xml')
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.descriptor, isA(File))
        assertThat(extension.descriptor.getPath(), endsWith("test-teamcity-plugin.xml"))
    }

    @Test
    public void serverPluginTasks() {
        project.teamcity {
            descriptor {}
        }

        assertNotNull(project.tasks.findByName('processDescriptor'))
        assertNotNull(project.tasks.findByName('generateDescriptor'))
        assertNotNull(project.tasks.findByName('serverPlugin'))
    }

    @Test
    public void serverPluginTasksWithFileDescriptor() {
        project.teamcity {
            descriptor = project.file('test-teamcity-plugin')
        }

        assertNotNull(project.tasks.findByName('processDescriptor'))
        assertNotNull(project.tasks.findByName('generateDescriptor'))
        assertNotNull(project.tasks.findByName('serverPlugin'))
    }

    @Test
    public void serverPluginWithAdditionalFiles() {
        project.teamcity {
            server {
                files {
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.server.files.childSpecs.size, is(1))
    }

    @Test
    public void serverPluginWithAdditionalFilesAlternative() {
        project.teamcity {
            files {
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.server.files.childSpecs.size, is(1))
    }

    @Test
    public void teamcityDeployTasks() {
        assertNotNull(project.tasks.findByName('deployPlugin'))
        assertNotNull(project.tasks.findByName('undeployPlugin'))
    }

    @Test
    public void startStopTasks() {
        assertNotNull(project.tasks.findByName('startAgent'))
        assertNotNull(project.tasks.findByName('stopAgent'))
        assertNotNull(project.tasks.findByName('startServer'))
        assertNotNull(project.tasks.findByName('stopServer'))
    }
}
