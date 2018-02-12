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

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SKIPPED
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.MatcherAssert.assertThat

class AgentPluginFunctionalTest {

    static final String BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR = """
        plugins {
            id 'java'
            id 'com.github.rodm.teamcity-agent'
        }
        teamcity {
            version = '8.1.5'
            descriptor {
                pluginDeployment {}
            }
        }
    """

    static final String BUILD_SCRIPT_WITH_FILE_DESCRIPTOR = """
        plugins {
            id 'java'
            id 'com.github.rodm.teamcity-agent'
        }
        teamcity {
            version = '8.1.5'
            descriptor = file(\"\$rootDir/teamcity-plugin.xml\")
        }
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

    static final String NO_DEFINITION_WARNING = TeamCityPlugin.NO_DEFINITION_WARNING_MESSAGE.substring(4)

    static final String NO_BEAN_CLASS_WARNING = TeamCityPlugin.NO_BEAN_CLASS_WARNING_MESSAGE.substring(4)

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    private File buildFile

    @Before
    void setup() throws IOException {
        buildFile = testProjectDir.newFile("build.gradle")
    }

    private BuildResult executeBuild(String... args = ['agentPlugin']) {
        GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(args)
                .withPluginClasspath()
                .build()
    }

    @Test
    void agentPluginBuildAndPackage() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << """
            rootProject.name = 'test-plugin'
        """

        BuildResult result = executeBuild()

        assertThat(result.task(":generateAgentDescriptor").getOutcome(), is(SUCCESS))
        assertThat(result.task(":processAgentDescriptor").getOutcome(), is(SKIPPED))
        assertThat(result.task(":agentPlugin").getOutcome(), is(SUCCESS))

        ZipFile pluginFile = new ZipFile(new File(testProjectDir.root, 'build/distributions/test-plugin-agent.zip'))
        List<String> entries = pluginFile.entries().collect { it.name }
        assertThat(entries, hasItem('teamcity-plugin.xml'))
        assertThat(entries, hasItem('lib/test-plugin.jar'))
    }

    @Test
    void agentPluginWithDescriptorFile() {
        buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR

        File descriptorFile = testProjectDir.newFile("teamcity-plugin.xml")
        descriptorFile << """<?xml version="1.0" encoding="UTF-8"?>
            <teamcity-agent-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                   xsi:noNamespaceSchemaLocation="urn:schemas-jetbrains-com:teamcity-plugin-v1-xml">
                <plugin-deployment use-separate-classloader="true"/>
            </teamcity-agent-plugin>
        """

        BuildResult result = executeBuild()

        assertThat(result.task(":generateAgentDescriptor").getOutcome(), is(SKIPPED))
        assertThat(result.task(":processAgentDescriptor").getOutcome(), is(SUCCESS))
        assertThat(result.task(":agentPlugin").getOutcome(), is(SUCCESS))
        assertThat(result.getOutput(), not(containsString("Plugin descriptor is invalid")))
    }

    @Test
    void agentPluginFailsWithMissingDescriptorFile() {
        buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("agentPlugin")
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.task(":processAgentDescriptor").getOutcome(), is(FAILED))
        assertThat(result.getOutput(), containsString("specified for property 'descriptor' does not exist."))
    }

    @Test
    void invalidAgentPluginDescriptor() {
        buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR

        File descriptorFile = testProjectDir.newFile("teamcity-plugin.xml")
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

        File metaInfDir = testProjectDir.newFolder('src', 'main', 'resources', 'META-INF')
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

        File exampleJavaDir = testProjectDir.newFolder('src', 'main', 'java', 'example')
        File javaFile = new File(exampleJavaDir, 'ExampleBuildFeature.java')
        javaFile << """
            package example;
            public class ExampleBuildFeature {
            }
        """

        File metaInfDir = testProjectDir.newFolder('src', 'main', 'resources', 'META-INF')
        File definitionFile = new File(metaInfDir, 'build-agent-plugin-test.xml')
        definitionFile << PLUGIN_DEFINITION_FILE

        BuildResult result = executeBuild()

        assertThat(result.getOutput(), not(containsString(NO_DEFINITION_WARNING)))
        assertThat(result.getOutput(), not(containsString('but the implementation class example.ExampleBuildFeature was not found in the jar')))
    }


    @Test
    void warningAboutMissingClass() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        File metaInfDir = testProjectDir.newFolder('src', 'main', 'resources', 'META-INF')
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

        File exampleJavaDir = testProjectDir.newFolder('src', 'main', 'java', 'example')
        File javaFile = new File(exampleJavaDir, 'ExampleBuildFeature.java')
        javaFile << """
            package example;
            public class ExampleBuildFeature {
            }
        """

        File metaInfDir = testProjectDir.newFolder('src', 'main', 'resources', 'META-INF')
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
                id 'com.github.rodm.teamcity-agent'
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
                id 'com.github.rodm.teamcity-agent'
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
        File descriptorFile = testProjectDir.newFile("teamcity-plugin.xml")
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
}
