/*
 * Copyright 2018 Rod MacKenzie
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path

import static com.github.rodm.teamcity.TestSupport.SETTINGS_SCRIPT_DEFAULT
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

class EnvironmentsPluginFunctionalTest extends FunctionalTestCase {

    @BeforeEach
    void setup() throws IOException {
        settingsFile << SETTINGS_SCRIPT_DEFAULT
    }

    @Test
    void 'startServer creates data directory'() {
        File homeDir = createFakeTeamCityInstall('teamcity', '9.1.6')
        File javaHome = createDirectory('jdk')
        File dataDir = testProjectDir.resolve('data').toFile()

        buildFile << """
            plugins {
                id 'com.github.rodm.teamcity-environments'
            }
            teamcity {
                environments {
                    teamcity {
                        homeDir = file('${homeDir.toURI()}')
                        dataDir = file('${dataDir.toURI()}')
                        javaHome = file('${javaHome.toURI()}')
                    }
                }
            }
        """

        assertFalse(dataDir.exists(), 'Data directory should not exist')

        BuildResult result = executeBuild('--info', 'startTeamcityServer')

        assertTrue(dataDir.exists(), 'Data directory was not created by startServer task')
        assertThat(result.task(":startTeamcityServer").getOutcome(), is(SUCCESS))
        assertThat(result.output, not(containsString('property is deprecated')))
        assertThat(result.output, not(containsString('server configuration is deprecated')))
    }

    @Test
    void startServerAfterDeployingPlugin() {
        File homeDir = createFakeTeamCityInstall('teamcity', '9.1.6')
        File dataDir = createDirectory('data')

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
                        homeDir = file('${homeDir.toURI()}')
                        dataDir = file('${dataDir.toURI()}')
                        javaHome = file('${dataDir.toURI()}')
                    }
                }
            }
        """

        BuildResult result = executeBuild('build', 'startTeamcityServer')

        File pluginFile = new File(dataDir, 'plugins/test-plugin.zip')
        assertTrue(pluginFile.exists(), 'Plugin archive not deployed')
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

        BuildResult result = executeBuild('build', 'startTeamcity9Server')

        File pluginFile = testProjectDir.resolve('teamcity/data/9.1/plugins/test-plugin.zip').toFile()
        assertTrue(pluginFile.exists(), 'Plugin archive not deployed')
        assertThat(result.task(":startTeamcity9Server").getOutcome(), is(SUCCESS))
        assertThat(result.output, not(containsString('property is deprecated')))
        assertThat(result.output, not(containsString('server configuration is deprecated')))
    }

    @TempDir
    public Path serversDir

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
                    baseHomeDir = file('${serversDir.resolve('teamcity').toUri()}')
                    baseDataDir = file('${serversDir.resolve('data').toUri()}')

                    teamcity8 {
                        version = '8.1.5'
                    }
                    teamcity9 {
                        version = '9.1.6'
                    }
                }
            }
        """

        BuildResult result = executeBuild('build', 'startTeamcity9Server')

        File pluginFile = serversDir.resolve('data/9.1/plugins/test-plugin.zip').toFile()
        assertTrue(pluginFile.exists(), 'Plugin archive not deployed')
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

        createFile('plugin1.zip')
        createFile('plugin2.zip')

        BuildResult result = executeBuild('-S', 'deployToTeamcity')

        File plugin1File = testProjectDir.resolve('teamcity/data/9.1/plugins/plugin1.zip').toFile()
        File plugin2File = testProjectDir.resolve('teamcity/data/9.1/plugins/plugin2.zip').toFile()
        assertThat(result.task(":deployToTeamcity").getOutcome(), is(SUCCESS))
        assertTrue(plugin1File.exists(), 'Plugin1 archive not deployed')
        assertTrue(plugin2File.exists(), 'Plugin2 archive not deployed')

        result = executeBuild('undeployFromTeamcity')
        assertThat(result.task(":undeployFromTeamcity").getOutcome(), is(SUCCESS))
        assertFalse(plugin1File.exists(), 'Plugin1 archive not undeployed')
        assertFalse(plugin2File.exists(), 'Plugin2 archive not undeployed')
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

    private static File createFakeTeamCityInstall(Path folder, String baseDir, String version) {
        File homeDir = createDirectory(folder, "${baseDir}/TeamCity-${version}".toString())
        File binDir = createDirectory(homeDir.toPath(), 'bin')

        File teamcityServerShellFile = createFile(binDir.toPath(), 'teamcity-server.sh')
        teamcityServerShellFile << """
            #!/bin/bash
            echo "Fake TeamCity startup script"
        """
        teamcityServerShellFile.executable = true

        File teamcityServerBatchFile = createFile(binDir.toPath(), 'teamcity-server.bat')
        teamcityServerBatchFile << """
            @echo off
            echo "Fake TeamCity startup script"
        """
        return homeDir
    }
}
