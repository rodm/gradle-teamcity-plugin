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
import org.gradle.api.tasks.bundling.Zip
import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.endsWith
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.isA
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat
import static org.junit.Assert.fail

class ServerConfigurationTest extends ConfigurationTestCase {

    @Before
    void applyPlugin() {
        project.apply plugin: 'com.github.rodm.teamcity-server'
        extension = project.getExtensions().getByType(TeamCityPluginExtension)
    }

    @Test
    void buildScriptPluginDescriptor() {
        project.teamcity {
            server {
                descriptor {
                    name = 'test plugin'
                }
            }
        }

        assertThat(extension.server.descriptor.getName(), equalTo('test plugin'))
    }

    @Test
    void filePluginDescriptor() {
        project.teamcity {
            server {
                descriptor = project.file('test-teamcity-plugin.xml')
            }
        }

        assertThat(extension.server.descriptor, isA(File))
        assertThat(extension.server.descriptor.getPath(), endsWith("test-teamcity-plugin.xml"))
    }

    @Test
    void serverPluginTasks() {
        project.teamcity {
            server {
                descriptor {}
            }
        }

        assertNotNull(project.tasks.findByName('processServerDescriptor'))
        assertNotNull(project.tasks.findByName('generateServerDescriptor'))
        assertNotNull(project.tasks.findByName('serverPlugin'))
    }

    @Test
    void serverPluginTasksWithFileDescriptor() {
        project.teamcity {
            server {
                descriptor = project.file('test-teamcity-plugin')
            }
        }

        assertNotNull(project.tasks.findByName('processServerDescriptor'))
        assertNotNull(project.tasks.findByName('generateServerDescriptor'))
        assertNotNull(project.tasks.findByName('serverPlugin'))
    }

    @Test
    void agentPluginDescriptorReplacementTokens() {
        project.teamcity {
            server {
                descriptor = project.file('test-teamcity-plugin')
                tokens VERSION: '1.2.3', VENDOR: 'rodm'
                tokens BUILD_NUMBER: '123'
            }
        }

        assertThat(extension.server.tokens, hasEntry('VERSION', '1.2.3'))
        assertThat(extension.server.tokens, hasEntry('VENDOR', 'rodm'))
        assertThat(extension.server.tokens, hasEntry('BUILD_NUMBER', '123'))
    }

    @Test
    void serverPluginWithAdditionalFiles() {
        project.teamcity {
            server {
                files {
                }
            }
        }

        assertThat(extension.server.files.children.size, is(1))
    }

    @Test
    void deprecatedEnvironmentsConfiguration() {
        project.teamcity {
            version = '8.1.5'
            server {
                downloadsDir = '/tmp'
                baseDownloadUrl = 'http://repository/'
                baseDataDir = '/tmp/data'
                baseHomeDir = '/tmp/servers'
                environments {
                    teamcity {
                    }
                }
            }
        }

        String output = outputEventListener.toString()
        assertThat(output, containsString('downloadsDir property in server configuration is deprecated'))
        assertThat(output, containsString('baseDownloadUrl property in server configuration is deprecated'))
        assertThat(output, containsString('baseDataDir property in server configuration is deprecated'))
        assertThat(output, containsString('baseHomeDir property in server configuration is deprecated'))
        assertThat(output, containsString('environments configuration in server configuration is deprecated'))
    }

    @Test
    void deprecatedDescriptorCreationForServerProjectType() {
        project.teamcity {
            descriptor {
            }
        }

        assertThat(extension.server.descriptor, isA(ServerPluginDescriptor))
        assertThat(outputEventListener.toString(), containsString('descriptor property is deprecated'))
    }

    @Test
    void deprecatedDescriptorAssignmentForServerProjectType() {
        project.teamcity {
            descriptor = project.file('teamcity-plugin.xml')
        }

        assertThat(extension.server.descriptor, isA(File))
        assertThat(outputEventListener.toString(), containsString('descriptor property is deprecated'))
    }

    @Test
    void deprecatedAdditionalFilesForServerPlugin() {
        project.teamcity {
            files {
            }
        }

        assertThat(extension.server.files.children.size, is(1))
        assertThat(outputEventListener.toString(), containsString('files property is deprecated'))
    }

    @Test
    void deprecatedTokensForServerPlugin() {
        project.teamcity {
            tokens VERSION: project.version
        }

        assertThat(extension.server.tokens.size(), is(1))
        assertThat(outputEventListener.toString(), containsString('tokens property is deprecated'))
    }

    @Test
    void deprecatedTokensAssignmentForServerPlugin() {
        project.teamcity {
            tokens = [VERSION: project.version]
        }

        assertThat(extension.server.tokens.size(), is(1))
        assertThat(outputEventListener.toString(), containsString('tokens property is deprecated'))
    }

    @Test
    void configuringAgentWithOnlyServerPluginFails() {
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
    void 'apply configures archive name using defaults'() {
        project.version = '1.2.3'
        project.teamcity {
            server {
                descriptor {}
            }
        }

        project.evaluate()

        Zip serverPlugin = (Zip) project.tasks.findByPath(':serverPlugin')
        assertThat(serverPlugin.archiveName, equalTo('test-1.2.3.zip'))
    }

    @Test
    void 'apply configures archive name using configuration value'() {
        project.version = '1.2.3'
        project.teamcity {
            server {
                archiveName = 'server-plugin.zip'
                descriptor {}
            }
        }

        project.evaluate()

        Zip serverPlugin = (Zip) project.tasks.findByPath(':serverPlugin')
        assertThat(serverPlugin.archiveName, equalTo('server-plugin.zip'))
    }

    @Test
    void 'archive name is appended with zip extension if missing'() {
        project.teamcity {
            server {
                archiveName = 'server-plugin'
                descriptor {}
            }
        }

        project.evaluate()

        Zip serverPlugin = (Zip) project.tasks.findByPath(':serverPlugin')
        assertThat(serverPlugin.archiveName, equalTo('server-plugin.zip'))
    }
}
