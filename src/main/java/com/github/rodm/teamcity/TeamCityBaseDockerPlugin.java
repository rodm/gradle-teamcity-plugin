/*
 * Copyright 2025 Rod MacKenzie
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

import com.github.rodm.teamcity.docker.DockerTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.tasks.TaskContainer;

@SuppressWarnings("unused")
public class TeamCityBaseDockerPlugin implements Plugin<Project> {

    public static final String DOCKER_CONFIGURATION_NAME = "docker";

    private static final String DOCKER_JAVA_CORE = "com.github.docker-java:docker-java-core:3.2.13";
    private static final String DOCKER_JAVA_CLIENT = "com.github.docker-java:docker-java-transport-httpclient5:3.2.13";

    @Override
    public void apply(Project project) {
        TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension.class);
        configureRepository(project, extension);
        configureConfiguration(project);
        configureDockerTasks(project);
    }

    private static void configureRepository(Project project, TeamCityPluginExtension extension) {
        project.afterEvaluate(p -> {
            if (extension.getDefaultRepositories()) {
                project.getRepositories().mavenCentral();
            }
        });
    }

    private static void configureConfiguration(final Project project) {
        ConfigurationContainer configurations = project.getConfigurations();
        configurations.maybeCreate(DOCKER_CONFIGURATION_NAME)
            .setVisible(false)
            .setDescription("Configuration for Docker task dependencies.")
            .defaultDependencies(dependencies -> {
                DependencyHandler handler = project.getDependencies();
                dependencies.add(handler.create(DOCKER_JAVA_CORE));
                dependencies.add(handler.create(DOCKER_JAVA_CLIENT));
            });
    }

    private static void configureDockerTasks(final Project project) {
        TaskContainer tasks = project.getTasks();
        tasks.withType(DockerTask.class, task ->
            task.setClasspath(project.getConfigurations().getByName(DOCKER_CONFIGURATION_NAME)));
    }
}
