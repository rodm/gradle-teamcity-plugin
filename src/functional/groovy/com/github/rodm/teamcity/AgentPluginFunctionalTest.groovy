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
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.github.rodm.teamcity.TestSupport.executeBuildAndFail
import static com.github.rodm.teamcity.TestSupport.SETTINGS_SCRIPT_DEFAULT
import static com.github.rodm.teamcity.internal.PluginDefinitionValidationAction.NO_BEAN_CLASS_WARNING_MESSAGE
import static com.github.rodm.teamcity.internal.PluginDefinitionValidationAction.NO_DEFINITION_WARNING_MESSAGE
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SKIPPED
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.MatcherAssert.assertThat

class AgentPluginFunctionalTest extends FunctionalTestCase {

    static final String BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR = """
        plugins {
            id 'java'
            id 'io.github.rodm.teamcity-agent'
        }
        teamcity {
            version = '8.1.5'
            agent {
                descriptor {
                    pluginDeployment {}
                }
            }
        }
    """

    static final String BUILD_SCRIPT_WITH_FILE_DESCRIPTOR = """
        plugins {
            id 'java'
            id 'io.github.rodm.teamcity-agent'
        }
        teamcity {
            version = '8.1.5'
            agent {
                descriptor = file(\"\$rootDir/teamcity-plugin.xml\")
            }
        }
    """

    static final String AGENT_DESCRIPTOR_FILE = """<?xml version="1.0" encoding="UTF-8"?>
        <teamcity-agent-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                               xsi:noNamespaceSchemaLocation="urn:schemas-jetbrains-com:teamcity-plugin-v1-xml">
            <plugin-deployment use-separate-classloader="true"/>
        </teamcity-agent-plugin>
    """

    static final String PLUGIN_DEFINITION_FILE = """<?xml version="1.0" encoding="UTF-8"?>
        <beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xmlns="http://www.springframework.org/schema/beans"
               xsi:schemaLocation="http://www.springframework.org/schema/beans
                                   http://www.springframework.org/schema/beans/spring-beans-3.0.xsd"
               default-autowire="constructor">
            <bean id="exampleFeature" class="example.ExampleBuildFeature"></bean>
        </beans>
    """

    static final String NO_DEFINITION_WARNING = NO_DEFINITION_WARNING_MESSAGE.substring(4)

    static final String NO_BEAN_CLASS_WARNING = NO_BEAN_CLASS_WARNING_MESSAGE.substring(4)

    @Test
    void agentPluginBuildAndPackage() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR
        settingsFile << SETTINGS_SCRIPT_DEFAULT

        BuildResult result = executeBuild()

        assertThat(result.task(":generateAgentDescriptor").getOutcome(), is(SUCCESS))
        assertThat(result.task(":processAgentDescriptor").getOutcome(), is(SKIPPED))
        assertThat(result.task(":agentPlugin").getOutcome(), is(SUCCESS))

