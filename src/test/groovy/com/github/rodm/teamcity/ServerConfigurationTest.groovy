/*
 * Copyright 2015 Rod MacKenzie
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

import com.github.rodm.teamcity.internal.PluginDescriptorContentsValidationAction
import com.github.rodm.teamcity.internal.PluginDescriptorValidationAction
import com.github.rodm.teamcity.tasks.GenerateServerPluginDescriptor
import com.github.rodm.teamcity.tasks.ProcessDescriptor
import com.github.rodm.teamcity.tasks.PublishTask
import com.github.rodm.teamcity.tasks.ServerPlugin
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.teamcity.TeamcityPlugin
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.intellij.pluginRepository.PluginUploader
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.github.rodm.teamcity.TestSupport.createDirectory
import static com.github.rodm.teamcity.TestSupport.createFile
import static com.github.rodm.teamcity.TestSupport.normalizePath
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.endsWith
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.isA
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.nullValue
import static org.hamcrest.Matchers.not
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.fail
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.ArgumentMatchers.isNull
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class ServerConfigurationTest extends ConfigurationTestCase {

    @BeforeEach
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

        assertThat(extension.server.descriptor, isA(ServerPluginDescriptor))
        assertThat(extension.server.descriptor.getName(), equalTo('test plugin'))
        assertThat(extension.server.descriptorFile.isPresent(), is(false))
    }

    @Test
    void filePluginDescriptor() {
        project.teamcity {
            server {
                descriptor = project.file('test-teamcity-plugin.xml')
            }
        }

        assertThat(extension.server.descriptor, is(nullValue()))
        assertThat(extension.server.descriptorFile.isPresent(), is(true))
        assertThat(extension.server.descriptorFile.get().asFile.getPath(), endsWith("test-teamcity-plugin.xml"))
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
    void 'apply configures generate server descriptor task'() {
        project.teamcity {
            server {
                descriptor {}
            }
        }
        GenerateServerPluginDescriptor task = (GenerateServerPluginDescriptor) project.tasks.findByName('generateServerDescriptor')

        assertThat(task.version.get(), equalTo('9.0'))
        assertThat(task.descriptor.get(), isA(ServerPluginDescriptor))
        assertThat(normalizePath(task.destination), endsWith('build/descriptor/server/teamcity-plugin.xml'))
    }

    @Test
    void 'generator task outputs warning about dependencies not being supported for versions before 9_0'() {
        project.teamcity {
            version = '8.1'
            server {
                descriptor {
                    dependencies {
                        plugin 'plugin-name'
                    }
                }
            }
        }
        createDirectory(projectDir.resolve('build/descriptor/server'))

        GenerateServerPluginDescriptor task = (GenerateServerPluginDescriptor) project.tasks.findByName('generateServerDescriptor')
        task.generateDescriptor()

        String output = outputEventListener.toString()
        assertThat(output, containsString('Plugin descriptor does not support dependencies for version 8.1'))
    }

    @Test
    void 'generator task outputs warning about allowRuntimeReload not being supported for versions before 2018_2'() {
        project.teamcity {
            version = '2018.1'
            server {
                descriptor {
                    allowRuntimeReload = true
                }
            }
        }
        createDirectory(projectDir.resolve('build/descriptor/server'))

        GenerateServerPluginDescriptor task = (GenerateServerPluginDescriptor) project.tasks.findByName('generateServerDescriptor')
        task.generateDescriptor()

        String output = outputEventListener.toString()
        assertThat(output, containsString('Plugin descriptor does not support allowRuntimeReload for version 2018.1'))
    }

    @Test
    void 'generator task outputs warning about nodeResponsibilitiesAware not being supported for versions before 2020_1'() {
        project.teamcity {
            version = '2019.2'
            server {
                descriptor {
                    nodeResponsibilitiesAware = true
                }
            }
        }
        createDirectory(projectDir.resolve('build/descriptor/server'))

        GenerateServerPluginDescriptor task = (GenerateServerPluginDescriptor) project.tasks.findByName('generateServerDescriptor')
        task.generateDescriptor()

        String output = outputEventListener.toString()
        assertThat(output, containsString('Plugin descriptor does not support nodeResponsibilitiesAware for version 2019.2'))
    }

    @Test
    void 'generator task outputs descriptor encoded in UTF-8'() {
        project.teamcity {
            version = '2018.1'
            server {
                descriptor {
                    description = 'àéîöū'
                }
            }
        }
        File outputDir = createDirectory(projectDir.resolve('build/descriptor/server'))

        GenerateServerPluginDescriptor task = (GenerateServerPluginDescriptor) project.tasks.findByName('generateServerDescriptor')
        task.generateDescriptor()

        File descriptorFile = new File(outputDir, 'teamcity-plugin.xml')
        String contents = new String(descriptorFile.bytes, 'UTF-8')
        assertThat(contents, containsString('àéîöū'))
    }

    @Test
    void 'allow server descriptor configuration to be created from multiple configuration blocks'() {
        project.teamcity {
            server {
                descriptor {
                    name = 'plugin-name'
                }
            }
        }
        project.teamcity {
            server {
                descriptor {
                    description = 'plugin description'
                }
            }
        }

        assertThat(extension.server.descriptor.getName(), equalTo('plugin-name'))
        assertThat(extension.server.descriptor.getDescription(), equalTo('plugin description'))
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
    void 'process descriptor replaces tokens in descriptor file'() {
        project.teamcity {
            server {
                descriptor = project.file('teamcity-plugin.xml')
                tokens VERSION: '1.2.3', VENDOR_NAME: 'rodm'
                tokens BUILD_NUMBER: '456'
            }
        }
        File descriptorFile = createFile(projectDir.resolve('teamcity-plugin.xml'))
        descriptorFile << """<?xml version="1.0" encoding="UTF-8"?>
            <teamcity-plugin>
                <info>
                    <name>test-plugin</name>
                    <display-name>test-plugin</display-name>
                    <description>plugin description. Build: @BUILD_NUMBER@ </description>
                    <version>@VERSION@</version>
                    <vendor>
                        <name>@VENDOR_NAME@</name>
                        <url>http://example.com</url>
                    </vendor>
                </info>
                <deployment use-separate-classloader="false"/>
            </teamcity-plugin>
        """

        File outputDir = createDirectory(projectDir.resolve('build/descriptor/server'))
        ProcessDescriptor task = (ProcessDescriptor) project.tasks.findByName('processServerDescriptor')
        task.process()

        File outputFile = new File(outputDir, 'teamcity-plugin.xml')
        String contents = new String(outputFile.bytes, 'UTF-8')
        assertThat(contents, not(containsString('@')))
        assertThat(contents, containsString('1.2.3'))
        assertThat(contents, containsString('rodm'))
        assertThat(contents, containsString('456'))
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

        assertThat(extension.server.descriptorFile.isPresent(), is(true))
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
        assertThat(serverPlugin.archiveFileName.get(), equalTo('test-1.2.3.zip'))
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
        assertThat(serverPlugin.archiveFileName.get(), equalTo('server-plugin.zip'))
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
        assertThat(serverPlugin.archiveFileName.get(), equalTo('server-plugin.zip'))
    }

    @Test
    void 'archive name configured using archivesBaseName property'() {
        project.archivesBaseName = 'my-plugin'
        project.teamcity {
            server {
                descriptor {}
            }
        }

        project.evaluate()

        Zip serverPlugin = (Zip) project.tasks.findByPath(':serverPlugin')
        assertThat(serverPlugin.archiveFileName.get(), equalTo('my-plugin.zip'))
    }

    @Test
    void 'applying server plugin configures validation actions on all ServerPlugin tasks'() {
        project.tasks.create('alternativeServerPlugin', ServerPlugin)
        project.evaluate()

        ServerPlugin serverPlugin = project.tasks.getByName('alternativeServerPlugin') as ServerPlugin
        List<String> taskActionClassNames = serverPlugin.taskActions.collect { it.action.getClass().name }
        assertThat(taskActionClassNames, hasItem(PluginDescriptorValidationAction.name))
        assertThat(taskActionClassNames, hasItem(PluginDescriptorContentsValidationAction.name))
    }

    @Test
    void 'publish task is configured with a Hub token'() {
        project.teamcity {
            server {
                publish {
                    token = 'token'
                }
            }
        }

        project.evaluate()

        PublishTask publishPlugin = (PublishTask) project.tasks.findByPath(':publishPlugin')
        assertThat(publishPlugin.token.get(), equalTo('token'))
    }

    @Test
    void 'publish task is configured with output of serverPlugin task'() {
        project.teamcity {
            server {
                publish {
                    token = 'token'
                }
            }
        }

        project.evaluate()

        Zip serverPlugin = (Zip) project.tasks.findByPath(':serverPlugin')
        PublishTask publishPlugin = (PublishTask) project.tasks.findByPath(':publishPlugin')
        assertThat(publishPlugin.distributionFile.get(), equalTo(serverPlugin.archiveFile.get()))
    }

    @Test
    void 'publish task is configured with change notes'() {
        project.teamcity {
            server {
                publish {
                    notes = 'change notes'
                }
            }
        }

        project.evaluate()

        PublishTask publishPlugin = (PublishTask) project.tasks.findByPath(':publishPlugin')
        assertThat(publishPlugin.notes.get(), equalTo('change notes'))
    }

    @Test
    void 'allow server publish configuration to be created from multiple configuration blocks'() {
        project.teamcity {
            server {
                publish {
                    channels = ['Beta', 'Test']
                }
            }
        }
        project.teamcity {
            server {
                publish {
                    token = 'token'
                }
            }
        }

        project.evaluate()

        PublishTask publishPlugin = (PublishTask) project.tasks.findByPath(':publishPlugin')
        assertThat(publishPlugin.channels.get(), equalTo(['Beta', 'Test']))
        assertThat(publishPlugin.token.get(), equalTo('token'))
    }

    @Test
    void 'publish task is configured to publish to default channel'() {
        project.teamcity {
            server {
                publish {}
            }
        }

        project.evaluate()

        PublishTask publishPlugin = (PublishTask) project.tasks.findByPath(':publishPlugin')
        assertThat(publishPlugin.channels.get(), equalTo(['default']))
    }

    @Test
    void 'publish task is configured to publish to a list of channels'() {
        project.teamcity {
            server {
                publish {
                    channels = ['Beta', 'Test']
                }
            }
        }

        project.evaluate()

        PublishTask publishPlugin = (PublishTask) project.tasks.findByPath(':publishPlugin')
        assertThat(publishPlugin.channels.get(), equalTo(['Beta', 'Test']))
    }

    @Test
    void 'publish task is only created with publish configuration'() {
        project.teamcity {
            server {
            }
        }

        project.evaluate()

        PublishTask publishPlugin = (PublishTask) project.tasks.findByPath(':publishPlugin')
        assertThat(publishPlugin, is(nullValue()))
    }

    @Test
    void 'publish task logs successful uploaded to default channel'() {
        def pluginFile = project.file('test-plugin.zip')
        PublishTask task = project.tasks.create('publish', MockPublishTask) {
            distributionFile.set(pluginFile)
            channels.set(['default'])
        }
        task.publishPlugin()

        String output = outputEventListener.toString()
        assertThat(output, containsString("Uploading plugin TestPluginId from ${pluginFile.absolutePath} to https://plugins.jetbrains.com"))
        assertThat(output, containsString('Uploading plugin to default channel'))
        assertThat(output, containsString('Uploaded successfully'))
    }

    @Test
    void 'publish task uploads plugin to default channel'() {
        def pluginFile = project.file('test-plugin.zip')
        MockPublishTask task = project.tasks.create('publish', MockPublishTask) {
            distributionFile.set(pluginFile)
            channels.set(['default'])
        }
        task.publishPlugin()

        verify(task.uploader).uploadPlugin(eq('TestPluginId'), eq(pluginFile), eq(''), isNull() as String)
    }

    @Test
    void 'publish task uploads plugin to custom channel'() {
        def pluginFile = project.file('test-plugin.zip')
        MockPublishTask task = project.tasks.create('publish', MockPublishTask) {
            distributionFile.set(pluginFile)
            channels.set(['Beta'])
        }
        task.publishPlugin()

        verify(task.uploader).uploadPlugin(eq('TestPluginId'), eq(pluginFile), eq('Beta'), isNull() as String)
    }

    @Test
    void 'publish task uploads plugin with change notes'() {
        def pluginFile = project.file('test-plugin.zip')
        MockPublishTask task = project.tasks.create('publish', MockPublishTask) {
            distributionFile.set(pluginFile)
            channels.set(['default'])
            notes.set('change notes')
        }
        task.publishPlugin()

        verify(task.uploader).uploadPlugin(eq('TestPluginId'), eq(pluginFile), eq(''), eq('change notes'))
    }

    @Test
    void 'publish task throws exception if channels list is empty'() {
        PublishTask task = project.tasks.create('publish', MockPublishTask) {
            channels.set([])
        }
        try {
            task.publishPlugin()
            fail('Should fail when channels list is empty')
        }
        catch (GradleException expected) {
            assertThat(expected.message, equalTo('Channels list can\'t be empty'))
        }
    }

    @Test
    void 'publish task logs upload to multiple channels'() {
        def pluginFile = project.file('test-plugin.zip')
        PublishTask task = project.tasks.create('publish', MockPublishTask) {
            distributionFile.set(pluginFile)
            channels.set(['Beta', 'Test'])
        }
        task.publishPlugin()

        String output = outputEventListener.toString()
        assertThat(output, containsString('Uploading plugin to Beta channel'))
        assertThat(output, containsString('Uploading plugin to Test channel'))
    }

    @Test
    void 'publish task throws exception for invalid plugin'() {
        def pluginFile = project.file('test-plugin.zip')
        MockPublishTask task = project.tasks.create('publish', MockPublishTask) {
            distributionFile.set(pluginFile)
            channels.set(['default'])
        }
        task.result = mock(PluginCreationFail)
        try {
            task.publishPlugin()
            fail('Should throw exception on plugin creation failure')
        }
        catch (GradleException expected) {
            assertThat(expected.message, containsString('Cannot upload plugin.'))
        }
    }

    static class MockPublishTask extends PublishTask {
        PluginCreationResult result
        PluginUploader uploader

        MockPublishTask() {
            TeamcityPlugin plugin = mock(TeamcityPlugin)
            when(plugin.getPluginId()).thenReturn('TestPluginId')
            result = mock(PluginCreationSuccess)
            when(result.getPlugin()).thenReturn(plugin)
            uploader = mock(PluginUploader)
        }

        @Override
        protected void publishPlugin() {
            super.publishPlugin()
        }

        @Override
        PluginCreationResult<TeamcityPlugin> createPlugin(File distributionFile) {
            return result
        }

        @Override
        PluginUploader createRepositoryUploader() {
            return uploader
        }
    }
}
