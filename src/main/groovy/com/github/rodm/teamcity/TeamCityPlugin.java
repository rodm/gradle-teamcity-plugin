/*
 * Copyright 2015 Rod MacKenzie
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

import com.github.rodm.teamcity.internal.ClassCollectorAction;
import com.github.rodm.teamcity.internal.PluginDefinition;
import com.github.rodm.teamcity.internal.PluginDefinitionCollectorAction;
import com.github.rodm.teamcity.internal.PluginDefinitionValidationAction;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.util.GradleVersion;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TeamCityPlugin implements Plugin<Project> {

    public static final String PLUGIN_DESCRIPTOR_FILENAME = "teamcity-plugin.xml";

    public static final String PLUGIN_DESCRIPTOR_DIR = "descriptor";

    private static final String TEAMCITY_EXTENSION_NAME = "teamcity";

    public static final String TEAMCITY_GROUP = "TeamCity";

    private static final String JETBRAINS_MAVEN_REPOSITORY = "https://download.jetbrains.com/teamcity-repository";

    private static final String CLASSES_PATTERN = "**/*.class";

    private final String MINIMUM_SUPPORTED_VERSION = "7.0";

    public void apply(Project project) {
        PluginContainer plugins = project.getPlugins();
        plugins.apply(BasePlugin.class);

        if (GradleVersion.current().compareTo(GradleVersion.version(MINIMUM_SUPPORTED_VERSION)) < 0) {
            throw new GradleException("Gradle TeamCity plugin requires Gradle version " + MINIMUM_SUPPORTED_VERSION + " or later");
        }

        if (plugins.hasPlugin(JavaPlugin.class) &&
            (plugins.hasPlugin(TeamCityAgentPlugin.class) || plugins.hasPlugin(TeamCityServerPlugin.class))) {
            throw new GradleException("Cannot apply both the teamcity-agent and teamcity-server plugins with the Java plugin");
        }

        TeamCityPluginExtension extension;
        if (project.getExtensions().findByType(TeamCityPluginExtension.class) != null) {
            extension = project.getExtensions().getByType(TeamCityPluginExtension.class);
        } else {
            extension = project.getExtensions().create(TEAMCITY_EXTENSION_NAME, TeamCityPluginExtension.class, project);
            extension.init();
        }
        configureRepositories(project, extension);
        configureConfigurations(project);
        validateVersion(project, extension);
    }

    private static void configureRepositories(Project project, TeamCityPluginExtension extension) {
        project.afterEvaluate(new ConfigureRepositories(extension));
    }

    public static void configureConfigurations(final Project project) {
        final ConfigurationContainer configurations = project.getConfigurations();
        configurations.maybeCreate("agent")
            .setVisible(false)
            .setDescription("Configuration for agent plugin.");
        configurations.maybeCreate("server")
            .setVisible(false)
            .setDescription("Configuration for server plugin.");
        configurations.maybeCreate("plugin")
            .setVisible(false)
            .setTransitive(false)
            .setDescription("Configuration for plugin artifact.");
        project.getPlugins().withType(JavaPlugin.class, plugin -> {
            Configuration providedConfiguration = configurations.maybeCreate("provided")
                .setVisible(false)
                .setDescription("Additional compile classpath for TeamCity libraries that will not be part of the plugin archive.");
            configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).extendsFrom(providedConfiguration);
            configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(providedConfiguration);
        });
    }

    public static void validateVersion(Project project, final TeamCityPluginExtension extension) {
        project.afterEvaluate(p -> {
            TeamCityVersion.version(extension.getVersion(), extension.getAllowSnapshotVersions());
        });
    }

    public static void configureJarTask(final Project project, final TeamCityPluginExtension extension, final String pattern) {
        project.afterEvaluate(p -> {
            Jar jarTask = (Jar) p.getTasks().findByName(JavaPlugin.JAR_TASK_NAME);
            if (jarTask != null) {
                ValidationMode mode = extension.getValidateBeanDefinition();
                List<PluginDefinition> pluginDefinitions = new ArrayList<>();
                Set<String> classes = new LinkedHashSet<>();
                jarTask.filesMatching(pattern, new PluginDefinitionCollectorAction(pluginDefinitions));
                jarTask.filesMatching(CLASSES_PATTERN, new ClassCollectorAction(classes));
                jarTask.doLast(new PluginDefinitionValidationAction(mode, pluginDefinitions, classes));
            }
        });
    }

    public static void configurePluginArchiveTask(Zip task, String archiveName) {
        if (archiveName != null) {
            String name = archiveName.endsWith(".zip") ? archiveName : archiveName + ".zip";
            task.getArchiveFileName().set(name);
        }
    }

    public static class ConfigureRepositories implements Action<Project> {

        private TeamCityPluginExtension extension;

        public ConfigureRepositories(TeamCityPluginExtension extension) {
            this.extension = extension;
        }

        @Override
        public void execute(final Project project) {
            project.getPlugins().withType(JavaPlugin.class, plugin -> {
                if (extension.getDefaultRepositories()) {
                    project.getRepositories().mavenCentral();
                    project.getRepositories().maven(repository -> {
                        repository.setUrl(JETBRAINS_MAVEN_REPOSITORY);
                    });
                }
            });
        }
    }
}
