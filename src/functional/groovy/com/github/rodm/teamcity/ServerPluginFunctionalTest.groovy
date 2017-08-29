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

import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.not
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.SKIPPED
import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ServerPluginFunctionalTest {

    static final String BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR = """
        plugins {
            id 'java'
            id 'com.github.rodm.teamcity-server'
        }
        teamcity {
            version = '8.1.5'
            descriptor {
                name = 'test-plugin'
                displayName = 'Test plugin'
                version = '1.0'
                vendorName = 'vendor name'
            }
        }
    """

    static final String BUILD_SCRIPT_WITH_FILE_DESCRIPTOR = """
        plugins {
            id 'java'
            id 'com.github.rodm.teamcity-server'
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
            <bean id="examplePlugin" class="example.ExampleServerPlugin"></bean>
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

    private BuildResult executeBuild(String... args = ['serverPlugin']) {
        GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(args)
                .withPluginClasspath()
                .build()
    }

    @Test
    void serverPluginBuildAndPackage() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << """
            rootProject.name = 'test-plugin'
        """

        BuildResult result = executeBuild()

        assertThat(result.task(":generateServerDescriptor").getOutcome(), is(SUCCESS))
        assertThat(result.task(":processServerDescriptor").getOutcome(), is(SKIPPED))
        assertThat(result.task(":serverPlugin").getOutcome(), is(SUCCESS))

        ZipFile pluginFile = new ZipFile(new File(testProjectDir.root, 'build/distributions/test-plugin.zip'))
        List<String> entries = pluginFile.entries().collect { it.name }
        assertThat(entries, hasItem('teamcity-plugin.xml'))
        assertThat(entries, hasItem('server/test-plugin.jar'))
    }

    @Test
    void serverPluginWithDescriptorFile() {
        buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR

        File descriptorFile = testProjectDir.newFile("teamcity-plugin.xml")
        descriptorFile << """<?xml version="1.0" encoding="UTF-8"?>
            <teamcity-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:noNamespaceSchemaLocation="urn:schemas-jetbrains-com:teamcity-plugin-v1-xml">
                <info>
                    <name>test-plugin</name>
                    <display-name>test-plugin</display-name>
                    <version>1.0</version>
                </info>
            </teamcity-plugin>
        """

        BuildResult result = executeBuild()

        assertThat(result.task(":generateServerDescriptor").getOutcome(), is(SKIPPED))
        assertThat(result.task(":processServerDescriptor").getOutcome(), is(SUCCESS))
        assertThat(result.task(":serverPlugin").getOutcome(), is(SUCCESS))
        assertThat(result.getOutput(), not(containsString("Plugin descriptor is invalid")))
    }

    @Test
    void serverPluginNoWarningsWithDefinitionFile() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        File metaInfDir = testProjectDir.newFolder('src', 'main', 'resources', 'META-INF')
        File definitionFile = new File(metaInfDir, 'build-server-plugin-example.xml')
        definitionFile << '<beans></beans>'

        BuildResult result = executeBuild()

        assertThat(result.getOutput(), not(containsString(NO_DEFINITION_WARNING)))
    }

    @Test
    void serverPluginWarnsAboutMissingDefinitionFile() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        BuildResult result = executeBuild()

        assertThat(result.getOutput(), containsString(NO_DEFINITION_WARNING))
    }

    @Test
    void noWarningWithValidDefinitionFile() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        File exampleJavaDir = testProjectDir.newFolder('src', 'main', 'java', 'example')
        File javaFile = new File(exampleJavaDir, 'ExampleServerPlugin.java')
        javaFile << """
            package example;
            public class ExampleServerPlugin {
            }
        """

        File metaInfDir = testProjectDir.newFolder('src', 'main', 'resources', 'META-INF')
        File definitionFile = new File(metaInfDir, 'build-server-plugin-test.xml')
        definitionFile << PLUGIN_DEFINITION_FILE

        BuildResult result = executeBuild()

        assertThat(result.getOutput(), not(containsString(NO_DEFINITION_WARNING)))
        assertThat(result.getOutput(), not(containsString('but the implementation class example.ExampleServerPlugin was not found in the jar')))
    }

    @Test
    void warningAboutMissingClass() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        File metaInfDir = testProjectDir.newFolder('src', 'main', 'resources', 'META-INF')
        File definitionFile = new File(metaInfDir, 'build-server-plugin-test.xml')
        definitionFile << PLUGIN_DEFINITION_FILE

        BuildResult result = executeBuild()

        assertThat(result.getOutput(), not(containsString(NO_DEFINITION_WARNING)))
        String expectedWarning = String.format(NO_BEAN_CLASS_WARNING, 'build-server-plugin-test.xml', 'example.ExampleServerPlugin')
        assertThat(result.getOutput(), containsString(expectedWarning))
    }

    @Test
    void supportOlderPluginDefinitionFile() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        File exampleJavaDir = testProjectDir.newFolder('src', 'main', 'java', 'example')
        File javaFile = new File(exampleJavaDir, 'ExampleServerPlugin.java')
        javaFile << """
            package example;
            public class ExampleServerPlugin {
            }
        """

        File metaInfDir = testProjectDir.newFolder('src', 'main', 'resources', 'META-INF')
        File definitionFile = new File(metaInfDir, 'build-server-plugin-test.xml')
        definitionFile << """<?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN//EN" "http://www.springframework.org/dtd/spring-beans.dtd">
            <beans default-autowire="constructor">
                <bean id="examplePlugin" class="example.ExampleServerPlugin"/>
            </beans>
        """

        BuildResult result = executeBuild()

        assertThat(result.getOutput(), not(containsString(NO_DEFINITION_WARNING)))
        assertThat(result.getOutput(), not(containsString('but the implementation class example.ExampleServerPlugin was not found in the jar')))
    }

    @Test
    void serverPluginFailsWithMissingDescriptorFile() {
        buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("serverPlugin")
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.task(":processServerDescriptor").getOutcome(), is(FAILED))
        assertThat(result.getOutput(), containsString("specified for property 'descriptor' does not exist."))
    }

    @Test
    void invalidServerPluginDescriptor() {
        buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR

        File descriptorFile = testProjectDir.newFile("teamcity-plugin.xml")
        descriptorFile << """<?xml version="1.0" encoding="UTF-8"?>
            <teamcity-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:noNamespaceSchemaLocation="urn:schemas-jetbrains-com:teamcity-plugin-v1-xml">
                <info>
                    <name>test-plugin</name>
                </info>
            </teamcity-plugin>
        """

        BuildResult result = executeBuild()

        assertThat(result.getOutput(), containsString("Plugin descriptor is invalid"))
    }

    @Test
    void simulateBuildingPluginInTeamCity() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        // Provides similar functionality to the init.gradle script used by the TeamCity Gradle Runner plugin
        File initFile = testProjectDir.newFile('init.gradle')
        initFile << """
            public class DummyTeamcityPropertiesListener implements ProjectEvaluationListener {
                void beforeEvaluate(Project project) {
                    def props = [:]
                    if (project.hasProperty("ext")) {
                        project.ext.teamcity = props
                    } else {
                        project.setProperty("teamcity", props)
                    }
                }
                void afterEvaluate(Project project, ProjectState projectState) {
                }
            }
            gradle.addListener(new DummyTeamcityPropertiesListener())
        """

        BuildResult result = executeBuild('--init-script', 'init.gradle', '--refresh-dependencies', 'serverPlugin')

        assertThat(result.task(":serverPlugin").getOutcome(), is(SUCCESS))
    }

    @Test
    void startServerAfterDeployingPlugin() {
        File homeDir = createFakeTeamCityInstall('teamcity', '9.1.6')
        File dataDir = testProjectDir.newFolder('data')

        buildFile << """
            plugins {
                id 'java'
                id 'com.github.rodm.teamcity-server'
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
        assertThat(result.output, not(containsString('deprecated')))
    }

    @Test
    void startNamedEnvironmentServer() {
        createFakeTeamCityInstall('teamcity', '9.1.6')

        buildFile << """
            plugins {
                id 'java'
                id 'com.github.rodm.teamcity-server'
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
        assertThat(result.output, not(containsString('deprecated')))
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
        assertThat(result.output, not(containsString('deprecated')))
    }

    @Test
    void 'deploy and undeploy multiple plugins to and from an environment'() {
        buildFile << """
            plugins {
                id 'java'
                id 'com.github.rodm.teamcity-server'
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
