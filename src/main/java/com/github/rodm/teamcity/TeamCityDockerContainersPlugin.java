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

import com.github.rodm.teamcity.internal.DefaultDockerContainer;
import com.github.rodm.teamcity.internal.DefaultTeamCityEnvironments;
import com.github.rodm.teamcity.tasks.StartDockerContainer;
import com.github.rodm.teamcity.tasks.StopDockerContainer;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.TaskContainer;

import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP;

@SuppressWarnings("unused")
public class TeamCityDockerContainersPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "containers";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(TeamCityBaseEnvironmentsPlugin.class);
        project.getPluginManager().apply(TeamCityBaseDockerPlugin.class);

        ObjectFactory objects = project.getObjects();
        NamedDomainObjectFactory<DockerContainer> factory = name ->
            objects.newInstance(DefaultDockerContainer.class, name, objects);
        NamedDomainObjectContainer<DockerContainer> containers = project.container(DockerContainer.class, factory);

        TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension.class);
        TeamCityEnvironments environments = getEnvironments(extension);
        ((ExtensionAware) environments).getExtensions().add(EXTENSION_NAME, containers);

        configureDockerContainers(project, containers);
    }

    private static void configureDockerContainers(final Project project, NamedDomainObjectContainer<DockerContainer> containers) {
        NamedDomainObjectContainer<TeamCityEnvironment> environments = getEnvironmentsContainer(project);

        final TaskContainer tasks = project.getTasks();
        containers.withType(DockerContainer.class, container ->
            environments.withType(TeamCityEnvironment.class, environment ->
                configureDockerContainer(project, (DefaultDockerContainer) container, (BaseTeamCityEnvironment) environment)));
    }

    private static void configureDockerContainer(Project project, DefaultDockerContainer container, BaseTeamCityEnvironment environment) {
        final TaskContainer tasks = project.getTasks();
        String startTaskName = "start" + environment.getCapitalizedName() + container.getCapitalizedName();
        tasks.register(startTaskName, StartDockerContainer.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getContainerName().set(container.getName());
            task.getImage().set(container.getImageProperty());
            task.getPorts().set(container.getPorts());
            task.getVolumes().set(container.getVolumes());
            task.getEnvironmentVariables().set(container.getVariables());
            task.getDataDir().set(environment.getDataDirProperty());
        });
        String stopTaskName = "stop" + environment.getCapitalizedName() + container.getCapitalizedName();
        tasks.register(stopTaskName, StopDockerContainer.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getContainerName().set(container.getName());
        });
    }

    private static NamedDomainObjectContainer<TeamCityEnvironment> getEnvironmentsContainer(Project project) {
        TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension.class);
        TeamCityEnvironments environments = getEnvironments(extension);
        return ((DefaultTeamCityEnvironments) environments).getEnvironments();
    }

    private static TeamCityEnvironments getEnvironments(final TeamCityPluginExtension extension) {
        return extension.getExtensions().getByType(TeamCityEnvironments.class);
    }
}
