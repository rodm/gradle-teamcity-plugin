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

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*

public class TeamCityPluginWithoutJavaPluginTest {

    private Project project;

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.rodm.teamcity'
    }

    @Test
    public void createsConfigurations() {
        def configuration = project.configurations.getByName('agent')
        assertThat(configuration, notNullValue())
        assertThat(configuration.visible, is(false))
        assertThat(configuration.transitive, is(false))

        configuration = project.configurations.getByName('plugin')
        assertThat(configuration, notNullValue())
        assertThat(configuration.visible, is(false))
        assertThat(configuration.transitive, is(false))
    }

    @Test
    public void doesNotCreateConfiguration() {
        def configurationNames = project.configurations.getNames()
        assertThat(configurationNames, not(hasItem('providedCompile')))
    }
}
