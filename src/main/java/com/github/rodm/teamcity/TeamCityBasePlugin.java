/*
 * Copyright 2022 Rod MacKenzie
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
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.Property;
import org.gradle.util.GradleVersion;

public class TeamCityBasePlugin implements Plugin<Project> {

    private static final String TEAMCITY_EXTENSION_NAME = "teamcity";

    private static final String MINIMUM_SUPPORTED_VERSION = "7.0";

    public void apply(Project project) {
        PluginContainer plugins = project.getPlugins();
        plugins.apply(BasePlugin.class);

        if (GradleVersion.current().compareTo(GradleVersion.version(MINIMUM_SUPPORTED_VERSION)) < 0) {
            throw new GradleException("Gradle TeamCity plugin requires Gradle version " + MINIMUM_SUPPORTED_VERSION + " or later");
        }

        TeamCityPluginExtension extension = project.getExtensions()
            .create(TeamCityPluginExtension.class, TEAMCITY_EXTENSION_NAME, DefaultTeamCityPluginExtension.class, project);
        ((DefaultTeamCityPluginExtension) extension).init();
        validateVersion(project, (DefaultTeamCityPluginExtension) extension);
        applyInheritedProperties(project, (DefaultTeamCityPluginExtension) extension);
    }

    private static void validateVersion(Project project, final DefaultTeamCityPluginExtension extension) {
        Property<String> version = extension.getVersionProperty();
        Property<Boolean> allowSnapshotVersions = extension.getAllowSnapshotVersionsProperty();
        project.afterEvaluate(p -> TeamCityVersion.version(version.get(), allowSnapshotVersions.get()));
    }

    private static void applyInheritedProperties(Project project, final DefaultTeamCityPluginExtension extension) {
        if (isNotRootProject(project)) {
            DefaultTeamCityPluginExtension rootExtension = (DefaultTeamCityPluginExtension) getRootExtension(project);
            if (rootExtension != null) {
                extension.getVersionProperty().set(rootExtension.getVersionProperty());
                extension.getAllowSnapshotVersionsProperty().set(rootExtension.getAllowSnapshotVersionsProperty());
                extension.getValidateBeanDefinitionProperty().set(rootExtension.getValidateBeanDefinitionProperty());
                extension.getDefaultRepositoriesProperty().set(rootExtension.getDefaultRepositoriesProperty());
            }
        }
    }

    private static TeamCityPluginExtension getRootExtension(Project project) {
        return project.getRootProject().getExtensions().findByType(TeamCityPluginExtension.class);
    }

    private static boolean isNotRootProject(Project project) {
        return !project.getRootProject().equals(project);
    }
}
