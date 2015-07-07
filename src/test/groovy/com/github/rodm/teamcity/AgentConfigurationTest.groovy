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

import static org.hamcrest.CoreMatchers.isA
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasSize
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertThat

class AgentConfigurationTest {

    private Project project;

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.rodm.teamcity'
    }

    @Test
    public void createDescriptorForAgentProjectType() {
        project.teamcity {
            type = 'agent-plugin'

            descriptor {
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.descriptor, isA(AgentPluginDescriptor))
    }

    @Test
    public void createDescriptorForPluginDeployment() {
        project.teamcity {
            type = 'agent-plugin'

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
            type = 'agent-plugin'

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
            type = 'agent-plugin'

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
            type = 'agent-plugin'

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
            type = 'agent-plugin'
            descriptor {}
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        TeamCityPlugin plugin = project.plugins.getPlugin(TeamCityPlugin)
        plugin.configureAgentPluginTasks(project, extension)

        assertNotNull(project.tasks.findByName('generateAgentDescriptor'))
        assertNotNull(project.tasks.findByName('packageAgentPlugin'))
        assertNull(project.tasks.findByName('processAgentDescriptor'))
    }

    @Test
    public void agentPluginTasksWithFileDescriptor() {
        project.teamcity {
            type = 'agent-plugin'
            descriptor = project.file('test-teamcity-plugin')
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        TeamCityPlugin plugin = project.plugins.getPlugin(TeamCityPlugin)
        plugin.configureAgentPluginTasks(project, extension)

        assertNull(project.tasks.findByName('generateAgentDescriptor'))
        assertNotNull(project.tasks.findByName('packageAgentPlugin'))
        assertNotNull(project.tasks.findByName('processAgentDescriptor'))
    }

    @Test
    public void noServerPluginTasks() {
        project.teamcity {
            type = 'agent-plugin'
            descriptor {}
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        TeamCityPlugin plugin = project.plugins.getPlugin(TeamCityPlugin)
        plugin.configureServerPluginTasks(project, extension)

        assertNull(project.tasks.findByName('processDescriptor'))
        assertNull(project.tasks.findByName('packagePlugin'))
        assertNull(project.tasks.findByName('generateDescriptor'))
        assertNull(project.tasks.findByName('deployPlugin'))
        assertNull(project.tasks.findByName('undeployPlugin'))
        assertNull(project.tasks.findByName('startAgent'))
        assertNull(project.tasks.findByName('stopAgent'))
        assertNull(project.tasks.findByName('startServer'))
        assertNull(project.tasks.findByName('stopServer'))
    }
}
