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
import org.gradle.api.Transformer;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;

public class TeamCityBasePlugin implements Plugin<Project> {

    private static final String TEAMCITY_EXTENSION_NAME = "teamcity";

    private static final String MINIMUM_SUPPORTED_VERSION = "7.0";

    private static final String INHERITED_PROPERTY_MESSAGE = "Using inherited property '{}' is deprecated. Consider using a convention plugin to configure properties in multiple projects.";
    private static final String DISABLE_INHERITED_PROPERTIES = "teamcity.disableInheritedProperties";

    public void apply(Project project) {
        project.getPluginManager().apply(BasePlugin.class);

        if (GradleVersion.current().compareTo(GradleVersion.version(MINIMUM_SUPPORTED_VERSION)) < 0) {
            throw new GradleException("Gradle TeamCity plugin requires Gradle version " + MINIMUM_SUPPORTED_VERSION + " or later");
        }

        TeamCityPluginExtension extension = project.getExtensions()
            .create(TeamCityPluginExtension.class, TEAMCITY_EXTENSION_NAME, DefaultTeamCityPluginExtension.class, project);
        validateVersion(project, (DefaultTeamCityPluginExtension) extension);
        applyInheritedProperties(project, (DefaultTeamCityPluginExtension) extension);
    }

    private static void validateVersion(Project project, final DefaultTeamCityPluginExtension extension) {
        Property<String> version = extension.getVersionProperty();
        Property<Boolean> allowSnapshotVersions = extension.getAllowSnapshotVersionsProperty();
        project.afterEvaluate(p -> TeamCityVersion.version(version.get(), allowSnapshotVersions.get()));
    }

    private static void applyInheritedProperties(Project project, final DefaultTeamCityPluginExtension extension) {
        if (isNotRootProject(project) && isInheritedPropertiesEnabled(project)) {
            final DefaultTeamCityPluginExtension rootExtension = (DefaultTeamCityPluginExtension) getRootExtension(project);
            if (rootExtension != null) {
                final Logger logger = project.getLogger();
                extension.getVersionProperty().set(withDeprecationTransformer(rootExtension.getVersionProperty(), logger, "version"));
                extension.getAllowSnapshotVersionsProperty().set(withDeprecationTransformer(rootExtension.getAllowSnapshotVersionsProperty(), logger, "allowSnapshotVersions"));
                extension.getValidateBeanDefinitionProperty().set(withDeprecationTransformer(rootExtension.getValidateBeanDefinitionProperty(), logger, "validateBeanDefinition"));
                extension.getDefaultRepositoriesProperty().set(withDeprecationTransformer(rootExtension.getDefaultRepositoriesProperty(), logger, "defaultRepositories"));
            }
        }
    }

    private static TeamCityPluginExtension getRootExtension(Project project) {
        return project.getRootProject().getExtensions().findByType(TeamCityPluginExtension.class);
    }

    private static boolean isNotRootProject(Project project) {
        return !project.getRootProject().equals(project);
    }

    private static boolean isInheritedPropertiesEnabled(Project project) {
        String value = getDisableInheritedProperties(project).getOrElse("false");
        return !Boolean.parseBoolean(value.trim());
    }

    private static Provider<String> getDisableInheritedProperties(Project project) {
        Provider<String> property = project.getProviders().gradleProperty(DISABLE_INHERITED_PROPERTIES);
        if (GradleVersion.current().compareTo(GradleVersion.version("7.4")) <= 0) {
            return property.forUseAtConfigurationTime();
        } else {
            return property;
        }
    }

    private static <T> Provider<T> withDeprecationTransformer(Property<T> property, Logger logger, String name) {
        return property.map(new DeprecationTransformer<>(logger, name));
    }

    private static class DeprecationTransformer<T> implements Transformer<T, T> {

        private final Logger logger;
        private final String name;
        private boolean shown = false;

        DeprecationTransformer(Logger logger, String name) {
            this.logger = logger;
            this.name = name;
        }

        @NotNull
        @Override
        public T transform(@NotNull T in) {
            if (!shown) {
                logger.warn(INHERITED_PROPERTY_MESSAGE, name);
                shown = true;
            }
            return in;
        }
    }
}
