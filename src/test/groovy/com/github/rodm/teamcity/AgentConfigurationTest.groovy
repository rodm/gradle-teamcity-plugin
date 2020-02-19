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

import com.github.rodm.teamcity.tasks.GenerateAgentPluginDescriptor
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.bundling.Zip
import org.junit.Before
import org.junit.Test

import static com.github.rodm.teamcity.GradleMatchers.hasDependency
import static com.github.rodm.teamcity.TestSupport.normalizePath
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.endsWith
import static org.hamcrest.CoreMatchers.isA
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.fail

class AgentConfigurationTest extends ConfigurationTestCase {

    @Before
    void applyPlugin() {
        project.apply plugin: 'com.github.rodm.teamcity-agent'
        extension = project.extensions.getByType(TeamCityPluginExtension)
    }

    @Test
    void createDescriptorForPluginDeployment() {
        project.teamcity {
            agent {
                descriptor {
                    pluginDeployment {
                    }
                }
            }
        }

        assertThat(extension.agent.descriptor.deployment, isA(PluginDeployment))
    }

    @Test
    void createDescriptorForPluginDeploymentWithExecutableFiles() {
        project.teamcity {
            agent {
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
        }

        PluginDeployment deployment = extension.agent.descriptor.deployment
        assertThat(deployment.useSeparateClassloader, equalTo(Boolean.TRUE))
        assertThat(deployment.executableFiles.includes, hasSize(2))
    }

    @Test
    void createDescriptorForToolDeployment() {
        project.teamcity {
            agent {
                descriptor {
                    toolDeployment {
                    }
                }
            }
        }

        assertThat(extension.agent.descriptor.deployment, isA(ToolDeployment))
    }

    @Test
    void createDescriptorForToolDeploymentWithExecutableFiles() {
        project.teamcity {
            agent {
                descriptor {
                    toolDeployment {
                        executableFiles {
                            include 'file1'
                            include 'file2'
                        }
                    }
                }
            }
        }

        ToolDeployment deployment = extension.agent.descriptor.deployment
        assertThat(deployment.executableFiles.includes, hasSize(2))
    }

    @Test
    void 'allow agent descriptor and plugin deployment to be created from multiple configuration blocks'() {
        project.teamcity {
            agent {
                descriptor {
                    pluginDeployment {
                        useSeparateClassloader = true
                    }
                }
            }
        }
        project.teamcity {
            agent {
                descriptor {
                    pluginDeployment {
                        executableFiles {
                            include 'file1'
                            include 'file2'
                        }
                    }
                }
            }
        }

        PluginDeployment deployment = extension.agent.descriptor.deployment
        assertThat(deployment.useSeparateClassloader, equalTo(Boolean.TRUE))
        assertThat(deployment.executableFiles.includes, hasSize(2))
    }

    @Test
    void 'allow agent descriptor and tool deployment to be created from multiple configuration blocks'() {
        project.teamcity {
            agent {
                descriptor {
                    toolDeployment {
                        executableFiles {
                            include 'file1'
                        }
                    }
                }
            }
        }
        project.teamcity {
            agent {
                descriptor {
                    toolDeployment {
                        executableFiles {
                            include 'file2'
                        }
                    }
                }
            }
        }

        ToolDeployment deployment = extension.agent.descriptor.deployment
        assertThat(deployment.executableFiles.includes, hasSize(2))
    }

    @Test
    void 'cannot configure plugin for both pluginDeployment and toolDeployment'() {
        try {
            project.teamcity {
                agent {
                    descriptor {
                        pluginDeployment {}
                        toolDeployment {}
                    }
                }
            }
        }
        catch (InvalidUserDataException expected) {
            assertThat(expected.message, equalTo('Agent plugin cannot be configured for plugin deployment and tool deployment'))
        }
    }

    @Test
    void 'cannot configure plugin for both toolDeployment and pluginDeployment'() {
        try {
            project.teamcity {
                agent {
                    descriptor {
                        toolDeployment {}
                        pluginDeployment {}
                    }
                }
            }
        }
        catch (InvalidUserDataException expected) {
            assertThat(expected.message, equalTo('Agent plugin cannot be configured for plugin deployment and tool deployment'))
        }
    }

    @Test
    void filePluginDescriptor() {
        project.teamcity {
            agent {
                descriptor = project.file('test-teamcity-plugin.xml')
            }
        }

        assertThat(extension.agent.descriptor, isA(File))
        assertThat(extension.agent.descriptor.getPath(), endsWith("test-teamcity-plugin.xml"))
    }

    @Test
    void agentPluginTasks() {
        project.teamcity {
            agent {
                descriptor {}
            }
        }

        assertNotNull(project.tasks.findByName('generateAgentDescriptor'))
        assertNotNull(project.tasks.findByName('processAgentDescriptor'))
        assertNotNull(project.tasks.findByName('agentPlugin'))
    }

    @Test
    void agentPluginTasksWithFileDescriptor() {
        project.teamcity {
            agent {
                descriptor = project.file('test-teamcity-plugin')
            }
        }

        assertNotNull(project.tasks.findByName('generateAgentDescriptor'))
        assertNotNull(project.tasks.findByName('processAgentDescriptor'))
        assertNotNull(project.tasks.findByName('agentPlugin'))
    }

    @Test
    void 'apply configures generate agent descriptor task'() {
        project.teamcity {
            agent {
                descriptor {}
            }
        }
        GenerateAgentPluginDescriptor task = (GenerateAgentPluginDescriptor) project.tasks.findByName('generateAgentDescriptor')

        assertThat(task.version.get(), equalTo('9.0'))
        assertThat(task.descriptor.get(), isA(AgentPluginDescriptor))
        assertThat(normalizePath(task.destination), endsWith('build/descriptor/agent/teamcity-plugin.xml'))
    }

    @Test
    void 'generator task outputs warning about dependencies not being supported for versions before 9_0'() {
        project.teamcity {
            version = '8.1'
            agent {
                descriptor {
                    dependencies {
                        plugin 'plugin-name'
                    }
                }
            }
        }
        projectDir.newFolder('build', 'descriptor', 'agent')

        GenerateAgentPluginDescriptor task = (GenerateAgentPluginDescriptor) project.tasks.findByName('generateAgentDescriptor')
        task.generateDescriptor()

        String output = outputEventListener.toString()
        assertThat(output, containsString('Plugin descriptor does not support dependencies for version 8.1'))
    }

    @Test
    void 'generator task outputs descriptor encoded in UTF-8'() {
        project.teamcity {
            version = '2018.1'
            agent {
                descriptor {
                    dependencies {
                        plugin 'plugin-name-àéîöū'
                    }
                }
            }
        }
        File outputDir = projectDir.newFolder('build', 'descriptor', 'agent')

        GenerateAgentPluginDescriptor task = (GenerateAgentPluginDescriptor) project.tasks.findByName('generateAgentDescriptor')
        task.generateDescriptor()

        File descriptorFile = new File(outputDir, 'teamcity-plugin.xml')
        String contents = new String(descriptorFile.bytes, 'UTF-8')
        assertThat(contents, containsString('plugin-name-àéîöū'))
    }

    @Test
    void agentPluginDescriptorReplacementTokens() {
        project.teamcity {
            agent {
                descriptor = project.file('test-teamcity-plugin')
                tokens VERSION: '1.2.3', VENDOR: 'rodm'
                tokens BUILD_NUMBER: '123'
            }
        }

        assertThat(extension.agent.tokens, hasEntry('VERSION', '1.2.3'))
        assertThat(extension.agent.tokens, hasEntry('VENDOR', 'rodm'))
        assertThat(extension.agent.tokens, hasEntry('BUILD_NUMBER', '123'))
    }

    @Test
    void agentPluginWithAdditionalFiles() {
        project.teamcity {
            agent {
                files {
                }
            }
        }

        assertThat(extension.agent.files.children.size, is(1))
    }

    @Test
    void deprecatedDescriptorCreationForAgentProjectType() {
        project.teamcity {
            descriptor {
            }
        }

        assertThat(extension.agent.descriptor, isA(AgentPluginDescriptor))
        assertThat(outputEventListener.toString(), containsString('descriptor property is deprecated'))
    }

    @Test
    void deprecatedDescriptorAssignmentForAgentProjectType() {
        project.teamcity {
            descriptor = project.file('teamcity-plugin.xml')
        }

        assertThat(extension.agent.descriptor, isA(File))
        assertThat(outputEventListener.toString(), containsString('descriptor property is deprecated'))
    }

    @Test
    void deprecatedAdditionalFilesForAgentPlugin() {
        project.teamcity {
            files {
            }
        }

        assertThat(extension.agent.files.children.size, is(1))
        assertThat(outputEventListener.toString(), containsString('files property is deprecated'))
    }

    @Test
    void deprecatedTokensForAgentPlugin() {
        project.teamcity {
            tokens VERSION: project.version
        }

        assertThat(extension.agent.tokens.size(), is(1))
        assertThat(outputEventListener.toString(), containsString('tokens property is deprecated'))
    }

    @Test
    void deprecatedTokensAssignmentForAgentPlugin() {
        project.teamcity {
            tokens = [VERSION: project.version]
        }

        assertThat(extension.agent.tokens.size(), is(1))
        assertThat(outputEventListener.toString(), containsString('tokens property is deprecated'))
    }

    @Test
    void configuringServerWithOnlyAgentPluginFails() {
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

    @Test
    void 'apply adds agent-api to the provided configuration'() {
        project.apply plugin: 'java'
        project.apply plugin: 'com.github.rodm.teamcity-agent'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('provided')
        assertThat(configuration, hasDependency('org.jetbrains.teamcity', 'agent-api', '9.0'))
    }

    @Test
    void 'apply adds tests-support to the testImplementation configuration'() {
        project.apply plugin: 'java'
        project.apply plugin: 'com.github.rodm.teamcity-agent'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('testImplementation')
        assertThat(configuration, hasDependency('org.jetbrains.teamcity', 'tests-support', '9.0'))
    }

    @Test
    void 'agent-side plugin artifact is published to the plugin configuration'() {
        project.apply plugin: 'java'
        project.apply plugin: 'com.github.rodm.teamcity-agent'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('plugin')
        assertThat(configuration.artifacts, hasSize(1))
        assertThat(normalizePath(configuration.artifacts[0].file), endsWith('/build/distributions/test.zip'))
    }

    @Test
    void 'apply configures archive name using defaults'() {
        project.version = '1.2.3'
        project.teamcity {
            agent {
                descriptor {}
            }
        }

        project.evaluate()

        Zip agentPlugin = (Zip) project.tasks.findByPath(':agentPlugin')
        assertThat(agentPlugin.archiveName, equalTo('test-1.2.3.zip'))
    }

    @Test
    void 'apply configures archive name using configuration value'() {
        project.version = '1.2.3'
        project.teamcity {
            agent {
                archiveName = 'agent-plugin.zip'
                descriptor {}
            }
        }

        project.evaluate()

        Zip agentPlugin = (Zip) project.tasks.findByPath(':agentPlugin')
        assertThat(agentPlugin.archiveName, equalTo('agent-plugin.zip'))
    }

    @Test
    void 'archive name is appended with zip extension if missing'() {
        project.version = '1.2.3'
        project.teamcity {
            agent {
                archiveName = 'agent-plugin'
                descriptor {}
            }
        }

        project.evaluate()

        Zip agentPlugin = (Zip) project.tasks.findByPath(':agentPlugin')
        assertThat(agentPlugin.archiveName, equalTo('agent-plugin.zip'))
    }

    @Test
    void 'archive name configured using archivesBaseName property'() {
        project.archivesBaseName = 'my-plugin'
        project.teamcity {
            agent {
                descriptor {}
            }
        }

        project.evaluate()

        Zip agentPlugin = (Zip) project.tasks.findByPath(':agentPlugin')
        assertThat(agentPlugin.archiveName, equalTo('my-plugin.zip'))
    }
}
