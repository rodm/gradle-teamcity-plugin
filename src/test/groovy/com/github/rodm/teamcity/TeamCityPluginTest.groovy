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
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.CoreMatchers.endsWith
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

public class TeamCityPluginTest {

    private Project project;

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.rodm.teamcity'
    }

    @Test
    public void applyJavaPlugin() {
        assertTrue(project.plugins.hasPlugin(JavaPlugin))
    }

    @Test
    public void addTeamCityPluginExtension() {
        assertTrue(project.extensions.getByName('teamcity') instanceof TeamCityPluginExtension)
    }

    @Test
    public void defaultVersion() {
        assertEquals('9.0', project.extensions.getByName('teamcity').version)
    }

    @Test
    public void specifiedVersion() {
        project.teamcity {
            version = '8.1.5'
        }
        assertEquals('8.1.5', project.extensions.getByName('teamcity').version)
    }

    @Test
    public void defaultDescriptorPath() {
        TeamCityPluginExtension extension = project.extensions.getByName('teamcity')

        assertThat(extension.descriptor.getPath(), endsWith("src/main/resources/teamcity-plugin.xml"))
    }
}
