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

import com.github.rodm.teamcity.internal.DefaultTeamCityEnvironment;
import com.github.rodm.teamcity.internal.DefaultTeamCityEnvironments;
import com.github.rodm.teamcity.internal.DisablePluginAction;
import com.github.rodm.teamcity.internal.EnablePluginAction;
import com.github.rodm.teamcity.tasks.Deploy;
import com.github.rodm.teamcity.tasks.DownloadTeamCity;
import com.github.rodm.teamcity.tasks.InstallTeamCity;
import com.github.rodm.teamcity.tasks.ServerPlugin;
import com.github.rodm.teamcity.tasks.StartAgent;
import com.github.rodm.teamcity.tasks.StartServer;
import com.github.rodm.teamcity.tasks.StopAgent;
import com.github.rodm.teamcity.tasks.StopServer;
import com.github.rodm.teamcity.tasks.Undeploy;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP;
import static com.github.rodm.teamcity.TeamCityServerPlugin.SERVER_PLUGIN_TASK_NAME;
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.BUILD_TASK_NAME;

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
            container.all(env -> {
                final DefaultTeamCityEnvironment environment = (DefaultTeamCityEnvironment) env;

                final String name = capitalize(environment.getName());
                final TaskProvider<DownloadTeamCity> download = project.getTasks().register("download" + name, DownloadTeamCity.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.src(environment.getDownloadUrl());
                    task.dest(project.file(environment.getInstallerFile()));
                });
                project.getTasks().register("install" + name, InstallTeamCity.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getSource().set(project.file(environment.getInstallerFile()));
                    task.getTarget().set(project.file(environment.getHomeDirProperty()));
                    task.dependsOn(download);
                });

                final TaskProvider<Deploy> deployPlugin = project.getTasks().register("deployTo" + name, Deploy.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getPlugins().from(environment.getPlugins());
                    task.getPluginsDir().set(project.file(environment.getPluginsDirProperty()));
                    task.dependsOn(project.getTasks().named(BUILD_TASK_NAME));
                });

                final TaskProvider<Undeploy> undeployPlugin = project.getTasks().register("undeployFrom" + name, Undeploy.class, task -> {
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

                final TaskProvider<StartServer> startServer = project.getTasks().register("start" + name + "Server", StartServer.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getVersion().set(environment.getVersion());
                    task.getHomeDir().set(environment.getHomeDirProperty());
                    task.getDataDir().set(environment.getDataDirProperty());
                    task.getJavaHome().set(environment.getJavaHomeProperty());
                    task.getServerOptions().set(environment.getServerOptionsProvider());
                    task.doFirst(t -> project.mkdir(environment.getDataDirProperty()));
                    task.dependsOn(deployPlugin);
                });

                final TaskProvider<StopServer> stopServer = project.getTasks().register("stop" + name + "Server", StopServer.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getVersion().set(environment.getVersion());
                    task.getHomeDir().set(environment.getHomeDirProperty());
                    task.getJavaHome().set(environment.getJavaHomeProperty());
                    task.finalizedBy(undeployPlugin);
                });

                final TaskProvider<StartAgent> startAgent = project.getTasks().register("start" + name + "Agent", StartAgent.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getVersion().set(environment.getVersion());
                    task.getHomeDir().set(environment.getHomeDirProperty());
                    task.getJavaHome().set(environment.getJavaHomeProperty());
                    task.getAgentOptions().set(environment.getAgentOptionsProvider());
                });

                final TaskProvider<StopAgent> stopAgent = project.getTasks().register("stop" + name + "Agent", StopAgent.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getVersion().set(environment.getVersion());
                    task.getHomeDir().set(environment.getHomeDirProperty());
                    task.getJavaHome().set(environment.getJavaHomeProperty());
                });

                project.getTasks().register("start" + name, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.setDescription("Starts the TeamCity Server and Build Agent");
                    task.dependsOn(startServer, startAgent);
                });
                project.getTasks().register("stop" + name, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.setDescription("Stops the TeamCity Server and Build Agent");
                    task.dependsOn(stopAgent, stopServer);
                });

                project.getTasks().withType(ServerPlugin.class, task -> {
                    if (((FileCollection) environment.getPlugins()).isEmpty()) {
                        environment.plugins(project.getTasks().named(SERVER_PLUGIN_TASK_NAME));
                    }
                });
            });
        }

        private String capitalize(String name) {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }
}
