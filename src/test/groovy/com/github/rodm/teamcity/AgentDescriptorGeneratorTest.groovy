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

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists

public class AgentDescriptorGeneratorTest {

    private Project project

    private StringWriter writer

    private AgentPluginDescriptor descriptor

    private AgentPluginDescriptorGenerator generator

    @Before
    public void setup() {
        project = ProjectBuilder.builder()
                .withName('test-plugin')
                .build()
        project.apply plugin: 'com.github.rodm.teamcity-agent'
        writer = new StringWriter()
        descriptor = new AgentPluginDescriptor()
        generator = new AgentPluginDescriptorGenerator(descriptor)
    }

    @Test
    public void writesRootNode() {
        generator.writeTo(writer)

        assertXpathExists("/teamcity-agent-plugin", writer.toString())
    }

    @Test
    public void writesPluginDeployment() {
        descriptor.deployment = new PluginDeployment()

        generator.writeTo(writer);

        assertXpathExists("/teamcity-agent-plugin/plugin-deployment", writer.toString())
        assertXpathNotExists("/teamcity-agent-plugin/plugin-deployment/@use-separate-classloader", writer.toString())
    }

    @Test
    public void writeOptionalUseSeparateClassloader() {
        descriptor.deployment = new PluginDeployment()
        descriptor.deployment.useSeparateClassloader = true

        generator.writeTo(writer);

        assertXpathEvaluatesTo('true', "//plugin-deployment/@use-separate-classloader", writer.toString())
    }

    @Test
    public void writeOptionalUseSeparateClassloaderWhenFalse() {
        descriptor.deployment = new PluginDeployment()
        descriptor.deployment.useSeparateClassloader = false

        generator.writeTo(writer);

        assertXpathEvaluatesTo('false', "//plugin-deployment/@use-separate-classloader", writer.toString())
    }

    @Test
    public void writePluginDeploymentExecutableFiles() {
        descriptor.deployment = new PluginDeployment()
        descriptor.deployment.executableFiles {
            include 'file1'
            include 'file2'
        }

        generator.writeTo(writer);

        assertXpathExists("//plugin-deployment/layout/executable-files", writer.toString())
        assertXpathEvaluatesTo('file1', "//executable-files/include[1]/@name", writer.toString())
        assertXpathEvaluatesTo('file2', "//executable-files/include[2]/@name", writer.toString())
    }

    @Test
    public void writePluginDeploymentExecutableFilesOnlyIfSpecified() {
        descriptor.deployment = new PluginDeployment()

        generator.writeTo(writer);

        assertXpathNotExists("//plugin-deployment/layout", writer.toString())
        assertXpathNotExists("//plugin-deployment/layout/executable-files", writer.toString())
    }

    @Test
    public void writesToolDeployment() {
        descriptor.deployment = new ToolDeployment()

        generator.writeTo(writer);

        assertXpathExists("/teamcity-agent-plugin/tool-deployment", writer.toString())
    }

    @Test
    public void writeToolDeploymentExecutableFiles() {
        descriptor.deployment = new ToolDeployment()
        descriptor.deployment.executableFiles {
            include 'file1'
            include 'file2'
        }

        generator.writeTo(writer);

        assertXpathExists("//tool-deployment/layout/executable-files", writer.toString())
        assertXpathEvaluatesTo('file1', "//executable-files/include[1]/@name", writer.toString())
        assertXpathEvaluatesTo('file2', "//executable-files/include[2]/@name", writer.toString())
    }

    @Test
    public void writeToolDeploymentExecutableFilesOnlyIfSpecified() {
        descriptor.deployment = new PluginDeployment()

        generator.writeTo(writer);

        assertXpathNotExists("//plugin-deployment/layout", writer.toString())
        assertXpathNotExists("//plugin-deployment/layout/executable-files", writer.toString())
    }

    @Test
    public void writePluginDependency() {
        descriptor.dependencies {
            plugin 'plugin-name'
        }

        generator.writeTo(writer)

        assertXpathEvaluatesTo("plugin-name", "//dependencies/plugin/@name", writer.toString());
    }

    @Test
    public void writeToolDependency() {
        descriptor.dependencies {
            tool 'tool-name'
        }

        generator.writeTo(writer)

        assertXpathEvaluatesTo("tool-name", "//dependencies/tool/@name", writer.toString());
    }
}
