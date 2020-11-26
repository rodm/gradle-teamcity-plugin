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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.util.zip.ZipFile

import static com.github.rodm.teamcity.TestSupport.SETTINGS_SCRIPT_DEFAULT
import static com.github.rodm.teamcity.TestSupport.executeBuild
import static com.github.rodm.teamcity.TestSupport.windowsCompatiblePath
import static com.github.rodm.teamcity.internal.PluginDefinitionValidationAction.NO_BEAN_CLASS_WARNING_MESSAGE
import static com.github.rodm.teamcity.internal.PluginDefinitionValidationAction.NO_DEFINITION_WARNING_MESSAGE
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.SKIPPED
import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.not

class ServerPluginFunctionalTest {

    static final String BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR = """
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
        }
    """

    static final String BUILD_SCRIPT_WITH_FILE_DESCRIPTOR = """
        plugins {
            id 'java'
            id 'com.github.rodm.teamcity-server'
        }
        teamcity {
            version = '8.1.5'
            server {
                descriptor = file(\"\$rootDir/teamcity-plugin.xml\")
            }
        }
    """

    static final String SERVER_DESCRIPTOR_FILE = """<?xml version="1.0" encoding="UTF-8"?>
        <teamcity-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:noNamespaceSchemaLocation="urn:schemas-jetbrains-com:teamcity-plugin-v1-xml">
            <info>
                <name>test-plugin</name>
                <display-name>test-plugin</display-name>
                <version>1.0</version>
            </info>
        </teamcity-plugin>
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

    static final String NO_DEFINITION_WARNING = NO_DEFINITION_WARNING_MESSAGE.substring(4)

    static final String NO_BEAN_CLASS_WARNING = NO_BEAN_CLASS_WARNING_MESSAGE.substring(4)

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    private File buildFile
    private File settingsFile

    @Before
    void setup() throws IOException {
        buildFile = testProjectDir.newFile("build.gradle")
        settingsFile = testProjectDir.newFile('settings.gradle')
    }

    private BuildResult executeBuild(String... args = ['build']) {
        return executeBuild(testProjectDir.root, args)
    }

    @Test
    void serverPluginBuildAndPackage() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR
        settingsFile << SETTINGS_SCRIPT_DEFAULT

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
        descriptorFile << SERVER_DESCRIPTOR_FILE

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
    void 'plugin archive is updated when descriptor property changes'() {
        buildFile << """
            plugins {
                id 'java'
                id 'com.github.rodm.teamcity-server'
            }
            ext {
                pluginVersion = findProperty('plugin.version') ?: '1.2.3'
            }
            teamcity {
                version = '8.1.5'
                server {
                    descriptor {
                        name = 'test-plugin'
                        displayName = 'Test plugin'
                        version = pluginVersion
                        vendorName = 'vendor name'
                    }
                }
            }
        """
        BuildResult result = executeBuild()
        assertThat(result.task(':generateServerDescriptor').getOutcome(), is(SUCCESS))
        assertThat(result.task(':serverPlugin').getOutcome(), is(SUCCESS))
        result = executeBuild()
        assertThat(result.task(':generateServerDescriptor').getOutcome(), is(UP_TO_DATE))
        assertThat(result.task(':serverPlugin').getOutcome(), is(UP_TO_DATE))

        result = executeBuild('serverPlugin', '-Pplugin.version=2018.1.2')

        assertThat(result.task(':generateServerDescriptor').getOutcome(), is(SUCCESS))
        assertThat(result.task(':serverPlugin').getOutcome(), is(SUCCESS))
    }

    @Test
    void 'plugin archive is updated when descriptor token changes'() {
        buildFile << """
            plugins {
                id 'java'
                id 'com.github.rodm.teamcity-server'
            }
            ext {
                pluginVersion = findProperty('plugin.version') ?: '1.2.3'
            }
            teamcity {
                version = '8.1.5'
                server {
                    descriptor = file(\"\$rootDir/teamcity-plugin.xml\")
                    tokens = [PLUGIN_VERSION: pluginVersion]
                }
            }
        """
        File descriptorFile = testProjectDir.newFile("teamcity-plugin.xml")
        descriptorFile << """<?xml version="1.0" encoding="UTF-8"?>
            <teamcity-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:noNamespaceSchemaLocation="urn:schemas-jetbrains-com:teamcity-plugin-v1-xml">
                <info>
                    <name>test-plugin</name>
                    <display-name>test-plugin</display-name>
                    <version>@PLUGIN_VERSION@</version>
                </info>
            </teamcity-plugin>
        """

        BuildResult result = executeBuild()
        assertThat(result.task(':processServerDescriptor').getOutcome(), is(SUCCESS))
        assertThat(result.task(':serverPlugin').getOutcome(), is(SUCCESS))
        result = executeBuild()
        assertThat(result.task(':processServerDescriptor').getOutcome(), is(UP_TO_DATE))
        assertThat(result.task(':serverPlugin').getOutcome(), is(UP_TO_DATE))

        result = executeBuild('serverPlugin', '-Pplugin.version=2018.1.2')

        assertThat(result.task(':processServerDescriptor').getOutcome(), is(SUCCESS))
        assertThat(result.task(':serverPlugin').getOutcome(), is(SUCCESS))
    }

    @Test
    void 'generate server descriptor task should be cacheable'() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR
        settingsFile << """
            rootProject.name = 'test-plugin'

            buildCache {
                local {
                    directory = new File('${windowsCompatiblePath(testProjectDir.root)}', 'build-cache')
                }
            }
        """

        BuildResult result
        result = executeBuild('--build-cache', 'clean', 'assemble')
        assertThat(result.task(":generateServerDescriptor").getOutcome(), is(SUCCESS))

        result = executeBuild('--build-cache', 'clean', 'assemble')
        assertThat(result.task(":generateServerDescriptor").getOutcome(), is(FROM_CACHE))
    }

    @Test
    void 'process server descriptor task should be cacheable'() {
        buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR
        settingsFile << """
            rootProject.name = 'test-plugin'

            buildCache {
                local {
                    directory = new File('${windowsCompatiblePath(testProjectDir.root)}', 'build-cache')
                }
            }
        """
        File descriptorFile = testProjectDir.newFile("teamcity-plugin.xml")
        descriptorFile << SERVER_DESCRIPTOR_FILE

        BuildResult result
        result = executeBuild('--build-cache', 'clean', 'assemble')
        assertThat(result.task(":processServerDescriptor").getOutcome(), is(SUCCESS))

        result = executeBuild('--build-cache', 'clean', 'assemble')
        assertThat(result.task(":processServerDescriptor").getOutcome(), is(FROM_CACHE))
    }

    @Test
    void 'plugin archive contains 3rd party libraries but not TeamCity libraries'() {
        buildFile << """
            plugins {
                id 'org.gradle.java'
                id 'com.github.rodm.teamcity-server'
            }
            dependencies {
                implementation 'org.apache.commons:commons-lang3:3.7'
                runtimeOnly 'org.apache.commons:commons-math:2.2'
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
            }
        """
        settingsFile << SETTINGS_SCRIPT_DEFAULT

        BuildResult result = executeBuild()

        assertThat(result.task(":serverPlugin").getOutcome(), is(SUCCESS))

        ZipFile pluginArchive = new ZipFile(new File(testProjectDir.root, 'build/distributions/test-plugin.zip'))
        List<String> entries = pluginArchive.entries().collect { it.name }
        assertThat(entries, hasItem('teamcity-plugin.xml'))
        assertThat(entries, hasItem('server/'))
        assertThat(entries, hasItem('server/test-plugin.jar'))
        assertThat(entries, hasItem('server/commons-lang3-3.7.jar'))
        assertThat(entries, hasItem('server/commons-math-2.2.jar'))
        assertThat(entries, not(hasItem('server/server-api-8.1.5.jar')))
        assertThat(entries.size(), equalTo(5))
    }

    @Test
    void 'plugin archive includes additional files'() {
        buildFile << """
            plugins {
                id 'org.gradle.java'
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
                    files {
                        into('files') {
                            from('srcdir')
                        }
                    }
                }
            }
        """
        settingsFile << SETTINGS_SCRIPT_DEFAULT

        File srcdir = testProjectDir.newFolder('srcdir')
        File file1 = new File(srcdir, 'file1')
        file1 << "file1"
        File file2 = new File(srcdir, 'file2')
        file2 << "file2"

        BuildResult result = executeBuild()

        assertThat(result.task(":serverPlugin").getOutcome(), is(SUCCESS))

        ZipFile pluginArchive = new ZipFile(new File(testProjectDir.root, 'build/distributions/test-plugin.zip'))
        List<String> entries = pluginArchive.entries().collect { it.name }
        assertThat(entries, hasItem('files/'))
        assertThat(entries, hasItem('files/file1'))
        assertThat(entries, hasItem('files/file2'))
    }

    @Test
    void 'jar archive contains additional frontend javascript in buildServerResources'() {
        buildFile << """
            plugins {
                id 'org.gradle.java'
                id 'com.github.rodm.teamcity-server'
            }
            teamcity {
                version = '2020.2'
                server {
                    descriptor {
                        name = 'test-plugin'
                        displayName = 'Test plugin'
                        version = '1.0'
                        vendorName = 'vendor name'
                    }
                    web {
                        project.files('bundle.js')
                    }
                }
            }
        """
        settingsFile << SETTINGS_SCRIPT_DEFAULT
        testProjectDir.newFile("bundle.js")

        BuildResult result = executeBuild()

        assertThat(result.task(":serverPlugin").getOutcome(), is(SUCCESS))

        ZipFile archive = new ZipFile(new File(testProjectDir.root, 'build/libs/test-plugin.jar'))
        List<String> entries = archive.entries().collect { it.name }
        assertThat(entries, hasItem('buildServerResources/'))
        assertThat(entries, hasItem('buildServerResources/bundle.js'))
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
}
