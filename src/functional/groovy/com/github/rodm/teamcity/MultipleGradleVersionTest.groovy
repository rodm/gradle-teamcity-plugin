/*
 * Copyright 2016 Rod MacKenzie
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

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

import static com.github.rodm.teamcity.internal.PluginDefinitionValidationAction.NO_DEFINITION_WARNING_MESSAGE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.jupiter.api.Assumptions.assumeTrue

class MultipleGradleVersionTest extends FunctionalTestCase {

    static final String NO_DEFINITION_WARNING = NO_DEFINITION_WARNING_MESSAGE.substring(4)

    @SuppressWarnings('unused')
    static List<String> data() {
        return [
            '6.0.1', '6.1.1', '6.2.2', '6.3', '6.4.1', '6.5.1', '6.6.1', '6.7.1', '6.8.3', '6.9.1',
            '7.0.2', '7.1.1', '7.2', '7.3'
        ]
    }

    static List<String> releasedJavaVersions() {
        return ['1.8', '1.9', '1.10', '11', '12', '13', '14', '15', '16', '17']
    }

    static List<String> supportedByGradle(String version) {
        def gradleVersion = GradleVersion.version(version)
        def javaVersions = releasedJavaVersions()
        if (gradleVersion < GradleVersion.version('6.3')) {
            javaVersions.remove('14')
        }
        if (gradleVersion < GradleVersion.version('6.7-rc-1')) {
            javaVersions.remove('15')
        }
        if (gradleVersion < GradleVersion.version('7.0-rc-1')) {
            javaVersions.remove('16')
        }
        if (gradleVersion < GradleVersion.version('7.3')) {
            javaVersions.remove('17')
        }
        return javaVersions
    }

    @BeforeEach
    void setup() throws IOException {
        buildFile << """
            plugins {
                id 'java'
                id 'com.github.rodm.teamcity-server'
            }

            project(':common') {
                apply plugin: 'java'
                apply plugin: 'com.github.rodm.teamcity-common'

                teamcity {
                    version = '8.1.5'
                }
            }

            project(':agent') {
                apply plugin: 'java'
                apply plugin: 'com.github.rodm.teamcity-agent'

                dependencies {
                    implementation project(':common')
                }

                teamcity {
                    version = '8.1.5'

                    agent {
                        archiveName = 'test-plugin-agent'
                        descriptor {
                            pluginDeployment {
                                useSeparateClassloader = false
                            }
                        }
                    }
                }
            }

            dependencies {
                implementation project(':common')
                agent project(path: ':agent', configuration: 'plugin')
            }

            teamcity {
                version = '8.1.5'

                server {
                    archiveName = 'test-plugin'
                    descriptor {
                        name = 'test-plugin'
                        displayName = 'Test plugin'
                        description = 'Test plugin description'
                        version = '1.0'
                        vendorName = 'vendor name'
                        vendorUrl = 'https://www.example.org'
                    }
                }
            }
        """

        settingsFile << """
            rootProject.name = 'test-plugin'

            include 'common'
            include 'agent'
        """

        File commonJavaDir = createDirectory('common/src/main/java/example/common')
        File commonJavaFile = createFile(commonJavaDir.toPath(), 'ExampleBuildFeature.java')
        commonJavaFile << """
            package example.common;

            import jetbrains.buildServer.util.StringUtil;

            public class ExampleBuildFeature {
                public static final String BUILD_FEATURE_NAME = "example";
            }
        """

        File agentJavaDir = createDirectory('agent/src/main/java/example/agent')
        File agentJavaFile = createFile(agentJavaDir.toPath(), 'ExampleBuildFeature.java')
        agentJavaFile << """
            package example.agent;

            import jetbrains.buildServer.agent.AgentLifeCycleAdapter;

            public class ExampleBuildFeature extends AgentLifeCycleAdapter {
                String FEATURE_NAME = example.common.ExampleBuildFeature.BUILD_FEATURE_NAME;
            }
        """

        File agentMetaInfDir = createDirectory('agent/src/main/resources/META-INF')
        File agentDefinitionFile = createFile(agentMetaInfDir.toPath(), 'build-agent-plugin-example.xml')
        agentDefinitionFile << """<?xml version="1.0" encoding="UTF-8"?>
            <beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.springframework.org/schema/beans"
                   xsi:schemaLocation="http://www.springframework.org/schema/beans
                                       http://www.springframework.org/schema/beans/spring-beans-3.0.xsd"
                   default-autowire="constructor">
                <bean id="exampleFeature" class="example.agent.ExampleBuildFeature"></bean>
            </beans>
        """

        File serverJavaDir = createDirectory('src/main/java/example/server')
        File serverJavaFile = createFile(serverJavaDir.toPath(), 'ExampleServerPlugin.java')
        serverJavaFile << """
            package example.server;

            import example.common.ExampleBuildFeature;
            import jetbrains.buildServer.serverSide.BuildServerAdapter;

            public class ExampleServerPlugin extends BuildServerAdapter {
                String FEATURE_NAME = ExampleBuildFeature.BUILD_FEATURE_NAME;
            }
        """

        File serverMetaInfDir = createDirectory('src/main/resources/META-INF')
        File serverDefinitionFile = createFile(serverMetaInfDir.toPath(), 'build-server-plugin-example.xml')
        serverDefinitionFile << """<?xml version="1.0" encoding="UTF-8"?>
            <beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.springframework.org/schema/beans"
                   xsi:schemaLocation="http://www.springframework.org/schema/beans
                                       http://www.springframework.org/schema/beans/spring-beans-3.0.xsd"
                   default-autowire="constructor">
                <bean id="examplePlugin" class="example.server.ExampleServerPlugin"></bean>
            </beans>
        """
    }

    @DisplayName('build plugin')
    @ParameterizedTest(name = 'with Gradle {0}')
    @MethodSource("data")
    void 'build plugin'(String gradleVersion) {
        def releasedJavaVersions = releasedJavaVersions()
        def javaVersion = JavaVersion.current().toString()
        assertThat(releasedJavaVersions, hasItem(javaVersion) )

        def supportedJavaVersions = supportedByGradle(gradleVersion)
        assumeTrue(supportedJavaVersions.contains(javaVersion))

        BuildResult result = executeBuild(gradleVersion)
        checkBuild(result)
    }

    private BuildResult executeBuild(String version) {
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments('--warning-mode', 'fail', 'build')
                .withPluginClasspath()
                .withGradleVersion(version)
                .forwardOutput()
                .build()
        return result
    }

    private void checkBuild(BuildResult result) {
        assertThat(result.task(":agent:agentPlugin").getOutcome(), is(SUCCESS))
        assertThat(result.task(":serverPlugin").getOutcome(), is(SUCCESS))

        assertThat(result.getOutput(), not(containsString(NO_DEFINITION_WARNING)))
        assertThat(result.getOutput(), not(containsString('but the implementation class')))
        assertThat(result.getOutput(), not(containsString('archiveName property has been deprecated.')))

        List<String> agentEntries = archiveEntries('agent/build/distributions/test-plugin-agent.zip')
        assertThat(agentEntries, hasItem('teamcity-plugin.xml'))
        assertThat(agentEntries, hasItem('lib/common.jar'))
        assertThat(agentEntries, hasItem('lib/agent.jar'))

        List<String> serverEntries = archiveEntries('build/distributions/test-plugin.zip')
        assertThat(serverEntries, hasItem('agent/test-plugin-agent.zip'))
        assertThat(serverEntries, hasItem('server/common.jar'))
        assertThat(serverEntries, hasItem('server/test-plugin.jar'))
        assertThat(serverEntries, hasItem('teamcity-plugin.xml'))
    }
}
