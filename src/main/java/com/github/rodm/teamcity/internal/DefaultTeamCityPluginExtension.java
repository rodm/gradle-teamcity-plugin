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
package com.github.rodm.teamcity.internal;

import com.github.rodm.teamcity.TeamCityPluginExtension;
import com.github.rodm.teamcity.ValidationMode;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

import static com.github.rodm.teamcity.ValidationMode.WARN;

/**
 * TeamCity Plugin extension.
 */
public abstract class DefaultTeamCityPluginExtension implements TeamCityPluginExtension {

    private static final String DEFAULT_TEAMCITY_API_VERSION = "9.0";

    private final Property<String> version;
    private final Property<Boolean> defaultRepositories;
    private final Property<Boolean> allowSnapshotVersions;
    private final Property<ValidationMode> validateBeanDefinition;

    public DefaultTeamCityPluginExtension(Project project) {
        this.version = project.getObjects().property(String.class).convention(DEFAULT_TEAMCITY_API_VERSION);
        this.defaultRepositories = project.getObjects().property(Boolean.class).convention(true);
        this.allowSnapshotVersions = project.getObjects().property(Boolean.class).convention(false);
        this.validateBeanDefinition = project.getObjects().property(ValidationMode.class).convention(WARN);
    }

    @Override
    public void setVersion(String version) {
        this.version.set(version);
    }

    @Override
    public String getVersion() {
        return version.get();
    }

    public Property<String> getVersionProperty() {
        return version;
    }

    @Override
    public void setDefaultRepositories(boolean useDefaultRepositories) {
        defaultRepositories.set(useDefaultRepositories);
    }

    @Override
    public boolean getDefaultRepositories() {
        return defaultRepositories.get();
    }

    public Property<Boolean> getDefaultRepositoriesProperty() {
        return defaultRepositories;
    }

    @Override
    public void setAllowSnapshotVersions(boolean allowSnapshots) {
        allowSnapshotVersions.set(allowSnapshots);
    }

    @Override
    public boolean getAllowSnapshotVersions() {
        return allowSnapshotVersions.get();
    }

    public Property<Boolean> getAllowSnapshotVersionsProperty() {
        return allowSnapshotVersions;
    }

    @Override
    public void setValidateBeanDefinition(ValidationMode mode) {
        validateBeanDefinition.set(mode);
    }

    @Override
    public void setValidateBeanDefinition(String mode) {
        validateBeanDefinition.set(ValidationMode.valueOf(mode.toUpperCase()));
    }

    @Override
    public ValidationMode getValidateBeanDefinition() {
        return validateBeanDefinition.get();
    }

    public Property<ValidationMode> getValidateBeanDefinitionProperty() {
        return validateBeanDefinition;
    }
}
