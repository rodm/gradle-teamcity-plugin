/*
 * Copyright 2016 Vladislav Rassokhin from JetBrains
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

import com.github.rodm.teamcity.internal.DefaultTeamCityPluginExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;

public class TeamCityCommonPlugin implements Plugin<Project> {
    public void apply(final Project project) {
        project.getPlugins().apply(TeamCityPlugin.class);

        final TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension.class);
        configureDependencies(project, (DefaultTeamCityPluginExtension) extension);
    }

    private void configureDependencies(final Project project, final DefaultTeamCityPluginExtension extension) {
        Provider<String> version = extension.getVersionProperty();
        project.getPlugins().withType(JavaPlugin.class, plugin -> {
            project.getDependencies().add("provided", version.map(v -> "org.jetbrains.teamcity:common-api:" + v));
            project.getDependencies().add("testImplementation", version.map(v ->"org.jetbrains.teamcity:tests-support:" + v));
        });
    }
}
