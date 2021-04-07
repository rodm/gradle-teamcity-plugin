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
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static com.github.rodm.teamcity.GradleMatchers.hasDependency
import static com.github.rodm.teamcity.TestSupport.normalizePath
import static org.hamcrest.CoreMatchers.anyOf
import static org.hamcrest.CoreMatchers.endsWith
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.CoreMatchers.notNullValue
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.startsWith
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assertions.fail

class TeamCityServerPluginTest {

    private Project project

    @BeforeEach
    void setup(@TempDir File projectDir) {
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
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
    void 'reject snapshot version without allow snapshots setting'() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        try {
            project.teamcity {
                version = '2020.2-SNAPSHOT'
            }
            project.evaluate()
            fail('Invalid version not rejected')
        }
        catch (ProjectConfigurationException expected) {
            def cause = expected.cause
            assertThat(cause.message, startsWith("'2020.2-SNAPSHOT' is not a valid TeamCity version"))
        }
    }

    @Test
    void 'allow snapshot version with allow snapshots setting'() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            version = '2020.2-SNAPSHOT'
            allowSnapshotVersions = true
        }
        project.evaluate()

        assertEquals('2020.2-SNAPSHOT', project.extensions.getByName('teamcity').version)
    }

    @Test
    void 'default mode for bean definition validation'() {
        project.apply plugin: 'com.github.rodm.teamcity-server'
        project.evaluate()

        TeamCityPluginExtension extension = (TeamCityPluginExtension) project.extensions.getByName('teamcity')
        assertThat(extension.validateBeanDefinition.get(), equalTo(ValidationMode.WARN))
    }

    @Test
    void 'set ignore mode for bean definition validation'() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            validateBeanDefinition = 'ignore'
        }
        project.evaluate()

        TeamCityPluginExtension extension = (TeamCityPluginExtension) project.extensions.getByName('teamcity')
        assertThat(extension.validateBeanDefinition.get(), equalTo(ValidationMode.IGNORE))
    }

    @Test
    void 'sub-project inherits version from root poject'() {
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
    void 'sub-project lazily inherits version '() {
        Project rootProject = project

        Project subproject = ProjectBuilder.builder().withParent(rootProject).build()
        subproject.apply plugin: 'com.github.rodm.teamcity-server'

        rootProject.apply plugin: 'com.github.rodm.teamcity-server'
        rootProject.teamcity {
            version = '8.1.5'
        }

        assertEquals('8.1.5', subproject.extensions.getByName('teamcity').version)
    }

    @Test
    void configurationsCreatedWithoutJavaPlugin() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

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
    void 'creates configuration for signing and publishing task dependencies'() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        def configuration = project.configurations.getByName('marketplace')
        assertThat(configuration, notNullValue())
        assertThat(configuration.visible, is(false))
        assertThat(configuration.transitive, is(true))
    }

    @Test
    void 'apply adds signing and publishing dependencies to the marketplace configuration'() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('marketplace')
        assertThat(configuration, hasDependency('org.jetbrains', 'marketplace-zip-signer', '0.1.3'))
        assertThat(configuration, hasDependency('org.jetbrains.intellij.plugins', 'structure-base', '3.164'))
        assertThat(configuration, hasDependency('org.jetbrains.intellij.plugins', 'structure-teamcity', '3.164'))
        assertThat(configuration, hasDependency('org.jetbrains.intellij', 'plugin-repository-rest-client', '2.0.15'))
    }

    @Test
    void 'adds jcenter repository for signing and publishing dependencies'() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.evaluate()

        List<String> urls = project.repositories.collect { repository -> repository.url.toString() }
        assertThat(urls, hasItem('https://jcenter.bintray.com/'))
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
    void 'apply adds server-web-api to the provided configuration'() {
        project.apply plugin: 'java'
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('provided')
        assertThat(configuration, hasDependency('org.jetbrains.teamcity', 'server-web-api', '9.0'))
    }

    @Test
    void 'apply does not add server-web-api to the provided configuration for versions before 9_0'() {
        project.apply plugin: 'java'
        project.apply plugin: 'com.github.rodm.teamcity-server'
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
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('testImplementation')
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
