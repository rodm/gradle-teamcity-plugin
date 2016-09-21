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
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.xmlunit.matchers.EvaluateXPathMatcher
import org.xmlunit.matchers.HasXPathMatcher

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.not

public class AgentDescriptorGeneratorTest {

    private Project project

    private StringWriter writer

    private AgentPluginDescriptor descriptor

    private AgentPluginDescriptorGenerator generator

    private static HasXPathMatcher hasXPath(String xPath) {
        return HasXPathMatcher.hasXPath(xPath)
    }

    private static EvaluateXPathMatcher hasXPath(String xPath, Matcher<String> valueMatcher) {
        return EvaluateXPathMatcher.hasXPath(xPath, valueMatcher)
    }

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

        assertThat(writer.toString(), hasXPath('/teamcity-agent-plugin'))
    }

    @Test
    public void writesPluginDeployment() {
        descriptor.deployment = new PluginDeployment()

        generator.writeTo(writer);

        assertThat(writer.toString(), hasXPath('/teamcity-agent-plugin/plugin-deployment'))
        assertThat(writer.toString(), not(hasXPath('/teamcity-agent-plugin/plugin-deployment/@use-separate-classloader')))
    }

    @Test
    public void writeOptionalUseSeparateClassloader() {
        descriptor.deployment = new PluginDeployment()
        descriptor.deployment.useSeparateClassloader = true

        generator.writeTo(writer);

        assertThat(writer.toString(), hasXPath('//plugin-deployment/@use-separate-classloader', equalTo('true')))
    }

    @Test
    public void writeOptionalUseSeparateClassloaderWhenFalse() {
        descriptor.deployment = new PluginDeployment()
        descriptor.deployment.useSeparateClassloader = false

        generator.writeTo(writer);

        assertThat(writer.toString(), hasXPath('//plugin-deployment/@use-separate-classloader', equalTo('false')))
    }

    @Test
    public void writePluginDeploymentExecutableFiles() {
        descriptor.deployment = new PluginDeployment()
        descriptor.deployment.executableFiles {
            include 'file1'
            include 'file2'
        }

        generator.writeTo(writer);

        assertThat(writer.toString(), hasXPath('//plugin-deployment/layout/executable-files'))
        assertThat(writer.toString(), hasXPath('//executable-files/include[1]/@name', equalTo('file1')))
        assertThat(writer.toString(), hasXPath('//executable-files/include[2]/@name', equalTo('file2')))
    }

    @Test
    public void writePluginDeploymentExecutableFilesOnlyIfSpecified() {
        descriptor.deployment = new PluginDeployment()

        generator.writeTo(writer);

        assertThat(writer.toString(), not(hasXPath('//plugin-deployment/layout')))
        assertThat(writer.toString(), not(hasXPath('//plugin-deployment/layout/executable-files')))
    }

    @Test
    public void writesToolDeployment() {
        descriptor.deployment = new ToolDeployment()

        generator.writeTo(writer);

        assertThat(writer.toString(), hasXPath('/teamcity-agent-plugin/tool-deployment'))
    }

    @Test
    public void writeToolDeploymentExecutableFiles() {
        descriptor.deployment = new ToolDeployment()
        descriptor.deployment.executableFiles {
            include 'file1'
            include 'file2'
        }

        generator.writeTo(writer);

        assertThat(writer.toString(), hasXPath('//tool-deployment/layout/executable-files'))
        assertThat(writer.toString(), hasXPath('//executable-files/include[1]/@name', equalTo('file1')))
        assertThat(writer.toString(), hasXPath('//executable-files/include[2]/@name', equalTo('file2')))
    }

    @Test
    public void writeToolDeploymentExecutableFilesOnlyIfSpecified() {
        descriptor.deployment = new PluginDeployment()

        generator.writeTo(writer);

        assertThat(writer.toString(), not(hasXPath('//plugin-deployment/layout')))
        assertThat(writer.toString(), not(hasXPath('//plugin-deployment/layout/executable-files')))
    }

    @Test
    public void writePluginDependency() {
        descriptor.dependencies {
            plugin 'plugin-name'
        }

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//dependencies/plugin/@name', equalTo('plugin-name')))
    }

    @Test
    public void writeToolDependency() {
        descriptor.dependencies {
            tool 'tool-name'
        }

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//dependencies/tool/@name', equalTo('tool-name')))
    }
}
