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
import com.github.rodm.teamcity.internal.DefaultDockerTeamCityEnvironment;
import com.github.rodm.teamcity.internal.DefaultTeamCityEnvironments;
import com.github.rodm.teamcity.tasks.Deploy;
import com.github.rodm.teamcity.tasks.StartDockerAgent;
import com.github.rodm.teamcity.tasks.StartDockerServer;
import com.github.rodm.teamcity.tasks.StopDockerAgent;
import com.github.rodm.teamcity.tasks.StopDockerServer;
import com.github.rodm.teamcity.tasks.Undeploy;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.TaskContainer;

import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP;

@SuppressWarnings("unused")
public class TeamCityDockerEnvironmentsPlugin implements Plugin<Project> {

    public static final String DOCKER_CONFIGURATION_NAME = "docker";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(TeamCityBaseEnvironmentsPlugin.class);

        TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension.class);
        configureRepository(project, extension);
        configureConfiguration(project);
        configureDockerEnvironments(project, extension);
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
                dependencies.add(project.getDependencies().create("com.github.docker-java:docker-java-core:3.2.13"));
                dependencies.add(project.getDependencies().create("com.github.docker-java:docker-java-transport-httpclient5:3.2.13"));
            });
    }

    private static void configureDockerEnvironments(final Project project, final TeamCityPluginExtension extension) {
        ObjectFactory objects = project.getObjects();
        DefaultTeamCityEnvironments environments = (DefaultTeamCityEnvironments) getEnvironments(extension);

        NamedDomainObjectFactory<DockerTeamCityEnvironment> dockerFactory = name ->
            objects.newInstance(DefaultDockerTeamCityEnvironment.class, name, environments, objects);
        environments.registerFactory(DockerTeamCityEnvironment.class, dockerFactory);

        NamedDomainObjectContainer<TeamCityEnvironment> container = environments.getEnvironments();
        container.withType(DockerTeamCityEnvironment.class).all(environment ->
            configureDockerEnvironmentTasks(project, (DefaultDockerTeamCityEnvironment) environment));
    }

    private static TeamCityEnvironments getEnvironments(final TeamCityPluginExtension extension) {
        return extension.getExtensions().getByType(TeamCityEnvironments.class);
    }

    private static void configureDockerEnvironmentTasks(Project project, DefaultDockerTeamCityEnvironment environment) {
        final TaskContainer tasks = project.getTasks();
        tasks.register(environment.startServerTaskName(), StartDockerServer.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getDataDir().set(environment.getDataDirProperty());
            task.getLogsDir().set(environment.getServerLogsDirProperty());
            task.getServerOptions().set(environment.getServerOptionsProvider());
            task.getImageName().set(environment.getServerImageProperty());
            task.getImageTag().set(environment.getServerTagProperty());
            task.getContainerName().set(environment.getServerNameProperty());
            task.getPort().set(environment.getPortProperty());
            task.doFirst(t -> project.mkdir(environment.getDataDir()));
            task.dependsOn(tasks.named(environment.deployTaskName()));
        });

        tasks.register(environment.stopServerTaskName(), StopDockerServer.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getContainerName().set(environment.getServerNameProperty());
            task.finalizedBy(tasks.named(environment.undeployTaskName()));
        });

        tasks.register(environment.startAgentTaskName(), StartDockerAgent.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getDataDir().set(environment.getDataDirProperty());
            task.getConfigDir().set(environment.getAgentConfigurationDirProperty());
            task.getLogsDir().set(environment.getAgentLogsDirProperty());
            task.getAgentOptions().set(environment.getAgentOptionsProvider());
            task.getImageName().set(environment.getAgentImageProperty());
            task.getImageTag().set(environment.getAgentTagProperty());
            task.getContainerName().set(environment.getAgentNameProperty());
            task.getServerContainerName().set(environment.getServerNameProperty());
            task.mustRunAfter(tasks.named(environment.startServerTaskName()));
        });

        tasks.register(environment.stopAgentTaskName(), StopDockerAgent.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getContainerName().set(environment.getAgentNameProperty());
        });

        tasks.named(environment.deployTaskName(), Deploy.class).configure(task ->
            task.getServerPort().set(environment.getPortProperty()));
        tasks.named(environment.undeployTaskName(), Undeploy.class).configure(task ->
            task.getServerPort().set(environment.getPortProperty()));

        tasks.withType(DockerTask.class, task ->
            task.setClasspath(project.getConfigurations().getByName(DOCKER_CONFIGURATION_NAME)));

        tasks.named(environment.startTaskName(), task ->
            task.dependsOn(tasks.named(environment.startServerTaskName()), tasks.named(environment.startAgentTaskName())));
        tasks.named(environment.stopTaskName(), task ->
            task.dependsOn(tasks.named(environment.stopServerTaskName()), tasks.named(environment.stopAgentTaskName())));
    }
}
