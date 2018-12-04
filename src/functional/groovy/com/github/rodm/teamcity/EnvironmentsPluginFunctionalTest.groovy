/*
 * Copyright 2018 Rod MacKenzie
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

import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class EnvironmentsPluginFunctionalTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    private File buildFile

    @Before
    void setup() throws IOException {
        buildFile = testProjectDir.newFile("build.gradle")
    }

    private BuildResult executeBuild(String... args) {
        GradleRunner.create()
            .withProjectDir(testProjectDir.getRoot())
            .withArguments(args)
            .withPluginClasspath()
            .forwardOutput()
            .build()
    }

    @Test
    void startServerAfterDeployingPlugin() {
        File homeDir = createFakeTeamCityInstall('teamcity', '9.1.6')
        File dataDir = testProjectDir.newFolder('data')

        buildFile << """
            plugins {
                id 'java'
                id 'com.github.rodm.teamcity-server'
                id 'com.github.rodm.teamcity-environments'
            }
            teamcity {
                version = '8.1.5'
                server {
                    descriptor {
                        name = 'test-plugin'
                        displayName = 'Test plugin'
                        version = '1.0'
                        vendorName = 'vendor name'
                    }
                }
                environments {
                    teamcity {
                        homeDir = file('${windowsCompatiblePath(homeDir)}')
                        dataDir = file('${windowsCompatiblePath(dataDir)}')
                        javaHome = file('${windowsCompatiblePath(dataDir)}')
                    }
                }
            }
        """

        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << """
            rootProject.name = 'test-plugin'
        """

        BuildResult result = executeBuild('build', 'startTeamcityServer')

        File pluginFile = new File(dataDir, 'plugins/test-plugin.zip')
        assertTrue('Plugin archive not deployed', pluginFile.exists())
        assertThat(result.task(":deployToTeamcity").getOutcome(), is(SUCCESS))
        assertThat(result.task(":startTeamcityServer").getOutcome(), is(SUCCESS))
        assertThat(result.output, not(containsString('property is deprecated')))
        assertThat(result.output, not(containsString('server configuration is deprecated')))
    }

    @Test
    void startNamedEnvironmentServer() {
        createFakeTeamCityInstall('teamcity', '9.1.6')

        buildFile << """
            plugins {
                id 'java'
                id 'com.github.rodm.teamcity-server'
                id 'com.github.rodm.teamcity-environments'
            }
            teamcity {
                version = '8.1.5'
                server {
                    descriptor {
                        name = 'test-plugin'
                        displayName = 'Test plugin'
                        version = '1.0'
                        vendorName = 'vendor name'
                    }
                }
                environments {
                    baseHomeDir = 'teamcity'
                    baseDataDir = 'teamcity/data'
                    teamcity8 {
                        version = '8.1.5'
                    }
                    teamcity9 {
                        version = '9.1.6'
                    }
                }
            }
        """

        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << """
            rootProject.name = 'test-plugin'
        """

        BuildResult result = executeBuild('build', 'startTeamcity9Server')

        File pluginFile = new File(testProjectDir.root, 'teamcity/data/9.1/plugins/test-plugin.zip')
        assertTrue('Plugin archive not deployed', pluginFile.exists())
        assertThat(result.task(":startTeamcity9Server").getOutcome(), is(SUCCESS))
        assertThat(result.output, not(containsString('property is deprecated')))
        assertThat(result.output, not(containsString('server configuration is deprecated')))
    }

    @Rule
    public final TemporaryFolder serversDir = new TemporaryFolder()

    @Test
    void startNamedEnvironmentServerInAlternativeDirectory() {
        createFakeTeamCityInstall(serversDir, 'teamcity', '9.1.6')

        buildFile << """
            plugins {
                id 'java'
                id 'com.github.rodm.teamcity-server'
                id 'com.github.rodm.teamcity-environments'
            }
            teamcity {
                version = '8.1.5'
                server {
                    descriptor {
                        name = 'test-plugin'
                        displayName = 'Test plugin'
                        version = '1.0'
                        vendorName = 'vendor name'
                    }
                }
                environments {
                    baseHomeDir = file('${windowsCompatiblePath(serversDir.root)}/teamcity')
                    baseDataDir = file('${windowsCompatiblePath(serversDir.root)}/data')

                    teamcity8 {
                        version = '8.1.5'
                    }
                    teamcity9 {
                        version = '9.1.6'
                    }
                }
            }
        """

        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << """
            rootProject.name = 'test-plugin'
        """

        BuildResult result = executeBuild('build', 'startTeamcity9Server')

        File pluginFile = new File(serversDir.root, 'data/9.1/plugins/test-plugin.zip')
        assertTrue('Plugin archive not deployed', pluginFile.exists())
        assertThat(result.task(":startTeamcity9Server").getOutcome(), is(SUCCESS))
        assertThat(result.output, not(containsString('property is deprecated')))
        assertThat(result.output, not(containsString('server configuration is deprecated')))
    }

    @Test
    void 'deploy and undeploy multiple plugins to and from an environment'() {
        buildFile << """
            plugins {
                id 'java'
                id 'com.github.rodm.teamcity-environments'
            }
            teamcity {
                version = '9.1'
                environments {
                    baseDataDir = 'teamcity/data'
                    teamcity {
                        version = '9.1.6'
                        plugins 'plugin1.zip'
                        plugins project.file('plugin2.zip')
                    }
                }
            }
        """

        testProjectDir.newFile('plugin1.zip')
        testProjectDir.newFile('plugin2.zip')

        BuildResult result = executeBuild('-S', 'deployToTeamcity')

        File plugin1File = new File(testProjectDir.root, 'teamcity/data/9.1/plugins/plugin1.zip')
        File plugin2File = new File(testProjectDir.root, 'teamcity/data/9.1/plugins/plugin2.zip')
        assertThat(result.task(":deployToTeamcity").getOutcome(), is(SUCCESS))
        assertTrue('Plugin1 archive not deployed', plugin1File.exists())
        assertTrue('Plugin2 archive not deployed', plugin2File.exists())

        result = executeBuild('undeployFromTeamcity')
        assertThat(result.task(":undeployFromTeamcity").getOutcome(), is(SUCCESS))
        assertFalse('Plugin1 archive not undeployed', plugin1File.exists())
        assertFalse('Plugin2 archive not undeployed', plugin2File.exists())
    }

    @Test
    void 'deploy and undeploy tasks are ignored when no plugins are configured'() {
        buildFile << """
            plugins {
                id 'java'
                id 'com.github.rodm.teamcity-environments'
            }
            teamcity {
                version = '9.1'
                environments {
                    baseDataDir = 'teamcity/data'
                    teamcity {
                        version = '9.1.6'
                    }
                }
            }
        """

        BuildResult result = executeBuild('deployToTeamcity')

        assertThat(result.task(":deployToTeamcity").getOutcome(), is(NO_SOURCE))

        result = executeBuild('undeployFromTeamcity')
        assertThat(result.task(":undeployFromTeamcity").getOutcome(), is(UP_TO_DATE))
    }

    private File createFakeTeamCityInstall(String baseDir, String version) {
        createFakeTeamCityInstall(testProjectDir, baseDir, version)
    }

    private static File createFakeTeamCityInstall(TemporaryFolder folder, String baseDir, String version) {
        File homeDir = folder.newFolder(baseDir, "TeamCity-${version}")
        File binDir = folder.newFolder(baseDir, "TeamCity-${version}", 'bin')

        File teamcityServerShellFile = new File(binDir, 'teamcity-server.sh')
        teamcityServerShellFile << """
            #!/bin/bash
            echo "Fake TeamCity startup script"
        """
        teamcityServerShellFile.executable = true

        File teamcityServerBatchFile = new File(binDir, 'teamcity-server.bat')
        teamcityServerBatchFile << """
            @echo off
            echo "Fake TeamCity startup script"
        """
        return homeDir
    }

    private static String windowsCompatiblePath(File path) {
        path.canonicalPath.replace('\\', '\\\\')
    }
}
