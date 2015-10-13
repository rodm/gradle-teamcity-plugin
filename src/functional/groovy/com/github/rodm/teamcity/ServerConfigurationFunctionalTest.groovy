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
        assertThat(result.getStandardError(), containsString("specified for property 'descriptor' does not exist."))
    }

    @Test
    public void startServerAfterDeployingPlugin() {
        File homeDir = testProjectDir.newFolder('teamcity')
        File binDir = testProjectDir.newFolder('teamcity', 'bin')
        File dataDir = testProjectDir.newFolder('data')

        File teamcityServerShellFile = new File(binDir, 'teamcity-server.sh')
        teamcityServerShellFile << """
            #!/bin/bash
            echo "Fake TeamCity startup script"
        """
        teamcityServerShellFile.executable = true

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
                homeDir = file('${homeDir.canonicalPath}')
                dataDir = file('${dataDir.canonicalPath}')
                javaHome = file('${dataDir.canonicalPath}')
            }
        """

        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << """
            rootProject.name = 'test-plugin'
        """

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("build", "startServer")
                .build()

        File pluginFile = new File(dataDir, 'plugins/test-plugin.zip')
        assertTrue('Plugin archive not deployed', pluginFile.exists())
        assertThat(result.task(":deployPlugin").getOutcome(), is(SUCCESS))
        assertThat(result.task(":startServer").getOutcome(), is(SUCCESS))
    }
}
