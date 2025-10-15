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

import com.github.rodm.teamcity.internal.AgentConfigurationAction;
import com.github.rodm.teamcity.internal.DefaultLocalTeamCityEnvironment;
import com.github.rodm.teamcity.internal.DefaultTeamCityEnvironments;
import com.github.rodm.teamcity.internal.ShutdownWaitAction;
import com.github.rodm.teamcity.tasks.DownloadTeamCity;
import com.github.rodm.teamcity.tasks.InstallTeamCity;
import com.github.rodm.teamcity.tasks.StartLocalAgent;
import com.github.rodm.teamcity.tasks.StartLocalServer;
import com.github.rodm.teamcity.tasks.StopLocalAgent;
import com.github.rodm.teamcity.tasks.StopLocalServer;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.TaskContainer;

import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP;

@SuppressWarnings("unused")
public class TeamCityLocalEnvironmentsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(TeamCityBaseEnvironmentsPlugin.class);

        TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension.class);
        configureLocalEnvironments(project, extension);
    }

    private static void configureLocalEnvironments(final Project project, final TeamCityPluginExtension extension) {
        ObjectFactory objects = project.getObjects();
        DefaultTeamCityEnvironments environments = (DefaultTeamCityEnvironments) getEnvironments(extension);

        NamedDomainObjectFactory<LocalTeamCityEnvironment> localFactory = name ->
            objects.newInstance(DefaultLocalTeamCityEnvironment.class, name, environments, objects);
        environments.registerFactory(LocalTeamCityEnvironment.class, localFactory);

        NamedDomainObjectContainer<TeamCityEnvironment> container = environments.getEnvironments();
        container.withType(LocalTeamCityEnvironment.class).all(environment ->
            configureLocalEnvironmentTasks(project, (DefaultLocalTeamCityEnvironment) environment));
    }

    private static TeamCityEnvironments getEnvironments(final TeamCityPluginExtension extension) {
        return extension.getExtensions().getByType(TeamCityEnvironments.class);
    }

    private static void configureLocalEnvironmentTasks(Project project, DefaultLocalTeamCityEnvironment environment) {
        final TaskContainer tasks = project.getTasks();
        tasks.register(environment.downloadTaskName(), DownloadTeamCity.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.src(environment.getDownloadUrl());
            task.dest(project.file(environment.getInstallerFile()));
        });

        tasks.register(environment.installTaskName(), InstallTeamCity.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getSource().set(project.file(environment.getInstallerFile()));
            task.getTarget().set(project.file(environment.getHomeDirProperty()));
            task.dependsOn(tasks.named(environment.downloadTaskName()));
        });

        tasks.register(environment.startServerTaskName(), StartLocalServer.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getVersion().set(environment.getVersionProperty());
            task.getHomeDir().set(environment.getHomeDirProperty());
            task.getDataDir().set(environment.getDataDirProperty());
            task.getLogsDir().set(environment.getServerLogsDirProperty());
            task.getJavaHome().set(environment.getJavaHomeProperty());
            task.getServerOptions().set(environment.getServerOptionsProvider());
            task.doFirst(t -> project.mkdir(environment.getDataDir()));
            task.dependsOn(tasks.named(environment.deployTaskName()));
        });

        tasks.register(environment.stopServerTaskName(), StopLocalServer.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getVersion().set(environment.getVersionProperty());
            task.getHomeDir().set(environment.getHomeDirProperty());
            task.getLogsDir().set(environment.getServerLogsDirProperty());
            task.getJavaHome().set(environment.getJavaHomeProperty());
            task.finalizedBy(tasks.named(environment.undeployTaskName()));
            task.doLast(new ShutdownWaitAction(environment.getShutdownTimeoutProperty()));
        });

        tasks.register(environment.startAgentTaskName(), StartLocalAgent.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getVersion().set(environment.getVersionProperty());
            task.getHomeDir().set(environment.getHomeDirProperty());
            task.getJavaHome().set(environment.getJavaHomeProperty());
            task.getConfigDir().set(environment.getAgentConfigurationDirProperty());
            task.getLogsDir().set(environment.getAgentLogsDirProperty());
            task.getAgentOptions().set(environment.getAgentOptionsProvider());
            task.doFirst(new AgentConfigurationAction());
        });

        tasks.register(environment.stopAgentTaskName(), StopLocalAgent.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getVersion().set(environment.getVersionProperty());
            task.getHomeDir().set(environment.getHomeDirProperty());
            task.getJavaHome().set(environment.getJavaHomeProperty());
        });

        tasks.named(environment.startTaskName(), task ->
            task.dependsOn(tasks.named(environment.startServerTaskName()), tasks.named(environment.startAgentTaskName())));
        tasks.named(environment.stopTaskName(), task ->
            task.dependsOn(tasks.named(environment.stopServerTaskName()), tasks.named(environment.stopAgentTaskName())));
    }
}
