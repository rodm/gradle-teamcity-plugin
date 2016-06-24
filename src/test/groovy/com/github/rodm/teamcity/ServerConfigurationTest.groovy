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

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.endsWith
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.isA
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat
import static org.junit.Assert.fail

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

        assertNotNull(project.tasks.findByName('processServerDescriptor'))
        assertNotNull(project.tasks.findByName('generateServerDescriptor'))
        assertNotNull(project.tasks.findByName('serverPlugin'))
    }

    @Test
    public void serverPluginTasksWithFileDescriptor() {
        project.teamcity {
            descriptor = project.file('test-teamcity-plugin')
        }

        assertNotNull(project.tasks.findByName('processServerDescriptor'))
        assertNotNull(project.tasks.findByName('generateServerDescriptor'))
        assertNotNull(project.tasks.findByName('serverPlugin'))
    }

    @Test
    public void agentPluginDescriptorReplacementTokens() {
        project.teamcity {
            descriptor = project.file('test-teamcity-plugin')
            tokens VERSION: '1.2.3', VENDOR: 'rodm'
            tokens BUILD_NUMBER: '123'
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.server.tokens, hasEntry('VERSION', '1.2.3'))
        assertThat(extension.server.tokens, hasEntry('VENDOR', 'rodm'))
        assertThat(extension.server.tokens, hasEntry('BUILD_NUMBER', '123'))
    }

    @Test
    public void serverPluginDescriptorReplacementTokensAlternative() {
        project.teamcity {
            server {
                descriptor = project.file('test-teamcity-plugin')
                tokens VERSION: '1.2.3', VENDOR: 'rodm'
                tokens BUILD_NUMBER: '123'
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.server.tokens, hasEntry('VERSION', '1.2.3'))
        assertThat(extension.server.tokens, hasEntry('VENDOR', 'rodm'))
        assertThat(extension.server.tokens, hasEntry('BUILD_NUMBER', '123'))
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
    public void configuringAgentWithOnlyServerPluginFails() {
        try {
            project.teamcity {
                agent {}
            }
            fail("Configuring agent block should fail when the agent plugin is not applied")
        }
        catch (InvalidUserDataException expected) {
            assertEquals('Agent plugin configuration is invalid for a project without the teamcity-agent plugin', expected.message)
        }
    }

    @Test
    public void defaultProperties() {
        project.teamcity {
            server {
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.server.getDownloadsDir(), equalTo('downloads'))
        assertThat(extension.server.getBaseDownloadUrl(), equalTo('http://download.jetbrains.com/teamcity'))
        assertThat(extension.server.getBaseDataDir(), equalTo('data'))
        assertThat(extension.server.getBaseHomeDir(), equalTo('servers'))
    }

    @Test
    public void alternativeDownloadsDir() {
        project.teamcity {
            server {
                downloadsDir = '/tmp/downloads'
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.server.getDownloadsDir(), equalTo('/tmp/downloads'))
    }

    @Test
    public void alternativeDownloadBaseUrl() {
        project.teamcity {
            server {
                baseDownloadUrl = 'http://local-repository'
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.server.getBaseDownloadUrl(), equalTo('http://local-repository'))
    }

    @Test
    public void alternativeBaseDataDir() {
        project.teamcity {
            server {
                baseDataDir = '/tmp/data'
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.server.getBaseDataDir(), equalTo('/tmp/data'))
    }

    @Test
    public void alternativeBaseHomeDir() {
        project.teamcity {
            server {
                baseHomeDir = '/tmp/servers'
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.server.getBaseHomeDir(), equalTo('/tmp/servers'))
    }
}
