/*
 * Copyright 2017 Rod MacKenzie
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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static com.github.rodm.teamcity.GradleMatchers.hasDependency
import static org.hamcrest.MatcherAssert.assertThat

class TeamCityCommonPluginTest {

    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder()

    private Project project

    @Before
    void setup() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.root).build()
    }

    @Test
    void 'apply adds common-api to the provided configuration'() {
        project.apply plugin: 'java'
        project.apply plugin: 'com.github.rodm.teamcity-common'

        project.evaluate()

        Configuration configuration = project.configurations.getByName('provided')
        assertThat(configuration, hasDependency('org.jetbrains.teamcity', 'common-api', '9.0'))
    }
}
