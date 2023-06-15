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

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.isA
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.jupiter.api.Assertions.assertThrows

@SuppressWarnings('ConfigurationAvoidance')
class AgentServerConfigurationTest {

    private File projectDir
    private Project project

    @BeforeEach
    void setup(@TempDir File projectDir) {
        this.projectDir = projectDir
        this.project = ProjectBuilder.builder().withProjectDir(projectDir).build()
    }

    @Test
    void 'cannot apply server plugin with java and agent plugins'() {
        project.apply plugin: 'java'
        project.apply plugin: 'io.github.rodm.teamcity-agent'

        def e = assertThrows(GradleException.class) {
            project.apply plugin: 'io.github.rodm.teamcity-server'
        }
        assertThat(e.message, containsString('Failed to apply plugin'))
    }

    @Test
    void 'cannot apply server plugin with agent and java plugins'() {
        project.apply plugin: 'io.github.rodm.teamcity-agent'
        project.apply plugin: 'java'

        def e = assertThrows(GradleException.class) {
            project.apply plugin: 'io.github.rodm.teamcity-server'
        }
        assertThat(e.message, containsString('Failed to apply plugin'))
    }

    @Test
    void 'cannot apply agent plugin with java and sever plugins'() {
        project.apply plugin: 'java'
        project.apply plugin: 'io.github.rodm.teamcity-server'

        def e = assertThrows(GradleException.class) {
            project.apply plugin: 'io.github.rodm.teamcity-agent'
        }
        assertThat(e.message, containsString('Failed to apply plugin'))
    }

    @Test
    void 'cannot apply agent plugin with server and java plugins'() {
        project.apply plugin: 'io.github.rodm.teamcity-server'
        project.apply plugin: 'java'

        def e = assertThrows(GradleException.class) {
            project.apply plugin: 'io.github.rodm.teamcity-agent'
        }
        assertThat(e.message, containsString('Failed to apply plugin'))
    }

    @Test
    void 'cannot apply java plugin with agent and server plugins'() {
        project.apply plugin: 'io.github.rodm.teamcity-agent'
        project.apply plugin: 'io.github.rodm.teamcity-server'

        def e = assertThrows(GradleException.class) {
            project.apply plugin: 'java'
        }
        assertThat(e.message, containsString('Failed to apply plugin'))
    }

    @Test
    void 'cannot apply java plugin with server and agent plugins'() {
        project.apply plugin: 'io.github.rodm.teamcity-server'
        project.apply plugin: 'io.github.rodm.teamcity-agent'

        def e = assertThrows(GradleException.class) {
            project.apply plugin: 'java'
        }
        assertThat(e.message, containsString('Failed to apply plugin'))
    }

    @Test
    void applyBothPlugins() {
        project.apply plugin: 'io.github.rodm.teamcity-agent'
        project.apply plugin: 'io.github.rodm.teamcity-server'
    }

    @Test
    void applyAndConfigureBothPlugins() {
        project.apply plugin: 'io.github.rodm.teamcity-agent'
        project.apply plugin: 'io.github.rodm.teamcity-server'

        project.teamcity {
            agent {
                descriptor {}
            }
            server {
                descriptor {}
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.agent.descriptor, isA(AgentPluginDescriptor))
        assertThat(extension.server.descriptor, isA(ServerPluginDescriptor))
    }

    @Test
    void 'agent and server archive names do not clash'() {
        project.apply plugin: 'io.github.rodm.teamcity-agent'
        project.apply plugin: 'io.github.rodm.teamcity-server'

        project.archivesBaseName = 'my-plugin'
        project.teamcity {
            agent {
                descriptor {}
            }
            server {
                descriptor {}
            }
        }

        project.evaluate()

        Zip agentPlugin = (Zip) project.tasks.findByPath(':agentPlugin')
        Zip serverPlugin = (Zip) project.tasks.findByPath(':serverPlugin')
        assertThat(agentPlugin.archiveFileName, not(equalTo(serverPlugin.archiveFileName)))
    }
}
