/*
 * Copyright 2021 Rod MacKenzie
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
import org.junit.jupiter.api.Test

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.MatcherAssert.assertThat

class MultipleProjectPluginFunctionalTest extends FunctionalTestCase {

    @Test
    void allowSnapshotVersions() {
        buildFile << """
            plugins {
                id 'io.github.rodm.teamcity-server'
            }

            teamcity {
                version = '2021.2-SNAPSHOT'
                allowSnapshotVersions = true
            }
        """

        createFile(createDirectory('common').toPath(), 'build.gradle') << """
            plugins {
                id 'java'
                id 'io.github.rodm.teamcity-common'
            }
            teamcity {
                allowSnapshotVersions = true
            }
        """

        createFile(createDirectory('agent').toPath(), 'build.gradle') << """
            plugins {
                id 'java'
                id 'io.github.rodm.teamcity-agent'
            }
            dependencies {
                implementation (project(':common'))
            }
            teamcity {
                allowSnapshotVersions = true
                agent {
                    descriptor {
                        pluginDeployment {
                            useSeparateClassloader = true
                        }
                    }
                }
            }
        """

        createFile(createDirectory('server').toPath(), 'build.gradle') << """
            plugins {
                id 'java'
                id 'io.github.rodm.teamcity-server'
            }
            dependencies {
                implementation (project(':common'))
                agent (project(path: ':agent', configuration: 'plugin'))
            }
            teamcity {
                allowSnapshotVersions = true
                server {
                    descriptor {
                        name = 'example-plugin'
                        displayName = 'Example Plugin'
                        version = '1.0'
                        vendorName = 'demo'
                        useSeparateClassloader = true
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

        BuildResult result = executeBuild()

        assertThat(result.task(":agent:generateAgentDescriptor").getOutcome(), is(SUCCESS))
        assertThat(result.task(":agent:agentPlugin").getOutcome(), is(SUCCESS))
        assertThat(result.task(":server:generateServerDescriptor").getOutcome(), is(SUCCESS))
        assertThat(result.task(":server:serverPlugin").getOutcome(), is(SUCCESS))
    }
}
