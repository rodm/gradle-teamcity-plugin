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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import javax.xml.XMLConstants

import static XPathMatcher.hasXPath
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.not

class AgentDescriptorGeneratorTest {

    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder()

    private Project project

    private StringWriter writer

    private AgentPluginDescriptor descriptor

    private AgentPluginDescriptorGenerator generator

    @Before
    void setup() {
        project = ProjectBuilder.builder()
                .withProjectDir(projectDir.root)
                .withName('test-plugin')
                .build()
        project.apply plugin: 'com.github.rodm.teamcity-agent'
        writer = new StringWriter()
        descriptor = project.extensions.create('descriptor', AgentPluginDescriptor)
        descriptor.init()
        generator = new AgentPluginDescriptorGenerator(descriptor)
    }

    @Test
    void writesRootNode() {
        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('/teamcity-agent-plugin'))
    }

    @Test
    void 'descriptor schema location'() {
        generator.writeTo(writer)

        def path = '/teamcity-agent-plugin/@xsi:noNamespaceSchemaLocation'
        def schemaLocation = 'urn:schemas-jetbrains-com:teamcity-agent-plugin-v1-xml'
        def context = ['xsi': XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI]
        def matcher = hasXPath(path, equalTo(schemaLocation)).withNamespaceContext(context)
        assertThat(writer.toString(), matcher)
    }

    @Test
    void writesPluginDeployment() {
        descriptor.pluginDeployment {}

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('/teamcity-agent-plugin/plugin-deployment'))
        assertThat(writer.toString(), not(hasXPath('/teamcity-agent-plugin/plugin-deployment/@use-separate-classloader')))
    }

    @Test
    void writeOptionalUseSeparateClassloader() {
        descriptor.pluginDeployment {
            useSeparateClassloader = true
        }

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//plugin-deployment/@use-separate-classloader', equalTo('true')))
    }

    @Test
    void writeOptionalUseSeparateClassloaderWhenFalse() {
        descriptor.pluginDeployment {
            useSeparateClassloader = false
        }

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//plugin-deployment/@use-separate-classloader', equalTo('false')))
    }

    @Test
    void writePluginDeploymentExecutableFiles() {
        descriptor.pluginDeployment {
            executableFiles {
                include 'file1'
                include 'file2'
            }
        }

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//plugin-deployment/layout/executable-files'))
        assertThat(writer.toString(), hasXPath('//executable-files/include[1]/@name', equalTo('file1')))
        assertThat(writer.toString(), hasXPath('//executable-files/include[2]/@name', equalTo('file2')))
    }

    @Test
    void writePluginDeploymentExecutableFilesOnlyIfSpecified() {
        descriptor.pluginDeployment {}

        generator.writeTo(writer)

        assertThat(writer.toString(), not(hasXPath('//plugin-deployment/layout')))
        assertThat(writer.toString(), not(hasXPath('//plugin-deployment/layout/executable-files')))
    }

    @Test
    void writesToolDeployment() {
        descriptor.toolDeployment {}

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('/teamcity-agent-plugin/tool-deployment'))
    }

    @Test
    void writeToolDeploymentExecutableFiles() {
        descriptor.toolDeployment {
            executableFiles {
                include 'file1'
                include 'file2'
            }
        }

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//tool-deployment/layout/executable-files'))
        assertThat(writer.toString(), hasXPath('//executable-files/include[1]/@name', equalTo('file1')))
        assertThat(writer.toString(), hasXPath('//executable-files/include[2]/@name', equalTo('file2')))
    }

    @Test
    void writeToolDeploymentExecutableFilesOnlyIfSpecified() {
        descriptor.pluginDeployment {}

        generator.writeTo(writer)

        assertThat(writer.toString(), not(hasXPath('//plugin-deployment/layout')))
        assertThat(writer.toString(), not(hasXPath('//plugin-deployment/layout/executable-files')))
    }

    @Test
    void writePluginDependency() {
        descriptor.dependencies {
            plugin 'plugin-name'
        }

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//dependencies/plugin/@name', equalTo('plugin-name')))
    }

    @Test
    void writeToolDependency() {
        descriptor.dependencies {
            tool 'tool-name'
        }

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//dependencies/tool/@name', equalTo('tool-name')))
    }
}
