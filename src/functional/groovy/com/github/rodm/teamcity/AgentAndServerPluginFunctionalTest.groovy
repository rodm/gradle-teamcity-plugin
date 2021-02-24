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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

import static com.github.rodm.teamcity.TestSupport.SETTINGS_SCRIPT_DEFAULT
import static com.github.rodm.teamcity.TestSupport.executeBuild
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.MatcherAssert.assertThat

class AgentAndServerPluginFunctionalTest {

    @TempDir
    public Path testProjectDir

    private File buildFile
    private File settingsFile

    @BeforeEach
    void setup() throws IOException {
        buildFile = createFile('build.gradle')
        settingsFile = createFile('settings.gradle')
    }

    private File createFile(String name) {
        Files.createFile(testProjectDir.resolve(name)).toFile()
    }

    private Path createDirectory(String name) {
        Files.createDirectory(testProjectDir.resolve(name))
    }

    private BuildResult executeBuild(String... args = ['build']) {
        return executeBuild(testProjectDir.toFile(), args)
    }

    @Test
    void agentAndServerPluginPackage() {
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
                        pluginDeployment {}
                    }
                }
                server {
                    descriptor {
                        name = 'test-plugin'
                        displayName = 'Test plugin'
                        version = '1.0'
                        vendorName = 'vendor name'
                    }
                }
            }
        """

        settingsFile << SETTINGS_SCRIPT_DEFAULT

        createDirectory('lib')
        createFile('lib/agent-lib.jar')
        createFile('lib/server-lib.jar')

        BuildResult result = executeBuild()

        assertThat(result.task(":agentPlugin").getOutcome(), is(SUCCESS))
        assertThat(result.task(":serverPlugin").getOutcome(), is(SUCCESS))

        ZipFile agentPluginFile = new ZipFile(testProjectDir.resolve('build/distributions/test-plugin-agent.zip').toFile())
        List<String> agentEntries = agentPluginFile.entries().collect { it.name }
        assertThat(agentEntries, hasItem('teamcity-plugin.xml'))
        assertThat(agentEntries, hasItem('lib/agent-lib.jar'))

        ZipFile serverPluginFile = new ZipFile(testProjectDir.resolve('build/distributions/test-plugin.zip').toFile())
        List<String> entries = serverPluginFile.entries().collect { it.name }
        assertThat(entries, hasItem('teamcity-plugin.xml'))
        assertThat(entries, hasItem('agent/test-plugin-agent.zip'))
        assertThat(entries, hasItem('server/server-lib.jar'))
    }

    @Test
    void multiProjectPlugin() {
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
                    implementation project(':common')
                    implementation files("\$rootDir/lib/agent-lib.jar")
                }
            }
            project(':server') {
                dependencies {
                    implementation project(':common')
                    implementation files("\$rootDir/lib/server-lib.jar")
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
                        pluginDeployment {}
                    }
                }
                server {
                    descriptor {
                        name = 'test-plugin'
                        displayName = 'Test plugin'
                        version = '1.0'
                        vendorName = 'vendor name'
                    }
                }
            }
        """

        settingsFile << """
            rootProject.name = 'test-plugin'
            include 'common'
            include 'agent'
            include 'server'
        """

        createDirectory('lib')
        createFile('lib/agent-lib.jar')
        createFile('lib/server-lib.jar')

        BuildResult result = executeBuild()

        assertThat(result.task(":agentPlugin").getOutcome(), is(SUCCESS))
        assertThat(result.task(":serverPlugin").getOutcome(), is(SUCCESS))

        ZipFile agentPluginFile = new ZipFile(testProjectDir.resolve('build/distributions/test-plugin-agent.zip').toFile())
        List<String> agentEntries = agentPluginFile.entries().collect { it.name }
        assertThat(agentEntries, hasItem('teamcity-plugin.xml'))
        assertThat(agentEntries, hasItem('lib/common.jar'))
        assertThat(agentEntries, hasItem('lib/agent.jar'))
        assertThat(agentEntries, hasItem('lib/agent-lib.jar'))

        ZipFile serverPluginFile = new ZipFile(testProjectDir.resolve('build/distributions/test-plugin.zip').toFile())
        List<String> entries = serverPluginFile.entries().collect { it.name }
        assertThat(entries, hasItem('teamcity-plugin.xml'))
        assertThat(entries, hasItem('agent/test-plugin-agent.zip'))
        assertThat(entries, hasItem('server/common.jar'))
        assertThat(entries, hasItem('server/server.jar'))
        assertThat(entries, hasItem('server/server-lib.jar'))
    }

    @Test
    void pluginsWithJavaFail() {
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
                .withProjectDir(testProjectDir.toFile())
                .withArguments("assemble")
                .withPluginClasspath()
                .forwardOutput()
                .buildAndFail()

        assertThat(result.getOutput(), containsString("Cannot apply both the teamcity-agent and teamcity-server plugins with the Java plugin"))
    }
}
