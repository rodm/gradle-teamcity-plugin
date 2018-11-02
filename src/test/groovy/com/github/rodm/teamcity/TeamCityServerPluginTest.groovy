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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static com.github.rodm.teamcity.GradleMatchers.hasDependency
import static com.github.rodm.teamcity.TestSupport.normalizePath
import static org.hamcrest.CoreMatchers.anyOf
import static org.hamcrest.CoreMatchers.endsWith
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.CoreMatchers.notNullValue
import static org.hamcrest.Matchers.hasSize
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

class TeamCityServerPluginTest {

    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder()

    private Project project

    @Before
    void setup() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.root).build()
    }

    @Test
    void applyBasePlugin() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        assertTrue(project.plugins.hasPlugin(BasePlugin))
    }

    @Test
    void addTeamCityPluginExtension() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        assertTrue(project.extensions.getByName('teamcity') instanceof TeamCityPluginExtension)
    }

    @Test
    void defaultVersion() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        assertEquals('9.0', project.extensions.getByName('teamcity').version)
    }

    @Test
    void specifiedVersion() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            version = '8.1.5'
        }
        assertEquals('8.1.5', project.extensions.getByName('teamcity').version)
    }

    @Test
    void subprojectInheritsVersion() {
        Project rootProject = project

        rootProject.apply plugin: 'com.github.rodm.teamcity-server'
        rootProject.teamcity {
            version = '8.1.5'
        }

        Project subproject = ProjectBuilder.builder().withParent(rootProject).build()
        subproject.apply plugin: 'com.github.rodm.teamcity-server'

        assertEquals('8.1.5', subproject.extensions.getByName('teamcity').version)
    }

    @Test
    void configurationsCreatedWithoutJavaPlugin() {
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
    void providedConfigurationNotCreatedWithoutJavaPlugin() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        def configurationNames = project.configurations.getNames()
        assertThat(configurationNames, not(hasItem('provided')))
    }

    @Test
    void configurationsCreatedWithJavaPlugin() {
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

    @Test
    void 'ConfigureRepositories adds MavenCentral and JetBrains repositories'() {
        project.apply plugin: 'java'

        TeamCityPluginExtension extension = project.extensions.create('teamcity', TeamCityPluginExtension, project)
        TeamCityPlugin.ConfigureRepositories configureRepositories = new TeamCityPlugin.ConfigureRepositories(extension)

        configureRepositories.execute(project)

        List<String> urls = project.repositories.collect { repository -> repository.url.toString() }
        assertThat(urls, anyOf(hasItem('https://repo1.maven.org/maven2/'), hasItem('https://repo.maven.apache.org/maven2/')))
        assertThat(urls, hasItem('https://download.jetbrains.com/teamcity-repository'))
    }

    @Test
    void 'ConfigureRepositories adds no repositories when Java plugin is not applied'() {
        TeamCityPluginExtension extension = project.extensions.create('teamcity', TeamCityPluginExtension, project)
        TeamCityPlugin.ConfigureRepositories configureRepositories = new TeamCityPlugin.ConfigureRepositories(extension)

        configureRepositories.execute(project)

        assertThat(project.repositories.size(), equalTo(0))
    }

    @Test
    void 'ConfigureRepositories adds no repositories when defaultRepositories is false'() {
        TeamCityPluginExtension extension = project.extensions.create('teamcity', TeamCityPluginExtension, project)
        TeamCityPlugin.ConfigureRepositories configureRepositories = new TeamCityPlugin.ConfigureRepositories(extension)
        extension.defaultRepositories = false

        configureRepositories.execute(project)

        assertThat(project.repositories.size(), equalTo(0))
    }

    @Test
    void 'apply adds server-api to the provided configuration'() {
        project.apply plugin: 'java'
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('provided')
        assertThat(configuration, hasDependency('org.jetbrains.teamcity', 'server-api', '9.0'))
    }

    @Test
    void 'apply adds tests-support to the testCompile configuration'() {
        project.apply plugin: 'java'
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('testCompile')
        assertThat(configuration, hasDependency('org.jetbrains.teamcity', 'tests-support', '9.0'))
    }

    @Test
    void 'server-side plugin artifact is published to the plugin configuration'() {
        project.apply plugin: 'java'
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('plugin')
        assertThat(configuration.artifacts, hasSize(1))
        assertThat(normalizePath(configuration.artifacts[0].file), endsWith('/build/distributions/test.zip'))
    }
}
