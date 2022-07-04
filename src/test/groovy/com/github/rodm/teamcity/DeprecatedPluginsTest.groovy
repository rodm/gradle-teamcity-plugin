/*
 * Copyright 2022 the original author or authors.
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

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.internal.deprecation.DeprecationLogger
import org.gradle.internal.featurelifecycle.UsageLocationReporter
import org.gradle.internal.operations.BuildOperationProgressEventEmitter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.github.rodm.teamcity.DeprecatedPluginConstants.DEPRECATED_AGENT_PLUGIN_ID
import static com.github.rodm.teamcity.DeprecatedPluginConstants.DEPRECATED_COMMON_PLUGIN_ID
import static com.github.rodm.teamcity.DeprecatedPluginConstants.DEPRECATED_ENVIRONMENTS_PLUGIN_ID
import static com.github.rodm.teamcity.DeprecatedPluginConstants.DEPRECATED_SERVER_PLUGIN_ID
import static com.github.rodm.teamcity.GradleMatchers.hasPlugin
import static com.github.rodm.teamcity.TeamCityPlugin.AGENT_PLUGIN_ID
import static com.github.rodm.teamcity.TeamCityPlugin.COMMON_PLUGIN_ID
import static com.github.rodm.teamcity.TeamCityPlugin.ENVIRONMENTS_PLUGIN_ID
import static com.github.rodm.teamcity.TeamCityPlugin.SERVER_PLUGIN_ID
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.MatcherAssert.assertThat
import static org.mockito.Mockito.mock

class DeprecatedPluginsTest extends ConfigurationTestCase {

    private String MESSAGE = 'The %s plugin has been deprecated. Please use the %s plugin instead.'

    @BeforeEach
    void setup() {
        DeprecationLogger.init(mock(UsageLocationReporter), WarningMode.All, mock(BuildOperationProgressEventEmitter))
    }

    @AfterEach
    void cleanup() {
        DeprecationLogger.reset()
    }

    @Test
    void 'deprecated server plugin id outputs warning'() {
        project.apply plugin: DEPRECATED_SERVER_PLUGIN_ID

        String warning = String.format(MESSAGE, DEPRECATED_SERVER_PLUGIN_ID, SERVER_PLUGIN_ID)
        String output = outputEventListener.toString()
        assertThat(output, containsString(warning))
    }

    @Test
    void 'deprecated server plugin id applies plugin with new id'() {
        project.apply plugin: DEPRECATED_SERVER_PLUGIN_ID

        assertThat(project, hasPlugin(SERVER_PLUGIN_ID))
    }

    @Test
    void 'deprecated agent plugin id outputs warning'() {
        project.apply plugin: DEPRECATED_AGENT_PLUGIN_ID

        String warning = String.format(MESSAGE, DEPRECATED_AGENT_PLUGIN_ID, AGENT_PLUGIN_ID)
        String output = outputEventListener.toString()
        assertThat(output, containsString(warning))
    }

    @Test
    void 'deprecated agent plugin id applies plugin with new id'() {
        project.apply plugin: DEPRECATED_AGENT_PLUGIN_ID

        assertThat(project, hasPlugin(AGENT_PLUGIN_ID))
    }

    @Test
    void 'deprecated common plugin id outputs warning'() {
        project.apply plugin: DEPRECATED_COMMON_PLUGIN_ID

        String warning = String.format(MESSAGE, DEPRECATED_COMMON_PLUGIN_ID, COMMON_PLUGIN_ID)
        String output = outputEventListener.toString()
        assertThat(output, containsString(warning))
    }

    @Test
    void 'deprecated common plugin id applies plugin with new id'() {
        project.apply plugin: DEPRECATED_COMMON_PLUGIN_ID

        assertThat(project, hasPlugin(COMMON_PLUGIN_ID))
    }

    @Test
    void 'deprecated environments plugin id outputs warning'() {
        project.apply plugin: DEPRECATED_ENVIRONMENTS_PLUGIN_ID

        String warning = String.format(MESSAGE, DEPRECATED_ENVIRONMENTS_PLUGIN_ID, ENVIRONMENTS_PLUGIN_ID)
        String output = outputEventListener.toString()
        assertThat(output, containsString(warning))
    }

    @Test
    void 'deprecated environments plugin id applies plugin with new id'() {
        project.apply plugin: DEPRECATED_ENVIRONMENTS_PLUGIN_ID

        assertThat(project, hasPlugin(ENVIRONMENTS_PLUGIN_ID))
    }
}
