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

import com.github.rodm.teamcity.internal.DefaultTeamCityPluginExtension;
import com.github.rodm.teamcity.internal.FileCollectorAction;
import com.github.rodm.teamcity.internal.PluginDescriptorValidationAction;
import com.github.rodm.teamcity.internal.PluginExecutableFilesValidationAction;
import com.github.rodm.teamcity.tasks.AgentPlugin;
import com.github.rodm.teamcity.tasks.GenerateAgentPluginDescriptor;
import com.github.rodm.teamcity.tasks.ProcessDescriptor;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_DIR;
import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME;
import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP;
import static com.github.rodm.teamcity.TeamCityPlugin.configureJarTask;
import static com.github.rodm.teamcity.TeamCityPlugin.configurePluginArchiveTask;
import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.ASSEMBLE_TASK_NAME;

public class TeamCityAgentPlugin implements Plugin<Project> {

    public static final String PLUGIN_DEFINITION_PATTERN = "META-INF/build-agent-plugin*.xml";

    public static final String AGENT_PLUGIN_DESCRIPTOR_DIR = PLUGIN_DESCRIPTOR_DIR + "/agent";

    public static final String PROCESS_AGENT_DESCRIPTOR_TASK_NAME = "processAgentDescriptor";
    public static final String GENERATE_AGENT_DESCRIPTOR_TASK_NAME = "generateAgentDescriptor";
    public static final String AGENT_PLUGIN_TASK_NAME = "agentPlugin";

    public void apply(final Project project) {
        PluginManager plugins = project.getPluginManager();
        plugins.apply(TeamCityPlugin.class);

        plugins.withPlugin("org.gradle.java", javaPlugin -> {
            if (plugins.hasPlugin("com.github.rodm.teamcity-server")) {
                throw new GradleException("Cannot apply both the teamcity-agent and teamcity-server plugins with the Java plugin");
            }
        });

        TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension.class);
        configureDependencies(project, (DefaultTeamCityPluginExtension) extension);
        configureJarTask(project, extension, PLUGIN_DEFINITION_PATTERN);
        configureTasks(project, extension);
    }

    public void configureDependencies(final Project project, final DefaultTeamCityPluginExtension extension) {
        Provider<String> version = extension.getVersionProperty();
        project.getPluginManager().withPlugin("org.gradle.java", plugin -> {
            project.getDependencies().add("provided", version.map(v -> "org.jetbrains.teamcity:agent-api:" + v));
            project.getDependencies().add("testImplementation", version.map(v ->"org.jetbrains.teamcity:tests-support:" + v));
        });
    }

    public void configureTasks(final Project project, final TeamCityPluginExtension extension) {
        final TaskContainer tasks = project.getTasks();
        final AgentPluginConfiguration agent = extension.getAgent();
        final Provider<RegularFile> descriptorFile = project.getLayout().getBuildDirectory().file(AGENT_PLUGIN_DESCRIPTOR_DIR + "/" + PLUGIN_DESCRIPTOR_FILENAME);

        final TaskProvider<ProcessDescriptor> processDescriptor =
            tasks.register(PROCESS_AGENT_DESCRIPTOR_TASK_NAME, ProcessDescriptor.class, task -> {
                task.getDescriptor().set(agent.getDescriptorFile());
                task.getTokens().set(agent.getTokens());
                task.getDestination().set(descriptorFile);
            });

        final TaskProvider<GenerateAgentPluginDescriptor> generateDescriptor =
            project.getTasks().register(GENERATE_AGENT_DESCRIPTOR_TASK_NAME, GenerateAgentPluginDescriptor.class, task -> {
                task.getVersion().set(project.getProviders().provider(() -> TeamCityVersion.version(extension.getVersion(), extension.getAllowSnapshotVersions())));
                task.getDescriptor().set(project.getProviders().provider(() -> (AgentPluginDescriptor) agent.getDescriptor()));
                task.getDestination().set(descriptorFile);
            });

        final TaskProvider<AgentPlugin> packagePlugin =
            project.getTasks().register(AGENT_PLUGIN_TASK_NAME, AgentPlugin.class, task -> {
            task.setDescription(TEAMCITY_GROUP);
            task.getDescriptor().set(descriptorFile);
            task.getLib().from(project.getConfigurations().getByName("agent"));
            project.getPluginManager().withPlugin("org.gradle.java", plugin -> {
                task.getLib().from(project.getTasks().named(JAR_TASK_NAME));
                task.getLib().from(project.getConfigurations().getByName("runtimeClasspath"));
            });
            task.with(agent.getFiles());
            task.dependsOn(processDescriptor, generateDescriptor);
        });

        project.getTasks().withType(AgentPlugin.class).configureEach(task -> {
            task.doLast(new PluginDescriptorValidationAction("teamcity-agent-plugin-descriptor.xsd"));
            Set<FileCopyDetails> files = new LinkedHashSet<>();
            task.filesMatching("**/*", new FileCollectorAction(files));
            task.doLast(new PluginExecutableFilesValidationAction(files));
        });

        project.getPluginManager().withPlugin("com.github.rodm.teamcity-server", serverPlugin ->
            packagePlugin.configure(agentPlugin ->
                agentPlugin.getArchiveAppendix().convention("agent")));

        tasks.named(ASSEMBLE_TASK_NAME, task -> task.dependsOn(packagePlugin));

        project.getArtifacts().add("plugin", packagePlugin);

        tasks.named(AGENT_PLUGIN_TASK_NAME, Zip.class, task ->
            configurePluginArchiveTask(task, agent.getArchiveName()));
    }
}
