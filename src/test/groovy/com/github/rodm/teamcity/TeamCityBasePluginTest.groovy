/*
 * Copyright 2022 Rod MacKenzie
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
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.initialization.GradlePropertiesController
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir

import static com.github.rodm.teamcity.ValidationMode.FAIL
import static com.github.rodm.teamcity.ValidationMode.IGNORE
import static com.github.rodm.teamcity.ValidationMode.WARN
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.instanceOf
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.not
import static org.hamcrest.Matchers.nullValue
import static org.hamcrest.Matchers.startsWith
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class TeamCityBasePluginTest {

    private Project project

    @BeforeEach
    void setup(@TempDir File projectDir) {
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.apply plugin: 'io.github.rodm.teamcity-base'

        // workaround for https://github.com/gradle/gradle/issues/13122
        (project as ProjectInternal).services.get(GradlePropertiesController).loadGradlePropertiesFrom(projectDir)
    }

    @Test
    void 'applies Gradle BasePlugin for lifecycle tasks'() {
        assertTrue(project.plugins.hasPlugin(BasePlugin))
    }

    @Test
    void 'apply base plugin creates teamcity extension'() {
        def extension = project.extensions.getByName('teamcity')
        assertThat(extension, is(not(nullValue())))
        assertThat(extension, instanceOf(TeamCityPluginExtension))
    }

    @Test
    void 'default TeamCity API version'() {
        def extension = project.extensions.getByName('teamcity') as TeamCityPluginExtension
        assertEquals('9.0', extension.version)
    }

    @Test
    void 'specified TeamCity API version'() {
        project.teamcity {
            version = '8.1.5'
        }

        def extension = project.extensions.getByName('teamcity') as TeamCityPluginExtension
        assertEquals('8.1.5', extension.version)
    }

    @Test
    void 'reject snapshot version without allow snapshots setting'() {
        def expected = assertThrows(ProjectConfigurationException.class) {
            project.teamcity {
                version = '2020.2-SNAPSHOT'
            }
            project.evaluate()
        }
        assertThat(expected.cause.message, startsWith("'2020.2-SNAPSHOT' is not a valid TeamCity version"))
    }

    @Test
    void 'accept snapshot version with allow snapshots setting'() {
        project.teamcity {
            version = '2020.2-SNAPSHOT'
            allowSnapshotVersions = true
        }
        project.evaluate()

        def extension = project.extensions.getByName('teamcity') as TeamCityPluginExtension
        assertEquals('2020.2-SNAPSHOT', extension.version)
    }

    @Test
    void 'accept release version with allow snapshots setting'() {
        project.teamcity {
            version = '2020.2'
            allowSnapshotVersions = true
        }
        project.evaluate()

        def extension = project.extensions.getByName('teamcity') as TeamCityPluginExtension
        assertEquals('2020.2', extension.version)
    }

    @Test
    void 'default mode for bean definition validation'() {
        def extension = project.extensions.getByName('teamcity') as TeamCityPluginExtension
        assertThat(extension.validateBeanDefinition, equalTo(WARN))
    }

    @Test
    void 'set ignore mode for bean definition validation'() {
        project.teamcity {
            validateBeanDefinition = 'ignore'
        }
        project.evaluate()

        def extension = project.extensions.getByName('teamcity') as TeamCityPluginExtension
        assertThat(extension.validateBeanDefinition, equalTo(IGNORE))
    }

    @Nested
    class SubProjects {

        private Project rootProject
        private Project subproject

        private final ResettableOutputEventListener outputEventListener = new ResettableOutputEventListener()

        @RegisterExtension
        public final ConfigureLogging logging = new ConfigureLogging(outputEventListener)

        @BeforeEach
        void setup() {
            rootProject = project
            subproject = ProjectBuilder.builder().withParent(rootProject).build()
        }

        @Test
        void 'sub-project inherits version from root project'() {
            rootProject.teamcity {
                version = '8.1.5'
            }

            subproject.apply plugin: 'io.github.rodm.teamcity-base'

            def extension = subproject.extensions.getByName('teamcity') as TeamCityPluginExtension
            assertEquals('8.1.5', extension.version)
        }

        @Test
        void 'sub-project lazily inherits version from root project'() {
            subproject.apply plugin: 'io.github.rodm.teamcity-base'

            rootProject.teamcity {
                version = '8.1.5'
            }

            def extension = subproject.extensions.getByName('teamcity') as TeamCityPluginExtension
            assertEquals('8.1.5', extension.version)
        }

        @Test
        void 'sub-project inherits properties from root project'() {
            rootProject.teamcity {
                defaultRepositories = false
                allowSnapshotVersions = true
                validateBeanDefinition = IGNORE
            }

            subproject.apply plugin: 'io.github.rodm.teamcity-base'

            def extension = subproject.extensions.getByName('teamcity') as TeamCityPluginExtension
            assertThat(extension.defaultRepositories, is(false))
            assertThat(extension.allowSnapshotVersions, is(true))
            assertThat(extension.validateBeanDefinition, is(IGNORE))
        }

        @Test
        void 'sub-project lazily inherits properties from root project'() {
            subproject.apply plugin: 'io.github.rodm.teamcity-base'

            rootProject.teamcity {
                defaultRepositories = false
                allowSnapshotVersions = true
                validateBeanDefinition = FAIL
            }

            def extension = subproject.extensions.getByName('teamcity') as TeamCityPluginExtension
            assertThat(extension.defaultRepositories, is(false))
            assertThat(extension.allowSnapshotVersions, is(true))
            assertThat(extension.validateBeanDefinition, is(FAIL))
        }

        @Test
        void 'output deprecation messages for inherited properties'() {
            rootProject.teamcity {
                version = '2024.03.2'
                defaultRepositories = false
                allowSnapshotVersions = true
                validateBeanDefinition = IGNORE
            }

            subproject.apply plugin: 'io.github.rodm.teamcity-base'

            def extension = subproject.extensions.getByName('teamcity') as TeamCityPluginExtension
            assertThat(extension.version, is('2024.03.2'))
            assertThat(extension.defaultRepositories, is(false))
            assertThat(extension.allowSnapshotVersions, is(true))
            assertThat(extension.validateBeanDefinition, is(IGNORE))

            def output = outputEventListener.toString()
            assertThat(output, containsString("Using inherited property 'version' is deprecated"))
            assertThat(output, containsString("Using inherited property 'defaultRepositories' is deprecated"))
            assertThat(output, containsString("Using inherited property 'allowSnapshotVersions' is deprecated"))
            assertThat(output, containsString("Using inherited property 'validateBeanDefinition' is deprecated"))
        }

        @Test
        void 'sub-project has default properties and no deprecation messages when inherited properties disabled'() {
            rootProject.projectDir.toPath().resolve('gradle.properties').toFile() << """
            teamcity.disableInheritedProperties = true
            """
            rootProject = ProjectBuilder.builder().withProjectDir(rootProject.projectDir).build()
            rootProject.apply plugin: 'io.github.rodm.teamcity-base'
            subproject = ProjectBuilder.builder().withParent(rootProject).build()
            // workaround for https://github.com/gradle/gradle/issues/13122
            (rootProject as ProjectInternal).services.get(GradlePropertiesController).loadGradlePropertiesFrom(rootProject.projectDir)

            rootProject.teamcity {
                version = '2024.03.2'
                defaultRepositories = false
                allowSnapshotVersions = true
                validateBeanDefinition = IGNORE
            }

            subproject.apply plugin: 'io.github.rodm.teamcity-base'

            def extension = subproject.extensions.getByName('teamcity') as TeamCityPluginExtension
            // subproject has default values
            assertThat(extension.version, is('9.0'))
            assertThat(extension.defaultRepositories, is(true))
            assertThat(extension.allowSnapshotVersions, is(false))
            assertThat(extension.validateBeanDefinition, is(WARN))

            def output = outputEventListener.toString()
            assertThat(output, not(containsString("Using inherited property")))
        }
    }
}
