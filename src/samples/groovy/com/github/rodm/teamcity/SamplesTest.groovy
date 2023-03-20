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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import java.nio.file.Path
import java.nio.file.Paths

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.CoreMatchers.is
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class SamplesTest {

    class Samples {

        Path samples

        Samples(Path samples) {
            this.samples = samples
        }

        BuildResult executeBuild(Path projectDir) {
            SamplesTest.executeBuild(projectDir)
        }

        @Test
        void 'server plugin'() {
            BuildResult result = executeBuild(samples.resolve('server-plugin'))

            assertThat(result.task(':build').getOutcome(), is(SUCCESS))
        }

        @Test
        void 'agent server plugin'() {
            BuildResult result = executeBuild(samples.resolve('agent-server-plugin'))

            assertThat(result.task(':build').getOutcome(), is(SUCCESS))
        }

        @Test
        void 'agent tool plugin'() {
            BuildResult result = executeBuild(samples.resolve('agent-tool-plugin'))

            assertThat(result.task(':build').getOutcome(), is(SUCCESS))
        }

        @Test
        void 'multiple plugins'() {
            BuildResult result = executeBuild(samples.resolve('multiple-plugins'))

            assertThat(result.task(':plugin1:build').getOutcome(), is(SUCCESS))
            assertThat(result.task(':plugin2:build').getOutcome(), is(SUCCESS))
        }

        @Test
        void 'reloadable plugin'() {
            BuildResult result = executeBuild(samples.resolve('reloadable-plugin'))

            assertThat(result.task(':build').getOutcome(), is(SUCCESS))
        }

        @Test
        void 'docker environment'() {
            BuildResult result = executeBuild(samples.resolve('docker-environment'))

            assertThat(result.task(':plugin:server:build').getOutcome(), is(SUCCESS))
        }
    }

    @Nested
    @DisplayName("using groovy build scripts")
    class UsingGroovyBuildScripts extends Samples {

        UsingGroovyBuildScripts() {
            super(Paths.get("samples", "groovy"))
        }

        @Test
        void 'multi project plugin'() {
            BuildResult result = executeBuild(samples.resolve('multi-project-plugin'))

            assertThat(result.task(':server:build').getOutcome(), is(SUCCESS))
        }
    }

    @Nested
    @DisplayName("using kotlin build scripts")
    class UsingKotlinBuildScripts extends Samples {

        UsingKotlinBuildScripts() {
            super(Paths.get("samples", "kotlin"))
        }

        @Test
        void 'build kotlin plugin'() {
            BuildResult result = executeBuild(samples.resolve('kotlin-plugin'))

            assertThat(result.task(':server:build').getOutcome(), is(SUCCESS))
        }
    }

    private static BuildResult executeBuild(Path projectDir, String... args = ['clean', 'build']) {
        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments('--warning-mode', 'fail', *args)
                .withPluginClasspath()
                .forwardOutput()
                .build()
        return result
    }
}
