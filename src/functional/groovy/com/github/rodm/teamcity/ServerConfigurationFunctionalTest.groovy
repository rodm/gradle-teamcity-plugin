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
import static org.junit.Assert.assertEquals
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.SKIPPED
import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.junit.Assert.assertTrue

public class ServerConfigurationFunctionalTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    private File buildFile

    private String pluginClasspath

    @Before
    public void setup() throws IOException {
        buildFile = testProjectDir.newFile("build.gradle")

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        pluginClasspath = pluginClasspathResource.readLines()
                .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { "'$it'" }
                .join(", ")
    }

    @Test
    public void serverPluginBuildAndPackage() {
        buildFile << """
            buildscript {
                dependencies {
                    classpath files(${pluginClasspath})
                }
            }
            apply plugin: 'java'
            apply plugin: 'com.github.rodm.teamcity-server'
            teamcity {
                version = '8.1.5'
                descriptor {
                }
            }
        """

        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << """
            rootProject.name = 'test-plugin'
        """

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("serverPlugin")
                .build();

        assertEquals(result.task(":generateServerDescriptor").getOutcome(), SUCCESS)
        assertEquals(result.task(":processServerDescriptor").getOutcome(), SKIPPED)
        assertEquals(result.task(":serverPlugin").getOutcome(), SUCCESS)

        ZipFile pluginFile = new ZipFile(new File(testProjectDir.root, 'build/distributions/test-plugin.zip'))
        List<String> entries = pluginFile.entries().collect { it.name }
        assertThat(entries, hasItem('teamcity-plugin.xml'))
        assertThat(entries, hasItem('server/test-plugin.jar'))
    }

    @Test
    public void serverPluginWithDescriptorFile() {
        buildFile << """
            buildscript {
                dependencies {
                    classpath files(${pluginClasspath})
                }
            }
            apply plugin: 'java'
            apply plugin: 'com.github.rodm.teamcity-server'
            teamcity {
                version = '8.1.5'
                descriptor = file(\"\$rootDir/teamcity-plugin.xml\")
            }
        """

        File descriptorFile = testProjectDir.newFile("teamcity-plugin.xml");
        descriptorFile << """
            <?xml version="1.0" encoding="UTF-8"?>
            <teamcity-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:noNamespaceSchemaLocation="urn:schemas-jetbrains-com:teamcity-plugin-v1-xml">
                <info>
                    <name>test-plugin</name>
                </info>
            </teamcity-plugin>
        """

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("serverPlugin")
                .build();

        assertEquals(result.task(":generateServerDescriptor").getOutcome(), SKIPPED)
        assertEquals(result.task(":processServerDescriptor").getOutcome(), SUCCESS)
        assertEquals(result.task(":serverPlugin").getOutcome(), SUCCESS)
    }

    @Test
    public void serverPluginFailsWithMissingDescriptorFile() {
        buildFile << """
            buildscript {
                dependencies {
                    classpath files(${pluginClasspath})
                }
            }
            apply plugin: 'java'
            apply plugin: 'com.github.rodm.teamcity-server'
            teamcity {
                version = '8.1.5'
                descriptor = file(\"\$rootDir/teamcity-plugin.xml\")
            }
        """

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("serverPlugin")
                .buildAndFail()

        assertThat(result.task(":processServerDescriptor").getOutcome(), is(FAILED))
        assertThat(result.getOutput(), containsString("specified for property 'descriptor' does not exist."))
    }

    @Test
    public void simulateBuildingPluginInTeamCity() {
        buildFile << """
            buildscript {
                dependencies {
                    classpath files(${pluginClasspath})
                }
            }
            apply plugin: 'java'
            apply plugin: 'com.github.rodm.teamcity-server'
            teamcity {
                version = '8.1.5'
                descriptor {
                }
            }
        """

        // Provides similar functionality to the init.gradle script used by the TeamCity Gradle Runner plugin
        File initFile = testProjectDir.newFile('init.gradle')
        initFile << """
            public class DummyTeamcityPropertiesListener implements ProjectEvaluationListener {
                public void beforeEvaluate(Project project) {
                    def props = [:]
                    if (project.hasProperty("ext")) {
                        project.ext.teamcity = props
                    } else {
                        project.setProperty("teamcity", props)
                    }
                }
                public void afterEvaluate(Project project, ProjectState projectState) {
                }
            }
            gradle.addListener(new DummyTeamcityPropertiesListener())
        """

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("--init-script", "init.gradle", "--refresh-dependencies", "serverPlugin")
                .withDebug(true)
                .build()

        assertEquals(result.task(":serverPlugin").getOutcome(), SUCCESS)
    }

    @Test
    public void startServerAfterDeployingPlugin() {
        File homeDir = createFakeTeamCityInstall('teamcity', '9.1.6')
        File dataDir = testProjectDir.newFolder('data')

        buildFile << """
            buildscript {
                dependencies {
                    classpath files(${pluginClasspath})
                }
            }
            apply plugin: 'java'
            apply plugin: 'com.github.rodm.teamcity-server'
            teamcity {
                version = '8.1.5'
                server {
                    descriptor {
                    }
                    environments {
                        teamcity {
                            homeDir = file('${homeDir.canonicalPath}')
                            dataDir = file('${dataDir.canonicalPath}')
                            javaHome = file('${dataDir.canonicalPath}')
                        }
                    }
                }
            }
        """

        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << """
            rootProject.name = 'test-plugin'
        """

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("build", "startTeamcityServer")
                .build()

        File pluginFile = new File(dataDir, 'plugins/test-plugin.zip')
        assertTrue('Plugin archive not deployed', pluginFile.exists())
        assertThat(result.task(":deployPluginToTeamcity").getOutcome(), is(SUCCESS))
        assertThat(result.task(":startTeamcityServer").getOutcome(), is(SUCCESS))
    }

    @Test
    public void tasksForMultipleEnvironmentSupport() {
        buildFile << """
            buildscript {
                dependencies {
                    classpath files(${pluginClasspath})
                }
            }
            apply plugin: 'java'
            apply plugin: 'com.github.rodm.teamcity-server'
            teamcity {
                version = '8.1.5'
                server {
                    descriptor {
                    }
                    environments {
                        teamcity {
                        }
                    }
                }
            }
        """

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("tasks")
                .build()

        assertThat(result.output, containsString('deployPluginToTeamcity'))
        assertThat(result.output, containsString('undeployPluginFromTeamcity'))
        assertThat(result.output, containsString('startTeamcityServer'))
        assertThat(result.output, containsString('stopTeamcityServer'))
        assertThat(result.output, containsString('startTeamcityAgent'))
        assertThat(result.output, containsString('stopTeamcityAgent'))
    }

    @Test
    public void startNamedEnvironmentServer() {
        createFakeTeamCityInstall('teamcity', '9.1.6')

        buildFile << """
            buildscript {
                dependencies {
                    classpath files(${pluginClasspath})
                }
            }
            apply plugin: 'java'
            apply plugin: 'com.github.rodm.teamcity-server'
            teamcity {
                version = '8.1.5'
                server {
                    descriptor {
                    }
                    baseHomeDir = 'teamcity'
                    baseDataDir = 'teamcity/data'
                    environments {
                        teamcity8 {
                            version = '8.1.5'
                        }
                        teamcity9 {
                            version = '9.1.6'
                        }
                    }
                }
            }
        """

        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << """
            rootProject.name = 'test-plugin'
        """

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments('build', 'startTeamcity9Server')
                .build()

        File pluginFile = new File(testProjectDir.root, 'teamcity/data/9.1/plugins/test-plugin.zip')
        assertTrue('Plugin archive not deployed', pluginFile.exists())
        assertThat(result.task(":startTeamcity9Server").getOutcome(), is(SUCCESS))
    }

    @Rule
    public final TemporaryFolder serversDir = new TemporaryFolder()

    @Test
    public void startNamedEnvironmentServerInAlternativeDirectory() {
        createFakeTeamCityInstall(serversDir, 'teamcity', '9.1.6')

        buildFile << """
            buildscript {
                dependencies {
                    classpath files(${pluginClasspath})
                }
            }
            apply plugin: 'java'
            apply plugin: 'com.github.rodm.teamcity-server'
            teamcity {
                version = '8.1.5'
                server {
                    descriptor {
                    }
                    baseHomeDir = file('${serversDir.root.canonicalPath}/teamcity')
                    baseDataDir = file('${serversDir.root.canonicalPath}/data')
                    environments {
                        teamcity8 {
                            version = '8.1.5'
                        }
                        teamcity9 {
                            version = '9.1.6'
                        }
                    }
                }
            }
        """

        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << """
            rootProject.name = 'test-plugin'
        """

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments('build', 'startTeamcity9Server')
                .build()

        File pluginFile = new File(serversDir.root, 'data/9.1/plugins/test-plugin.zip')
        assertTrue('Plugin archive not deployed', pluginFile.exists())
        assertThat(result.task(":startTeamcity9Server").getOutcome(), is(SUCCESS))
    }

    private File createFakeTeamCityInstall(String baseDir, String version) {
        createFakeTeamCityInstall(testProjectDir, baseDir, version)
    }

    private File createFakeTeamCityInstall(TemporaryFolder folder, String baseDir, String version) {
        File homeDir = folder.newFolder(baseDir, "TeamCity-${version}")
        File binDir = folder.newFolder(baseDir, "TeamCity-${version}", 'bin')

        File teamcityServerShellFile = new File(binDir, 'teamcity-server.sh')
        teamcityServerShellFile << """
            #!/bin/bash
            echo "Fake TeamCity startup script"
        """
        teamcityServerShellFile.executable = true
        return homeDir
    }
}
