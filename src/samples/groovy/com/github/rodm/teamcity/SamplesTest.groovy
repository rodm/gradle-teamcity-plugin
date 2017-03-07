/*
 * Copyright 2016 Rod MacKenzie
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
import org.junit.Test

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.junit.Assert.assertEquals

class SamplesTest {

    private File samplesDir

    @Before
    void setup() throws IOException {
        samplesDir = new File('samples')
    }

    @Test
    void serverPlugin() {
        File projectDir = new File(samplesDir, 'server-plugin')
        BuildResult result = executeBuild(projectDir)

        assertEquals(result.task(':build').getOutcome(), SUCCESS)
    }

    @Test
    void serverPluginWithAlternativeBuildFile() {
        File projectDir = new File(samplesDir, 'server-plugin')
        BuildResult result = executeBuild(projectDir, '-b', 'build-alt.gradle', 'clean', 'build')

        assertEquals(result.task(':build').getOutcome(), SUCCESS)
    }

    @Test
    void agentServerPlugin() {
        File projectDir = new File(samplesDir, 'agent-server-plugin')
        BuildResult result = executeBuild(projectDir)

        assertEquals(result.task(':build').getOutcome(), SUCCESS)
    }

    @Test
    void agentToolPlugin() {
        File projectDir = new File(samplesDir, 'agent-tool-plugin')
        BuildResult result = executeBuild(projectDir)

        assertEquals(result.task(':build').getOutcome(), SUCCESS)
    }

    @Test
    void multiProjectPlugin() {
        File projectDir = new File(samplesDir, 'multi-project-plugin')
        BuildResult result = executeBuild(projectDir)

        assertEquals(result.task(':server:build').getOutcome(), SUCCESS)
    }

    @Test
    void 'build kotlin plugin'() {
        File projectDir = new File(samplesDir, 'kotlin-plugin')
        BuildResult result = executeBuildWithVersion(projectDir, '3.3')

        assertEquals(result.task(':server:build').getOutcome(), SUCCESS)
    }

    private static BuildResult executeBuild(File projectDir, String... args = ['clean', 'build']) {
        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(args)
                .withPluginClasspath()
                .forwardOutput()
                .build()
        return result
    }

    private static BuildResult executeBuildWithVersion(File projectDir, String version, String... args = ['clean', 'build']) {
        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(args)
                .withPluginClasspath()
                .withGradleVersion(version)
                .forwardOutput()
                .build()
        return result
    }
}
