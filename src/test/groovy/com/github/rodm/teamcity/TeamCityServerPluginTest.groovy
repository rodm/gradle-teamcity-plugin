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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.junit.jupiter.api.io.TempDir

import static com.github.rodm.teamcity.GradleMatchers.hasDefaultDependency
import static com.github.rodm.teamcity.GradleMatchers.hasDependency
import static com.github.rodm.teamcity.TestSupport.normalizePath
import static org.hamcrest.CoreMatchers.anyOf
import static org.hamcrest.CoreMatchers.endsWith
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.CoreMatchers.notNullValue
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is

class TeamCityServerPluginTest {

    private Project project

    @BeforeEach
    void setup(@TempDir File projectDir) {
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.apply plugin: 'io.github.rodm.teamcity-server'
    }

    @Test
    void configurationsCreatedWithoutJavaPlugin() {
        def configuration = project.configurations.getByName('agent')
        assertThat(configuration, notNullValue())
        assertThat(configuration.visible, is(false))
        assertThat(configuration.transitive, is(true))

        configuration = project.configurations.getByName('server')
        assertThat(configuration, notNullValue())
        assertThat(configuration.visible, is(false))
        assertThat(configuration.transitive, is(true))

        configuration = project.configurations.getByName('plugin')
        assertThat(configuration, notNullValue())
        assertThat(configuration.visible, is(false))
        assertThat(configuration.transitive, is(false))
    }

    @Test
    void providedConfigurationNotCreatedWithoutJavaPlugin() {
        def configurationNames = project.configurations.getNames()
        assertThat(configurationNames, not(hasItem('provided')))
    }

    @Test
    void configurationsCreatedWithJavaPlugin() {
        project.apply plugin: 'java'

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
    void 'creates configuration for signing and publishing task dependencies'() {
        def configuration = project.configurations.getByName('marketplace')
        assertThat(configuration, notNullValue())
        assertThat(configuration.visible, is(false))
        assertThat(configuration.transitive, is(true))
    }

    @Test
    @EnabledIfSystemProperty(named = "plugin.structure.version", matches = ".*")
    @EnabledIfSystemProperty(named = "plugin.signer.version", matches = ".*")
    @EnabledIfSystemProperty(named = "plugin.client.version", matches = ".*")
    void 'apply adds signing and publishing as default dependencies to the marketplace configuration'() {
        project.evaluate()

        def pluginStructureVersion = System.properties['plugin.structure.version'] as String
        def pluginSignerVersion = System.properties['plugin.signer.version'] as String
        def pluginClientVersion = System.properties['plugin.client.version'] as String

        Configuration configuration = project.configurations.getByName('marketplace')
        assertThat(configuration, hasDefaultDependency('org.jetbrains.intellij.plugins', 'structure-base', pluginStructureVersion))
        assertThat(configuration, hasDefaultDependency('org.jetbrains.intellij.plugins', 'structure-teamcity', pluginStructureVersion))
        assertThat(configuration, hasDefaultDependency('org.jetbrains', 'marketplace-zip-signer', pluginSignerVersion))
        assertThat(configuration, hasDefaultDependency('org.jetbrains.intellij', 'plugin-repository-rest-client', pluginClientVersion))
    }

    @Test
    void 'adds MavenCentral and JetBrains repositories when Java plugin is applied'() {
        project.apply plugin: 'java'
        project.evaluate()

        List<String> urls = project.repositories.collect { repository -> repository.url.toString() }
        assertThat(urls, anyOf(hasItem('https://repo1.maven.org/maven2/'), hasItem('https://repo.maven.apache.org/maven2/')))
        assertThat(urls, hasItem('https://download.jetbrains.com/teamcity-repository'))
    }

    @Test
    void 'adds no repositories when defaultRepositories is false'() {
        project.apply plugin: 'java'

        project.teamcity {
            defaultRepositories = false
        }
        project.evaluate()

        assertThat(project.repositories.size(), equalTo(0))
    }

    @Test
    void 'adds no repositories when Java plugin is not applied'() {
        project.evaluate()

        assertThat(project.repositories.size(), equalTo(0))
    }

    @Test
    void 'apply adds server-api to the provided configuration'() {
        project.apply plugin: 'java'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('provided')
        assertThat(configuration, hasDependency('org.jetbrains.teamcity', 'server-api', '9.0'))
    }

    @Test
    void 'apply adds server-api with specified version to the provided configuration'() {
        project.apply plugin: 'java'
        project.teamcity {
            version = '2021.2'
        }

        project.evaluate()

        Configuration configuration = project.configurations.getByName('provided')
        assertThat(configuration, hasDependency('org.jetbrains.teamcity', 'server-api', '2021.2'))
    }

    @Test
    void 'apply adds server-web-api to the provided configuration'() {
        project.apply plugin: 'java'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('provided')
        assertThat(configuration, hasDependency('org.jetbrains.teamcity', 'server-web-api', '9.0'))
    }

    @Test
    void 'apply does not add server-web-api to the provided configuration for versions before 9_0'() {
        project.apply plugin: 'java'
        project.teamcity {
            version = '8.1'
        }

        project.evaluate()

        Configuration configuration = project.configurations.getByName('provided')
        assertThat(configuration, not(hasDependency('org.jetbrains.teamcity', 'server-web-api', '8.1')))
    }

    @Test
    void 'apply adds tests-support to the testImplementation configuration'() {
        project.apply plugin: 'java'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('testImplementation')
        assertThat(configuration, hasDependency('org.jetbrains.teamcity', 'tests-support', '9.0'))
    }

    @Test
    void 'server-side plugin artifact is published to the plugin configuration'() {
        project.apply plugin: 'java'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('plugin')
        assertThat(configuration.artifacts, hasSize(1))
        assertThat(normalizePath(configuration.artifacts[0].file), endsWith('/build/distributions/test.zip'))
    }
}
