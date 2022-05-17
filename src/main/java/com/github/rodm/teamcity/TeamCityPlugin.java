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
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.bundling.Zip;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TeamCityPlugin implements Plugin<Project> {

    public static final String PLUGIN_DESCRIPTOR_FILENAME = "teamcity-plugin.xml";

    public static final String PLUGIN_DESCRIPTOR_DIR = "descriptor";

    public static final String TEAMCITY_GROUP = "TeamCity";

    public static final String JAVA_PLUGIN_ID = "org.gradle.java";
    public static final String SERVER_PLUGIN_ID = "com.github.rodm.teamcity-server";
    public static final String AGENT_PLUGIN_ID = "com.github.rodm.teamcity-agent";

    public static final String AGENT_CONFIGURATION_NAME = "agent";
    public static final String SERVER_CONFIGURATION_NAME = "server";
    public static final String PLUGIN_CONFIGURATION_NAME = "plugin";
    public static final String PROVIDED_CONFIGURATION_NAME = "provided";

    public static final String GRADLE_OFFLINE = "gradle-offline";

    private static final String JETBRAINS_MAVEN_REPOSITORY = "https://download.jetbrains.com/teamcity-repository";

    private static final String CLASSES_PATTERN = "**/*.class";

    public void apply(Project project) {
        project.getPluginManager().apply(TeamCityBasePlugin.class);

        TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension.class);
        configureRepositories(project, extension);
        configureConfigurations(project);
    }

    private static void configureRepositories(Project project, TeamCityPluginExtension extension) {
        project.afterEvaluate(p ->
            project.getPluginManager().withPlugin(JAVA_PLUGIN_ID, plugin -> {
                if (extension.getDefaultRepositories()) {
                    project.getRepositories().mavenCentral();
                    project.getRepositories().maven(repository -> repository.setUrl(JETBRAINS_MAVEN_REPOSITORY));
                }
            })
        );
    }

    public static void configureConfigurations(final Project project) {
        final ConfigurationContainer configurations = project.getConfigurations();
        configurations.maybeCreate(AGENT_CONFIGURATION_NAME)
            .setVisible(false)
            .setDescription("Configuration for agent plugin.");
        configurations.maybeCreate(SERVER_CONFIGURATION_NAME)
            .setVisible(false)
            .setDescription("Configuration for server plugin.");
        configurations.maybeCreate(PLUGIN_CONFIGURATION_NAME)
            .setVisible(false)
            .setTransitive(false)
            .setDescription("Configuration for plugin artifact.");
        project.getPluginManager().withPlugin(JAVA_PLUGIN_ID, plugin -> {
            Configuration providedConfiguration = configurations.maybeCreate(PROVIDED_CONFIGURATION_NAME)
                .setVisible(false)
                .setDescription("Additional compile classpath for TeamCity libraries that will not be part of the plugin archive.");
            configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).extendsFrom(providedConfiguration);
            configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(providedConfiguration);
        });
    }

    public static void configureJarTask(final Project project, final TeamCityPluginExtension extension, final String pattern) {
        project.getPluginManager().withPlugin(JAVA_PLUGIN_ID, plugin ->
            project.getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class).configure(task -> {
            task.getInputs().property(GRADLE_OFFLINE, project.getGradle().getStartParameter().isOffline());
            ValidationMode mode = extension.getValidateBeanDefinition();
            List<PluginDefinition> pluginDefinitions = new ArrayList<>();
            Set<String> classes = new LinkedHashSet<>();
            task.filesMatching(pattern, new PluginDefinitionCollectorAction(pluginDefinitions));
            task.filesMatching(CLASSES_PATTERN, new ClassCollectorAction(classes));
            task.doLast(new PluginDefinitionValidationAction(mode, pluginDefinitions, classes));
        }));
    }

    public static void configurePluginArchiveTask(Zip task, String archiveName) {
        if (archiveName != null) {
            String name = archiveName.endsWith(".zip") ? archiveName : archiveName + ".zip";
            task.getArchiveFileName().set(name);
        }
    }
}