        List<String> entries = archiveEntries('build/distributions/test-plugin.zip')
        assertThat(entries, hasItem('teamcity-plugin.xml'))
        assertThat(entries, hasItem('lib/test-plugin.jar'))
    }

    @Test
    void agentPluginWithDescriptorFile() {
        buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR
        File descriptorFile = createFile("teamcity-plugin.xml")
        descriptorFile << AGENT_DESCRIPTOR_FILE

        BuildResult result = executeBuild()

        assertThat(result.task(":generateAgentDescriptor").getOutcome(), is(SKIPPED))
        assertThat(result.task(":processAgentDescriptor").getOutcome(), is(SUCCESS))
        assertThat(result.task(":agentPlugin").getOutcome(), is(SUCCESS))
        assertThat(result.getOutput(), not(containsString("Plugin descriptor is invalid")))
    }

    @Test
    void agentPluginFailsWithMissingDescriptorFile() {
        buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR

        BuildResult result = executeBuildAndFail(testProjectDir.toFile(), 'agentPlugin')

        assertThat(result.task(":processAgentDescriptor").getOutcome(), is(FAILED))
        if (GradleVersion.current() < GradleVersion.version('7.0')) {
            assertThat(result.getOutput(), containsString("specified for property 'descriptor' does not exist."))
        } else {
            assertThat(result.getOutput(), containsString("property 'descriptor' specifies file"))
            assertThat(result.getOutput(), containsString("which doesn't exist."))
        }
    }

    @Test
    void invalidAgentPluginDescriptor() {
        buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR

        File descriptorFile = createFile("teamcity-plugin.xml")
        descriptorFile << """<?xml version="1.0" encoding="UTF-8"?>
            <teamcity-agent-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:noNamespaceSchemaLocation="urn:schemas-jetbrains-com:teamcity-plugin-v1-xml">
            </teamcity-agent-plugin>
        """

        BuildResult result = executeBuild()

        assertThat(result.getOutput(), containsString("Plugin descriptor is invalid"))
    }

    @Test
    void agentPluginNoWarningsWithDefinitionFile() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        File metaInfDir = createDirectory('src/main/resources/META-INF')
        File definitionFile = new File(metaInfDir, 'build-agent-plugin-example.xml')
        definitionFile << '<beans></beans>'

        BuildResult result = executeBuild()

        assertThat(result.getOutput(), not(containsString(NO_DEFINITION_WARNING)))
    }

    @Test
    void agentPluginWarnsAboutMissingDefinitionFile() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        BuildResult result = executeBuild()

        assertThat(result.getOutput(), containsString(NO_DEFINITION_WARNING))
    }

    @Test
    void noWarningWithValidDefinitionFile() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        File exampleJavaDir = createDirectory('src/main/java/example')
        File javaFile = new File(exampleJavaDir, 'ExampleBuildFeature.java')
        javaFile << """
            package example;
            public class ExampleBuildFeature {
            }
        """

        File metaInfDir = createDirectory('src/main/resources/META-INF')
        File definitionFile = new File(metaInfDir, 'build-agent-plugin-test.xml')
        definitionFile << PLUGIN_DEFINITION_FILE

        BuildResult result = executeBuild()

        assertThat(result.getOutput(), not(containsString(NO_DEFINITION_WARNING)))
        assertThat(result.getOutput(), not(containsString('but the implementation class example.ExampleBuildFeature was not found in the jar')))
    }

    @Test
    void warningAboutMissingClass() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        File metaInfDir = createDirectory('src/main/resources/META-INF')
        File definitionFile = new File(metaInfDir, 'build-agent-plugin-test.xml')
        definitionFile << PLUGIN_DEFINITION_FILE

        BuildResult result = executeBuild()

        assertThat(result.getOutput(), not(containsString(NO_DEFINITION_WARNING)))
        String expectedWarning = String.format(NO_BEAN_CLASS_WARNING, 'build-agent-plugin-test.xml', 'example.ExampleBuildFeature')
        assertThat(result.getOutput(), containsString(expectedWarning))
    }

    @Test
    void supportOlderPluginDefinitionFile() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        File exampleJavaDir = createDirectory('src/main/java/example')
        File javaFile = new File(exampleJavaDir, 'ExampleBuildFeature.java')
        javaFile << """
            package example;
            public class ExampleBuildFeature {
            }
        """

        File metaInfDir = createDirectory('src/main/resources/META-INF')
        File definitionFile = new File(metaInfDir, 'build-agent-plugin-test.xml')
        definitionFile << """<?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN//EN" "http://www.springframework.org/dtd/spring-beans.dtd">
            <beans default-autowire="constructor">
                <bean id="exampleFeature" class="example.ExampleBuildFeature"/>
            </beans>
        """

        BuildResult result = executeBuild()

        assertThat(result.getOutput(), not(containsString(NO_DEFINITION_WARNING)))
        assertThat(result.getOutput(), not(containsString('but the implementation class example.ExampleBuildFeature was not found in the jar')))
    }

    @Test
    void 'plugin archive is updated when descriptor property changes'() {
        buildFile << """
            plugins {
                id 'java'
                id 'io.github.rodm.teamcity-agent'
            }
            ext {
                executablePath = findProperty('exec.path') ?: 'file'
            }
            teamcity {
                version = '8.1.5'
                agent {
                    descriptor {
                        pluginDeployment {
                            executableFiles {
                                include executablePath
                            }
                        }
                    }
                }
            }
        """
        BuildResult result = executeBuild()
        assertThat(result.task(':generateAgentDescriptor').getOutcome(), is(SUCCESS))
        assertThat(result.task(':agentPlugin').getOutcome(), is(SUCCESS))
        result = executeBuild()
        assertThat(result.task(':generateAgentDescriptor').getOutcome(), is(UP_TO_DATE))
        assertThat(result.task(':agentPlugin').getOutcome(), is(UP_TO_DATE))

        result = executeBuild('agentPlugin', '-Pexec.path=file2')

        assertThat(result.task(':generateAgentDescriptor').getOutcome(), is(SUCCESS))
        assertThat(result.task(':agentPlugin').getOutcome(), is(SUCCESS))
    }

    @Test
    void 'plugin archive is updated when descriptor token changes'() {
        buildFile << """
            plugins {
                id 'java'
                id 'io.github.rodm.teamcity-agent'
            }
            ext {
                executablePath = findProperty('exec.path') ?: 'file'
            }
            teamcity {
                version = '8.1.5'
                agent {
                    descriptor = file(\"\$rootDir/teamcity-plugin.xml\")
                    tokens = [PATH: executablePath]
                }
            }
        """
        File descriptorFile = createFile("teamcity-plugin.xml")
        descriptorFile << """<?xml version="1.0" encoding="UTF-8"?>
            <teamcity-agent-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:noNamespaceSchemaLocation="urn:schemas-jetbrains-com:teamcity-agent-plugin-v1-xml">
                <plugin-deployment>
                    <layout>
                        <executable-files>
                            <include name="@PATH@"/>
                        </executable-files>
                    </layout>
                </plugin-deployment>
            </teamcity-agent-plugin>
        """
        BuildResult result = executeBuild()
        assertThat(result.task(':processAgentDescriptor').getOutcome(), is(SUCCESS))
        assertThat(result.task(':agentPlugin').getOutcome(), is(SUCCESS))
        result = executeBuild()
        assertThat(result.task(':processAgentDescriptor').getOutcome(), is(UP_TO_DATE))
        assertThat(result.task(':agentPlugin').getOutcome(), is(UP_TO_DATE))

        result = executeBuild('agentPlugin', '-Pexec.path=file2')

        assertThat(result.task(':processAgentDescriptor').getOutcome(), is(SUCCESS))
        assertThat(result.task(':agentPlugin').getOutcome(), is(SUCCESS))
    }

    @Test
    void 'plugin archive contains 3rd party libraries but not TeamCity libraries'() {
        buildFile << """
            plugins {
                id 'org.gradle.java'
                id 'io.github.rodm.teamcity-agent'
            }
            dependencies {
                implementation 'org.apache.commons:commons-lang3:3.7'
                runtimeOnly 'org.apache.commons:commons-math:2.2'
            }
            teamcity {
                version = '8.1.5'
                agent {
                    archiveName = 'test-plugin-agent.zip'
                    descriptor {
                        pluginDeployment {}
                    }
                }
            }
        """
        settingsFile << SETTINGS_SCRIPT_DEFAULT

        BuildResult result = executeBuild()

        assertThat(result.task(":agentPlugin").getOutcome(), is(SUCCESS))

        List<String> entries = archiveEntries('build/distributions/test-plugin-agent.zip')
        assertThat(entries, hasItem('teamcity-plugin.xml'))
        assertThat(entries, hasItem('lib/'))
        assertThat(entries, hasItem('lib/test-plugin.jar'))
        assertThat(entries, hasItem('lib/commons-lang3-3.7.jar'))
        assertThat(entries, hasItem('lib/commons-math-2.2.jar'))
        assertThat(entries, not(hasItem('lib/agent-api-8.1.5.jar')))
        assertThat(entries.size(), equalTo(5))
    }

    @Test
    void 'agentPlugin is skipped if project is an agent-side library'() {
        buildFile << """
            plugins {
                id 'org.gradle.java'
                id 'io.github.rodm.teamcity-agent'
            }
            teamcity {
                version = '2020.2'
            }
        """
        settingsFile << SETTINGS_SCRIPT_DEFAULT

        BuildResult result = executeBuild()

        assertThat(result.task(":generateAgentDescriptor").getOutcome(), is(SKIPPED))
        assertThat(result.task(":processAgentDescriptor").getOutcome(), is(SKIPPED))
        assertThat(result.task(":agentPlugin").getOutcome(), is(SKIPPED))
    }

    @Nested
    @DisplayName("with build cache")
    class WithBuildCache {

        @BeforeEach
        void setupWithLocalBuildCache() {
            settingsFile << """
            rootProject.name = 'test-plugin'

            buildCache {
                local {
                    directory = '${testProjectDir.resolve('local-cache').toUri()}'
                }
            }
            """
        }

        @Test
        void 'generate agent descriptor task should be cacheable'() {
            buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

            BuildResult result
            result = executeBuild('--build-cache', 'clean', 'assemble')
            assertThat(result.task(":generateAgentDescriptor").getOutcome(), is(SUCCESS))

            result = executeBuild('--build-cache', 'clean', 'assemble')
            assertThat(result.task(":generateAgentDescriptor").getOutcome(), is(FROM_CACHE))
        }

        @Test
        void 'process agent descriptor task should be cacheable'() {
            buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR
            createFile("teamcity-plugin.xml") << AGENT_DESCRIPTOR_FILE

            BuildResult result
            result = executeBuild('--build-cache', 'clean', 'assemble')
            assertThat(result.task(":processAgentDescriptor").getOutcome(), is(SUCCESS))

            result = executeBuild('--build-cache', 'clean', 'assemble')
            assertThat(result.task(":processAgentDescriptor").getOutcome(), is(FROM_CACHE))
        }
    }

    @Nested
    @DisplayName("with configuration cache")
    class WithConfigurationCache {

        @Test
        void 'check generateAgentDescriptor task can be loaded from the configuration cache'() {
            buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

            executeBuild('--configuration-cache', 'generateAgentDescriptor')
            BuildResult result = executeBuild('--configuration-cache', 'generateAgentDescriptor')
            assertThat(result.output, containsString('Reusing configuration cache.'))
        }

        @Test
        void 'check processAgentDescriptor task can be loaded from the configuration cache'() {
            buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR
            createFile("teamcity-plugin.xml") << AGENT_DESCRIPTOR_FILE

            executeBuild('--configuration-cache', 'processAgentDescriptor')
            BuildResult result = executeBuild('--configuration-cache', 'processAgentDescriptor')
            assertThat(result.output, containsString('Reusing configuration cache.'))
        }

        @Test
        void 'check agentPlugin task can be loaded from the configuration cache'() {
            buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

            executeBuild('--configuration-cache', 'agentPlugin')
            BuildResult result = executeBuild('--configuration-cache', 'agentPlugin')
            assertThat(result.output, containsString('Reusing configuration cache.'))
        }
    }
}
