package com.github.rodm.teamcity

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.util.zip.ZipFile

import static org.gradle.testkit.runner.TaskOutcome.*
import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertEquals

public class AgentPluginFunctionalTest {

    static final String BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR = """
        plugins {
            id 'java'
            id 'com.github.rodm.teamcity-agent'
        }
        teamcity {
            version = '8.1.5'
            descriptor {
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

    static final String NO_DEFINITION_WARNING = TeamCityAgentPlugin.NO_DEFINITION_WARNING_MESSAGE.substring(4)

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    private File buildFile

    @Before
    public void setup() throws IOException {
        buildFile = testProjectDir.newFile("build.gradle")
    }

    @Test
    public void agentPluginBuildAndPackage() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        File settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile << """
            rootProject.name = 'test-plugin'
        """

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("agentPlugin")
                .withPluginClasspath()
                .build();

        assertEquals(result.task(":generateAgentDescriptor").getOutcome(), SUCCESS)
        assertEquals(result.task(":processAgentDescriptor").getOutcome(), SKIPPED)
        assertEquals(result.task(":agentPlugin").getOutcome(), SUCCESS)

        ZipFile pluginFile = new ZipFile(new File(testProjectDir.root, 'build/distributions/test-plugin-agent.zip'))
        List<String> entries = pluginFile.entries().collect { it.name }
        assertThat(entries, hasItem('teamcity-plugin.xml'))
        assertThat(entries, hasItem('lib/test-plugin.jar'))
    }

    @Test
    public void agentPluginWithDescriptorFile() {
        buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR

        File descriptorFile = testProjectDir.newFile("teamcity-plugin.xml");
        descriptorFile << """
            <?xml version="1.0" encoding="UTF-8"?>
            <teamcity-agent-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                   xsi:noNamespaceSchemaLocation="urn:schemas-jetbrains-com:teamcity-plugin-v1-xml">
                <plugin-deployment use-separate-classloader="true"/>
            </teamcity-agent-plugin>
        """

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("agentPlugin")
                .withPluginClasspath()
                .build();

        assertEquals(result.task(":generateAgentDescriptor").getOutcome(), SKIPPED)
        assertEquals(result.task(":processAgentDescriptor").getOutcome(), SUCCESS)
        assertEquals(result.task(":agentPlugin").getOutcome(), SUCCESS)
    }


    @Test
    public void agentPluginNoWarningsWithDefinitionFile() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        File metaInfDir = testProjectDir.newFolder('src', 'main', 'resources', 'META-INF')
        File definitionFile = new File(metaInfDir, 'build-agent-plugin-example.xml')
        definitionFile.createNewFile()

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("agentPlugin")
                .withPluginClasspath()
                .build();

        assertThat(result.getOutput(), not(containsString(NO_DEFINITION_WARNING)))
    }


    @Test
    public void agentPluginWarnsAboutMissingDefinitionFile() {
        buildFile << BUILD_SCRIPT_WITH_INLINE_DESCRIPTOR

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("agentPlugin")
                .withPluginClasspath()
                .build();

        assertThat(result.getOutput(), containsString(NO_DEFINITION_WARNING))
    }

    @Test
    public void agentPluginFailsWithMissingDescriptorFile() {
        buildFile << BUILD_SCRIPT_WITH_FILE_DESCRIPTOR

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("agentPlugin")
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.task(":processAgentDescriptor").getOutcome(), is(FAILED))
        assertThat(result.getOutput(), containsString("specified for property 'descriptor' does not exist."))
    }
}
