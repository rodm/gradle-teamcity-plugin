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

import static org.hamcrest.CoreMatchers.isA
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.fail

class AgentConfigurationTest {

    private Project project;

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.rodm.teamcity-agent'
    }

    @Test
    public void createDescriptorForAgentProjectType() {
        project.teamcity {
            descriptor {
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.descriptor, isA(AgentPluginDescriptor))
    }

    @Test
    public void createDescriptorForPluginDeployment() {
        project.teamcity {
            descriptor {
                pluginDeployment {
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.descriptor.deployment, isA(PluginDeployment))
    }

    @Test
    public void createDescriptorForPluginDeploymentWithExecutableFiles() {
        project.teamcity {
            descriptor {
                pluginDeployment {
                    useSeparateClassloader = true
                    executableFiles {
                        include 'file1'
                        include 'file2'
                    }
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        PluginDeployment deployment = extension.descriptor.deployment
        assertThat(deployment.useSeparateClassloader, equalTo(Boolean.TRUE))
        assertThat(deployment.executableFiles.includes, hasSize(2))
    }

    @Test
    public void createDescriptorForToolDeployment() {
        project.teamcity {
            descriptor {
                toolDeployment {
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.descriptor.deployment, isA(ToolDeployment))
    }

    @Test
    public void createDescriptorForToolDeploymentWithExecutableFiles() {
        project.teamcity {
            descriptor {
                toolDeployment {
                    executableFiles {
                        include 'file1'
                        include 'file2'
                    }
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        ToolDeployment deployment = extension.descriptor.deployment
        assertThat(deployment.executableFiles.includes, hasSize(2))
    }

    @Test
    public void agentPluginTasks() {
        project.teamcity {
            descriptor {}
        }

        assertNotNull(project.tasks.findByName('generateAgentDescriptor'))
        assertNotNull(project.tasks.findByName('processAgentDescriptor'))
        assertNotNull(project.tasks.findByName('agentPlugin'))
    }

    @Test
    public void agentPluginTasksWithFileDescriptor() {
        project.teamcity {
            descriptor = project.file('test-teamcity-plugin')
        }

        assertNotNull(project.tasks.findByName('generateAgentDescriptor'))
        assertNotNull(project.tasks.findByName('processAgentDescriptor'))
        assertNotNull(project.tasks.findByName('agentPlugin'))
    }

    @Test
    public void agentPluginDescriptorReplacementTokens() {
        project.teamcity {
            descriptor = project.file('test-teamcity-plugin')
            tokens VERSION: '1.2.3', VENDOR: 'rodm'
            tokens BUILD_NUMBER: '123'
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.agent.tokens, hasEntry('VERSION', '1.2.3'))
        assertThat(extension.agent.tokens, hasEntry('VENDOR', 'rodm'))
        assertThat(extension.agent.tokens, hasEntry('BUILD_NUMBER', '123'))
    }

    @Test
    public void agentPluginDescriptorReplacementTokensAlternative() {
        project.teamcity {
            agent {
                descriptor = project.file('test-teamcity-plugin')
                tokens VERSION: '1.2.3', VENDOR: 'rodm'
                tokens BUILD_NUMBER: '123'
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.agent.tokens, hasEntry('VERSION', '1.2.3'))
        assertThat(extension.agent.tokens, hasEntry('VENDOR', 'rodm'))
        assertThat(extension.agent.tokens, hasEntry('BUILD_NUMBER', '123'))
    }

    @Test
    public void agentPluginWithAdditionalFiles() {
        project.teamcity {
            agent {
                files {
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.agent.files.childSpecs.size, is(1))
    }

    @Test
    public void agentPluginWithAdditionalFilesAlternative() {
        project.teamcity {
            files {
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.agent.files.childSpecs.size, is(1))
    }

    @Test
    public void configuringServerWithOnlyAgentPluginFails() {
        try {
            project.teamcity {
                server {}
            }
            fail("Configuring server block should fail when the server plugin is not applied")
        }
        catch (InvalidUserDataException expected) {
            assertEquals('Server plugin configuration is invalid for a project without the teamcity-server plugin', expected.message)
        }
    }
}
