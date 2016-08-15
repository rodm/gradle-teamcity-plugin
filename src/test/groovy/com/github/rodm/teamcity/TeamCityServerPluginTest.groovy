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
import org.gradle.api.plugins.BasePlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.CoreMatchers.notNullValue
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

public class TeamCityServerPluginTest {

    private Project project;

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    public void applyBasePlugin() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        assertTrue(project.plugins.hasPlugin(BasePlugin))
    }

    @Test
    public void addTeamCityPluginExtension() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        assertTrue(project.extensions.getByName('teamcity') instanceof TeamCityPluginExtension)
    }

    @Test
    public void defaultVersion() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        assertEquals('9.0', project.extensions.getByName('teamcity').version)
    }

    @Test
    public void specifiedVersion() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            version = '8.1.5'
        }
        assertEquals('8.1.5', project.extensions.getByName('teamcity').version)
    }

    @Test
    public void configurationsCreatedWithoutJavaPlugin() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        def configuration = project.configurations.getByName('agent')
        assertThat(configuration, notNullValue())
        assertThat(configuration.visible, is(false))
        assertThat(configuration.transitive, is(false))

        configuration = project.configurations.getByName('server')
        assertThat(configuration, notNullValue())
        assertThat(configuration.visible, is(false))
        assertThat(configuration.transitive, is(false))

        configuration = project.configurations.getByName('plugin')
        assertThat(configuration, notNullValue())
        assertThat(configuration.visible, is(false))
        assertThat(configuration.transitive, is(false))
    }

    @Test
    public void providedConfigurationNotCreatedWithoutJavaPlugin() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        def configurationNames = project.configurations.getNames()
        assertThat(configurationNames, not(hasItem('provided')))
    }

    @Test
    public void configurationsCreatedWithJavaPlugin() {
        project.apply plugin: 'java'
        project.apply plugin: 'com.github.rodm.teamcity-server'

        def configuration = project.configurations.getByName('agent')
        assertThat(configuration, notNullValue())

        configuration = project.configurations.getByName('server')
        assertThat(configuration, notNullValue())

        configuration = project.configurations.getByName('plugin')
        assertThat(configuration, notNullValue())

        configuration = project.configurations.getByName('provided')
        assertThat(configuration, notNullValue())
        assertThat(configuration.visible, is(false))
        assertThat(configuration.transitive, is(true))
    }
}
