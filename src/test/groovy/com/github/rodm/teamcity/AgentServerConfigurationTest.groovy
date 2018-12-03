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

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.isA
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.MatcherAssert.assertThat

class AgentServerConfigurationTest {

    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder()

    @Rule
    public ExpectedException thrown = ExpectedException.none()

    private Project project

    @Before
    void setup() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.root).build()
    }

    @Test
    void 'cannot apply server plugin with java and agent plugins'() {
        project.apply plugin: 'java'
        project.apply plugin: 'com.github.rodm.teamcity-agent'

        thrown.expect(GradleException)
        thrown.expectMessage('Failed to apply plugin')
        project.apply plugin: 'com.github.rodm.teamcity-server'
    }

    @Test
    void 'cannot apply server plugin with agent and java plugins'() {
        project.apply plugin: 'com.github.rodm.teamcity-agent'
        project.apply plugin: 'java'

        thrown.expect(GradleException)
        thrown.expectMessage('Failed to apply plugin')
        project.apply plugin: 'com.github.rodm.teamcity-server'
    }

    @Test
    void 'cannot apply agent plugin with java and sever plugins'() {
        project.apply plugin: 'java'
        project.apply plugin: 'com.github.rodm.teamcity-server'

        thrown.expect(GradleException)
        thrown.expectMessage('Failed to apply plugin')
        project.apply plugin: 'com.github.rodm.teamcity-agent'
    }

    @Test
    void 'cannot apply agent plugin with server and java plugins'() {
        project.apply plugin: 'com.github.rodm.teamcity-server'
        project.apply plugin: 'java'

        thrown.expect(GradleException)
        thrown.expectMessage('Failed to apply plugin')
        project.apply plugin: 'com.github.rodm.teamcity-agent'
    }

    @Test
    void 'cannot apply java plugin with agent and server plugins'() {
        project.apply plugin: 'com.github.rodm.teamcity-agent'
        project.apply plugin: 'com.github.rodm.teamcity-server'

        thrown.expect(GradleException)
        thrown.expectMessage('Failed to apply plugin')
        project.apply plugin: 'java'
    }

    @Test
    void 'cannot apply java plugin with server and agent plugins'() {
        project.apply plugin: 'com.github.rodm.teamcity-server'
        project.apply plugin: 'com.github.rodm.teamcity-agent'

        thrown.expect(GradleException)
        thrown.expectMessage('Failed to apply plugin')
        project.apply plugin: 'java'
    }

    @Test
    void applyBothPlugins() {
        project.apply plugin: 'com.github.rodm.teamcity-agent'
        project.apply plugin: 'com.github.rodm.teamcity-server'
    }

    @Test
    void applyAndConfigureBothPlugins() {
        project.apply plugin: 'com.github.rodm.teamcity-agent'
        project.apply plugin: 'com.github.rodm.teamcity-server'

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
        project.apply plugin: 'com.github.rodm.teamcity-agent'
        project.apply plugin: 'com.github.rodm.teamcity-server'

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
        assertThat(agentPlugin.archiveName, not(equalTo(serverPlugin.archiveName)))
    }
}
