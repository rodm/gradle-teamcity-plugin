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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.util.zip.ZipFile

import static org.gradle.testkit.runner.TaskOutcome.*
import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertEquals

public class AgentAndServerPluginFunctionalTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    private File buildFile

    @Before
    public void setup() throws IOException {
        buildFile = testProjectDir.newFile("build.gradle")
    }

    @Test
    public void agentAndServerPluginPackage() {
        buildFile << """
            plugins {
                id 'com.github.rodm.teamcity-agent'
                id 'com.github.rodm.teamcity-server'
            }
            dependencies {
                agent files('lib/agent-lib.jar')
                server files('lib/server-lib.jar')
            }
            teamcity {
                version = '8.1.5'
                agent {
                    descriptor {
                    }
                }
                server {
                    descriptor {
                    }
                }
            }
        """

        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << """
            rootProject.name = 'test-plugin'
        """

        testProjectDir.newFolder('lib')
        testProjectDir.newFile('lib/agent-lib.jar')
        testProjectDir.newFile('lib/server-lib.jar')

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("build")
                .withPluginClasspath()
                .build();

        assertEquals(result.task(":agentPlugin").getOutcome(), SUCCESS)
        assertEquals(result.task(":serverPlugin").getOutcome(), SUCCESS)

        ZipFile agentPluginFile = new ZipFile(new File(testProjectDir.root, 'build/distributions/test-plugin-agent.zip'))
        List<String> agentEntries = agentPluginFile.entries().collect { it.name }
        assertThat(agentEntries, hasItem('teamcity-plugin.xml'))
        assertThat(agentEntries, hasItem('lib/agent-lib.jar'))

        ZipFile serverPluginFile = new ZipFile(new File(testProjectDir.root, 'build/distributions/test-plugin.zip'))
        List<String> entries = serverPluginFile.entries().collect { it.name }
        assertThat(entries, hasItem('teamcity-plugin.xml'))
        assertThat(entries, hasItem('agent/test-plugin-agent.zip'))
        assertThat(entries, hasItem('server/server-lib.jar'))
    }


    @Test
    public void multiProjectPlugin() {
        buildFile << """
            plugins {
                id 'com.github.rodm.teamcity-agent'
                id 'com.github.rodm.teamcity-server'
            }

            subprojects {
                apply plugin: 'java'
            }
            project(':agent') {
                dependencies {
                    compile project(':common')
                    compile files("\$rootDir/lib/agent-lib.jar")
                }
            }
            project(':server') {
                dependencies {
                    compile project(':common')
                    compile files("\$rootDir/lib/server-lib.jar")
                }
            }

            dependencies {
                agent project(':agent')
                agent project(':common')
                agent files("\$rootDir/lib/agent-lib.jar")
                server project(':server')
                server project(':common')
                server files("\$rootDir/lib/server-lib.jar")
            }

            teamcity {
                version = '8.1.5'
                agent {
                    descriptor {
                    }
                }
                server {
                    descriptor {
                    }
                }
            }
        """

        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << """
            rootProject.name = 'test-plugin'
            include 'common'
            include 'agent'
            include 'server'
        """

        testProjectDir.newFolder('lib')
        testProjectDir.newFile('lib/agent-lib.jar')
        testProjectDir.newFile('lib/server-lib.jar')

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("build")
                .withPluginClasspath()
                .build();

        assertEquals(result.task(":agentPlugin").getOutcome(), SUCCESS)
        assertEquals(result.task(":serverPlugin").getOutcome(), SUCCESS)

        ZipFile agentPluginFile = new ZipFile(new File(testProjectDir.root, 'build/distributions/test-plugin-agent.zip'))
        List<String> agentEntries = agentPluginFile.entries().collect { it.name }
        assertThat(agentEntries, hasItem('teamcity-plugin.xml'))
        assertThat(agentEntries, hasItem('lib/common.jar'))
        assertThat(agentEntries, hasItem('lib/agent.jar'))
        assertThat(agentEntries, hasItem('lib/agent-lib.jar'))

        ZipFile serverPluginFile = new ZipFile(new File(testProjectDir.root, 'build/distributions/test-plugin.zip'))
        List<String> entries = serverPluginFile.entries().collect { it.name }
        assertThat(entries, hasItem('teamcity-plugin.xml'))
        assertThat(entries, hasItem('agent/test-plugin-agent.zip'))
        assertThat(entries, hasItem('server/common.jar'))
        assertThat(entries, hasItem('server/server.jar'))
        assertThat(entries, hasItem('server/server-lib.jar'))
    }

    @Test
    public void pluginsWithJavaFail() {
        buildFile << """
            plugins {
                id 'java'
                id 'com.github.rodm.teamcity-agent'
                id 'com.github.rodm.teamcity-server'
            }
            teamcity {
                version = '8.1.5'
            }
        """

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("assemble")
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.getOutput(), containsString("Cannot apply both the teamcity-agent and teamcity-server plugins with the Java plugin"))
    }
}
