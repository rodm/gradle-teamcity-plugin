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
package com.github.rodm.teamcity;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import static com.github.rodm.teamcity.DeprecatedPluginConstants.DEPRECATED_PLUGIN_MESSAGE;
import static com.github.rodm.teamcity.DeprecatedPluginConstants.DEPRECATED_SERVER_PLUGIN_ID;
import static com.github.rodm.teamcity.TeamCityPlugin.SERVER_PLUGIN_ID;

public class DeprecatedTeamCityServerPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getLogger().warn(DEPRECATED_PLUGIN_MESSAGE, DEPRECATED_SERVER_PLUGIN_ID, SERVER_PLUGIN_ID);
        project.getPluginManager().apply(SERVER_PLUGIN_ID);
    }
}
