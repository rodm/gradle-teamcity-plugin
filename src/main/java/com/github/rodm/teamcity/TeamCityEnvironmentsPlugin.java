/*
 * Copyright 2018 Rod MacKenzie
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

import com.github.rodm.teamcity.internal.BaseTeamCityEnvironment;
import com.github.rodm.teamcity.internal.DefaultLocalTeamCityEnvironment;
import com.github.rodm.teamcity.internal.DefaultTeamCityEnvironments;
import com.github.rodm.teamcity.internal.DisablePluginAction;
import com.github.rodm.teamcity.internal.EnablePluginAction;
import com.github.rodm.teamcity.tasks.Deploy;
import com.github.rodm.teamcity.tasks.DownloadTeamCity;
import com.github.rodm.teamcity.tasks.InstallTeamCity;
import com.github.rodm.teamcity.tasks.ServerPlugin;
import com.github.rodm.teamcity.tasks.StartAgent;
import com.github.rodm.teamcity.tasks.StartDockerAgent;
import com.github.rodm.teamcity.tasks.StartDockerServer;
import com.github.rodm.teamcity.tasks.StartServer;
import com.github.rodm.teamcity.tasks.StopAgent;
import com.github.rodm.teamcity.tasks.StopDockerAgent;
import com.github.rodm.teamcity.tasks.StopDockerServer;
import com.github.rodm.teamcity.tasks.StopServer;
import com.github.rodm.teamcity.tasks.Undeploy;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP;
import static com.github.rodm.teamcity.TeamCityServerPlugin.SERVER_PLUGIN_TASK_NAME;
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.ASSEMBLE_TASK_NAME;

public class TeamCityEnvironmentsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(TeamCityPlugin.class);

        TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension.class);
        project.afterEvaluate(new ConfigureEnvironmentTasksAction(extension));
    }

    public static class ConfigureEnvironmentTasksAction implements Action<Project> {

        private final TeamCityPluginExtension extension;

        public ConfigureEnvironmentTasksAction(TeamCityPluginExtension extension) {
            this.extension = extension;
        }

        @Override
        public void execute(final Project project) {
            DefaultTeamCityEnvironments environments = (DefaultTeamCityEnvironments) extension.getEnvironments();
            NamedDomainObjectContainer<TeamCityEnvironment> container = environments.getEnvironments();
            container.withType(DefaultLocalTeamCityEnvironment.class).all(environment -> {
                configureDeploymentTasks(project, environment);
                if (environment.isDockerEnabled())  {
                    configureDockerEnvironmentTasks(project, environment);
                } else {
                    configureLocalEnvironmentTasks(project, environment);
                }
                configureCommonTasks(project, capitalize(environment.getName()));
            });
        }

        private void configureDeploymentTasks(Project project, BaseTeamCityEnvironment environment) {
            final TaskContainer tasks = project.getTasks();
            final String name = capitalize(environment.getName());
            final TaskProvider<Deploy> deployPlugin = tasks.register("deployTo" + name, Deploy.class, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.getPlugins().from(environment.getPlugins());
                task.getPluginsDir().set(project.file(environment.getPluginsDirProperty()));
                task.dependsOn(tasks.named(ASSEMBLE_TASK_NAME));
            });

            final TaskProvider<Undeploy> undeployPlugin = tasks.register("undeployFrom" + name, Undeploy.class, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.getPlugins().from(environment.getPlugins());
                task.getPluginsDir().set(project.file(environment.getPluginsDirProperty()));
            });

            if (TeamCityVersion.version(environment.getVersion()).equalOrGreaterThan(VERSION_2018_2)) {
                final File dataDir = project.file(environment.getDataDirProperty().get());
                deployPlugin.configure(task -> {
                    Set<File> plugins = ((FileCollection) environment.getPlugins()).getFiles();
                    List<String> disabledPlugins = new ArrayList<>();
                    task.doFirst(new DisablePluginAction(project.getLogger(), dataDir, plugins, disabledPlugins));
                    task.doLast(new EnablePluginAction(project.getLogger(), dataDir, plugins, disabledPlugins));
                });
                undeployPlugin.configure(task -> {
                    Set<File> plugins = ((FileCollection) environment.getPlugins()).getFiles();
                    task.doFirst(new DisablePluginAction(project.getLogger(), dataDir, plugins, new ArrayList<>()));
                });
            }

            tasks.withType(ServerPlugin.class, task -> {
                if (((FileCollection) environment.getPlugins()).isEmpty()) {
                    environment.plugins(tasks.named(SERVER_PLUGIN_TASK_NAME));
                }
            });
        }

        private void configureLocalEnvironmentTasks(Project project, DefaultLocalTeamCityEnvironment environment) {
            final TaskContainer tasks = project.getTasks();
            final String name = capitalize(environment.getName());
            final String downloadTaskName = "download" + name;
            tasks.register(downloadTaskName, DownloadTeamCity.class, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.src(environment.getDownloadUrl());
                task.dest(project.file(environment.getInstallerFile()));
            });

            tasks.register("install" + name, InstallTeamCity.class, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.getSource().set(project.file(environment.getInstallerFile()));
                task.getTarget().set(project.file(environment.getHomeDirProperty()));
                task.dependsOn(tasks.named(downloadTaskName));
            });

            tasks.register("start" + name + "Server", StartServer.class, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.getVersion().set(environment.getVersion());
                task.getHomeDir().set(environment.getHomeDirProperty());
                task.getDataDir().set(environment.getDataDirProperty());
                task.getJavaHome().set(environment.getJavaHomeProperty());
                task.getServerOptions().set(environment.getServerOptionsProvider());
                task.doFirst(t -> project.mkdir(environment.getDataDir()));
                task.dependsOn(tasks.named("deployTo" + name));
            });

            tasks.register("stop" + name + "Server", StopServer.class, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.getVersion().set(environment.getVersion());
                task.getHomeDir().set(environment.getHomeDirProperty());
                task.getJavaHome().set(environment.getJavaHomeProperty());
                task.finalizedBy(tasks.named("undeployFrom" + name));
            });

            tasks.register("start" + name + "Agent", StartAgent.class, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.getVersion().set(environment.getVersion());
                task.getHomeDir().set(environment.getHomeDirProperty());
                task.getJavaHome().set(environment.getJavaHomeProperty());
                task.getAgentOptions().set(environment.getAgentOptionsProvider());
            });

            tasks.register("stop" + name + "Agent", StopAgent.class, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.getVersion().set(environment.getVersion());
                task.getHomeDir().set(environment.getHomeDirProperty());
                task.getJavaHome().set(environment.getJavaHomeProperty());
            });
        }

        private void configureDockerEnvironmentTasks(Project project, DefaultLocalTeamCityEnvironment environment) {
            final TaskContainer tasks = project.getTasks();
            final String name = capitalize(environment.getName());
            final String startServerTaskName = "start" + name + "Server";
            tasks.register(startServerTaskName, StartDockerServer.class, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.getVersion().set(environment.getVersion());
                task.getDataDir().set(environment.getDataDirProperty());
                task.getServerOptions().set(environment.getServerOptionsProvider());
                task.getImageName().set(environment.getDockerOptions().getServerImage());
                task.getContainerName().set(environment.getDockerOptions().getServerName());
                task.doFirst(t -> project.mkdir(environment.getDataDir()));
                task.dependsOn(tasks.named("deployTo" + name));
            });

            tasks.register("stop" + name + "Server", StopDockerServer.class, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.getContainerName().set(environment.getDockerOptions().getServerName());
                task.finalizedBy(tasks.named("undeployFrom" + name));
            });

            tasks.register("start" + name + "Agent", StartDockerAgent.class, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.getVersion().set(environment.getVersion());
                task.getAgentOptions().set(environment.getAgentOptionsProvider());
                task.getImageName().set(environment.getDockerOptions().getAgentImage());
                task.getContainerName().set(environment.getDockerOptions().getAgentName());
                task.getServerContainerName().set(environment.getDockerOptions().getServerName());
                task.mustRunAfter(tasks.named(startServerTaskName));
            });

            tasks.register("stop" + name + "Agent", StopDockerAgent.class, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.getContainerName().set(environment.getDockerOptions().getAgentName());
            });
        }

        private void configureCommonTasks(Project project, String name) {
            final TaskContainer tasks = project.getTasks();
            final String startServerTaskName = "start" + name + "Server";
            final String startAgentTaskName = "start" + name + "Agent";
            tasks.register("start" + name, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.setDescription("Starts the TeamCity Server and Build Agent");
                task.dependsOn(tasks.named(startServerTaskName), tasks.named(startAgentTaskName));
            });

            final String stopServerTaskName = "stop" + name + "Server";
            final String stopAgentTaskName = "stop" + name + "Agent";
            tasks.register("stop" + name, task -> {
                task.setGroup(TEAMCITY_GROUP);
                task.setDescription("Stops the TeamCity Server and Build Agent");
                task.dependsOn(tasks.named(stopServerTaskName), tasks.named(stopAgentTaskName));
            });
        }

        private String capitalize(String name) {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }
}
