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

import com.github.rodm.teamcity.internal.DefaultTeamCityEnvironments;
import com.github.rodm.teamcity.internal.DisablePluginAction;
import com.github.rodm.teamcity.internal.EnablePluginAction;
import com.github.rodm.teamcity.tasks.Deploy;
import com.github.rodm.teamcity.tasks.ServerPlugin;
import com.github.rodm.teamcity.tasks.Undeploy;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP;
import static com.github.rodm.teamcity.TeamCityServerPlugin.SERVER_PLUGIN_TASK_NAME;
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.ASSEMBLE_TASK_NAME;

public class TeamCityBaseEnvironmentsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(TeamCityPlugin.class);

        TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension.class);
        TeamCityEnvironments environments = extension.getExtensions().create(TeamCityEnvironments.class, "environments", DefaultTeamCityEnvironments.class);
        configureEnvironments(project, (DefaultTeamCityEnvironments) environments);
    }

    private static void configureEnvironments(final Project project, final DefaultTeamCityEnvironments environments) {
        NamedDomainObjectContainer<TeamCityEnvironment> container = environments.getEnvironments();
        container.withType(TeamCityEnvironment.class).all(environment -> {
            configureDeploymentTasks(project, (BaseTeamCityEnvironment) environment);
            configureLifecycleTasks(project, (BaseTeamCityEnvironment) environment);
        });
    }

    private static void configureDeploymentTasks(Project project, BaseTeamCityEnvironment environment) {
        final TaskContainer tasks = project.getTasks();
        tasks.register(environment.deployTaskName(), Deploy.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getPlugins().from(environment.getPlugins());
            task.getPluginsDir().set(project.file(environment.getPluginsDirProperty()));
            task.dependsOn(tasks.named(ASSEMBLE_TASK_NAME));

            if (TeamCityVersion.version(environment.getVersion()).equalOrGreaterThan(VERSION_2018_2)) {
                final File dataDir = project.file(environment.getDataDirProperty().get());
                Set<File> plugins = ((FileCollection) environment.getPlugins()).getFiles();
                List<String> disabledPlugins = new ArrayList<>();
                task.doFirst(new DisablePluginAction(project.getLogger(), dataDir, plugins, disabledPlugins));
                task.doLast(new EnablePluginAction(project.getLogger(), dataDir, plugins, disabledPlugins));
            }
        });

        tasks.register(environment.undeployTaskName(), Undeploy.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getPlugins().from(environment.getPlugins());
            task.getPluginsDir().set(project.file(environment.getPluginsDirProperty()));

            if (TeamCityVersion.version(environment.getVersion()).equalOrGreaterThan(VERSION_2018_2)) {
                final File dataDir = project.file(environment.getDataDirProperty().get());
                Set<File> plugins = ((FileCollection) environment.getPlugins()).getFiles();
                task.doFirst(new DisablePluginAction(project.getLogger(), dataDir, plugins, new ArrayList<>()));
            }
        });

        tasks.withType(ServerPlugin.class, task -> {
            if (((FileCollection) environment.getPlugins()).isEmpty()) {
                environment.plugins(tasks.named(SERVER_PLUGIN_TASK_NAME));
            }
        });
    }

    private static void configureLifecycleTasks(Project project, BaseTeamCityEnvironment environment) {
        final TaskContainer tasks = project.getTasks();
        tasks.register(environment.startTaskName(), task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.setDescription("Starts the TeamCity Server and Build Agent");
        });

        tasks.register(environment.stopTaskName(), task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.setDescription("Stops the TeamCity Server and Build Agent");
        });
    }
}
