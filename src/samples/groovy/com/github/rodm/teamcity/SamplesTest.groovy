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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.CoreMatchers.is
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

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

        assertThat(result.task(':build').getOutcome(), is(SUCCESS))
    }

    @Test
    void serverPluginWithAlternativeBuildFile() {
        File projectDir = new File(samplesDir, 'server-plugin')
        BuildResult result = executeBuild(projectDir, '--include-build', '../..', '-b', 'build-alt.gradle', 'clean', 'build')

        assertThat(result.task(':build').getOutcome(), is(SUCCESS))
    }

    @Test
    void agentServerPlugin() {
        File projectDir = new File(samplesDir, 'agent-server-plugin')
        BuildResult result = executeBuild(projectDir)

        assertThat(result.task(':build').getOutcome(), is(SUCCESS))
    }

    @Test
    void agentToolPlugin() {
        File projectDir = new File(samplesDir, 'agent-tool-plugin')
        BuildResult result = executeBuild(projectDir)

        assertThat(result.task(':build').getOutcome(), is(SUCCESS))
    }

    @Test
    void multiProjectPlugin() {
        File projectDir = new File(samplesDir, 'multi-project-plugin')
        BuildResult result = executeBuild(projectDir)

        assertThat(result.task(':server:build').getOutcome(), is(SUCCESS))
    }

    @Test
    void 'build kotlin plugin'() {
        File projectDir = new File(samplesDir, 'kotlin-plugin')
        BuildResult result = executeBuild(projectDir)

        assertThat(result.task(':server:build').getOutcome(), is(SUCCESS))
    }

    @Test
    void 'multiple plugins'() {
        File projectDir = new File(samplesDir, 'multiple-plugins')
        BuildResult result = executeBuild(projectDir)

        assertThat(result.task(':plugin1:build').getOutcome(), is(SUCCESS))
        assertThat(result.task(':plugin2:build').getOutcome(), is(SUCCESS))
    }

    private static BuildResult executeBuild(File projectDir, String... args = ['clean', 'build']) {
        def buildArgs = ['--warning-mode', 'all', *args]
        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(buildArgs)
                .withPluginClasspath()
                .forwardOutput()
                .build()
        return result
    }
}
