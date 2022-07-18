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

import com.github.rodm.teamcity.internal.DefaultPublishConfiguration;
import com.github.rodm.teamcity.internal.DefaultSignConfiguration;
import com.github.rodm.teamcity.internal.DefaultTeamCityPluginExtension;
import com.github.rodm.teamcity.internal.PluginDescriptorContentsValidationAction;
import com.github.rodm.teamcity.internal.PluginDescriptorValidationAction;
import com.github.rodm.teamcity.tasks.GenerateServerPluginDescriptor;
import com.github.rodm.teamcity.tasks.ProcessDescriptor;
import com.github.rodm.teamcity.tasks.PublishPlugin;
import com.github.rodm.teamcity.tasks.ServerPlugin;
import com.github.rodm.teamcity.tasks.SignPlugin;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.bundling.Zip;

import static com.github.rodm.teamcity.TeamCityAgentPlugin.AGENT_PLUGIN_TASK_NAME;
import static com.github.rodm.teamcity.TeamCityPlugin.AGENT_CONFIGURATION_NAME;
import static com.github.rodm.teamcity.TeamCityPlugin.AGENT_PLUGIN_ID;
import static com.github.rodm.teamcity.TeamCityPlugin.GRADLE_OFFLINE;
import static com.github.rodm.teamcity.TeamCityPlugin.JAVA_PLUGIN_ID;
import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_CONFIGURATION_NAME;
import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_DIR;
import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME;
import static com.github.rodm.teamcity.TeamCityPlugin.PROVIDED_CONFIGURATION_NAME;
import static com.github.rodm.teamcity.TeamCityPlugin.SERVER_CONFIGURATION_NAME;
import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP;
import static com.github.rodm.teamcity.TeamCityPlugin.configureJarTask;
import static com.github.rodm.teamcity.TeamCityPlugin.configurePluginArchiveTask;
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2;
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2020_1;
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_9_0;
import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.ASSEMBLE_TASK_NAME;

public class TeamCityServerPlugin implements Plugin<Project> {

    public static final String PLUGIN_DEFINITION_PATTERN = "META-INF/build-server-plugin*.xml";

    public static final String SERVER_PLUGIN_DESCRIPTOR_DIR = PLUGIN_DESCRIPTOR_DIR + "/server";

    public static final String PROCESS_SERVER_DESCRIPTOR_TASK_NAME = "processServerDescriptor";
    public static final String GENERATE_SERVER_DESCRIPTOR_TASK_NAME = "generateServerDescriptor";
    public static final String SERVER_PLUGIN_TASK_NAME = "serverPlugin";
    public static final String PUBLISH_PLUGIN_TASK_NAME = "publishPlugin";
    public static final String SIGN_PLUGIN_TASK_NAME = "signPlugin";

    private static final String MARKETPLACE_CONFIGURATION_NAME = "marketplace";
    private static final String BUILD_SERVER_RESOURCES_PATH = "buildServerResources";

    public void apply(final Project project) {
        PluginManager plugins = project.getPluginManager();
        plugins.apply(TeamCityPlugin.class);

        plugins.withPlugin(JAVA_PLUGIN_ID, plugin -> {
            if (plugins.hasPlugin(AGENT_PLUGIN_ID)) {
                throw new GradleException("Cannot apply both the teamcity-agent and teamcity-server plugins with the Java plugin");
            }
        });

        configureConfigurations(project);

        TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension.class);
        configureDependencies(project, (DefaultTeamCityPluginExtension) extension);
        configureJarTask(project, extension, PLUGIN_DEFINITION_PATTERN);
        configureServerPluginTasks(project, extension);
        configureSignPluginTask(project, extension);
        configurePublishPluginTask(project, extension);
        configureEnvironmentTasks(project, extension);
    }

    public static void configureConfigurations(final Project project) {
        ConfigurationContainer configurations = project.getConfigurations();
        configurations.maybeCreate(MARKETPLACE_CONFIGURATION_NAME)
            .setVisible(false)
            .setDescription("Configuration for signing and publishing task dependencies.")
            .defaultDependencies(dependencies -> {
                dependencies.add(project.getDependencies().create("org.jetbrains:marketplace-zip-signer:0.1.3"));
                dependencies.add(project.getDependencies().create("org.jetbrains.intellij.plugins:structure-base:3.171"));
                dependencies.add(project.getDependencies().create("org.jetbrains.intellij.plugins:structure-teamcity:3.171"));
                dependencies.add(project.getDependencies().create("org.jetbrains.intellij:plugin-repository-rest-client:2.0.17"));
            });
    }

    public void configureDependencies(final Project project, final DefaultTeamCityPluginExtension extension) {
        Provider<String> version = extension.getVersionProperty();
        Property<Boolean> allowSnapshotVersions = extension.getAllowSnapshotVersionsProperty();
        project.getPluginManager().withPlugin(JAVA_PLUGIN_ID, plugin -> {
            project.getDependencies().add(PROVIDED_CONFIGURATION_NAME, version.map(v -> "org.jetbrains.teamcity:server-api:" + v));
            project.getDependencies().add(PROVIDED_CONFIGURATION_NAME, version.map(v -> {
                TeamCityVersion teamCityVersion = TeamCityVersion.version(version.get(), allowSnapshotVersions.get());
                return teamCityVersion.equalOrGreaterThan(VERSION_9_0) ? "org.jetbrains.teamcity:server-web-api:" + v : null;
            }));
            project.getDependencies().add(TEST_IMPLEMENTATION_CONFIGURATION_NAME, version.map(v -> "org.jetbrains.teamcity:tests-support:" + v));
        });
    }

    public void configureServerPluginTasks(final Project project, final TeamCityPluginExtension extension) {
        final PluginManager plugins = project.getPluginManager();
        final TaskContainer tasks = project.getTasks();
        final ServerPluginConfiguration server = extension.getServer();

        plugins.withPlugin(JAVA_PLUGIN_ID, plugin ->
            tasks.named(JAR_TASK_NAME, Jar.class).configure(task ->
                task.into(BUILD_SERVER_RESOURCES_PATH, copySpec ->
                    copySpec.from(server.getWeb()))));

        final Provider<RegularFile> descriptorFile = project.getLayout().getBuildDirectory().file(SERVER_PLUGIN_DESCRIPTOR_DIR + "/" + PLUGIN_DESCRIPTOR_FILENAME);

        final TaskProvider<ProcessDescriptor> processDescriptor = tasks.register(PROCESS_SERVER_DESCRIPTOR_TASK_NAME, ProcessDescriptor.class, task -> {
            task.getDescriptor().set(server.getDescriptorFile());
            task.getTokens().set(server.getTokens());
            task.getDestination().set(descriptorFile);
        });

        final TaskProvider<GenerateServerPluginDescriptor> generateDescriptor = tasks.register(GENERATE_SERVER_DESCRIPTOR_TASK_NAME, GenerateServerPluginDescriptor.class, task -> {
            task.getVersion().set(project.getProviders().provider(() -> TeamCityVersion.version(extension.getVersion(), extension.getAllowSnapshotVersions())));
            task.getDescriptor().set(project.getProviders().provider(() -> (ServerPluginDescriptor) server.getDescriptor()));
            task.getDestination().set(descriptorFile);
        });

        final TaskProvider<ServerPlugin> packagePlugin = tasks.register(SERVER_PLUGIN_TASK_NAME, ServerPlugin.class, task -> {
            task.setGroup(TEAMCITY_GROUP);
            task.getDescriptor().set(descriptorFile);
            task.getServer().from(project.getConfigurations().getByName(SERVER_CONFIGURATION_NAME));
            plugins.withPlugin(JAVA_PLUGIN_ID, plugin -> {
                task.getServer().from(tasks.named(JAR_TASK_NAME));
                task.getServer().from(project.getConfigurations().getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME));
            });
            if (plugins.hasPlugin(AGENT_PLUGIN_ID)) {
                task.getAgent().from(tasks.named(AGENT_PLUGIN_TASK_NAME));
            } else {
                task.getAgent().from((project.getConfigurations().getByName(AGENT_CONFIGURATION_NAME)));
            }
            task.with(server.getFiles());
            task.dependsOn(processDescriptor, generateDescriptor);
        });

        tasks.withType(ServerPlugin.class).configureEach(task -> {
            String schemaPath = getSchemaPath(extension.getVersion(), extension.getAllowSnapshotVersions());
            task.doLast(new PluginDescriptorValidationAction(schemaPath));
            task.doLast(new PluginDescriptorContentsValidationAction());
        });

        tasks.named(ASSEMBLE_TASK_NAME, task -> task.dependsOn(packagePlugin));

        project.getArtifacts().add(PLUGIN_CONFIGURATION_NAME, packagePlugin);

        tasks.named(SERVER_PLUGIN_TASK_NAME, Zip.class, task ->
            configurePluginArchiveTask(task, server.getArchiveName()));
    }

    private static String getSchemaPath(String version, boolean allowSnapshots) {
        TeamCityVersion teamcityVersion = TeamCityVersion.version(version, allowSnapshots);
        if (teamcityVersion.equalOrGreaterThan(VERSION_2020_1)) {
            return "2020.1/teamcity-server-plugin-descriptor.xsd";
        } else if (teamcityVersion.equalOrGreaterThan(VERSION_2018_2)) {
            return "2018.2/teamcity-server-plugin-descriptor.xsd";
        } else {
            return "teamcity-server-plugin-descriptor.xsd";
        }
    }

    private static void configureEnvironmentTasks(final Project project, final TeamCityPluginExtension extension) {
        project.afterEvaluate(p -> {
            if (!p.getPlugins().hasPlugin(TeamCityEnvironmentsPlugin.class)) {
                new TeamCityEnvironmentsPlugin.ConfigureEnvironmentTasksAction(extension);
            }
        });
    }

    private static void configureSignPluginTask(final Project project, final TeamCityPluginExtension extension) {
        project.afterEvaluate(p -> {
            if (extension.getServer().getSign() != null) {
                DefaultSignConfiguration configuration = (DefaultSignConfiguration) extension.getServer().getSign();
                Zip packagePlugin = (Zip) p.getTasks().findByName(SERVER_PLUGIN_TASK_NAME);

                p.getTasks().register(SIGN_PLUGIN_TASK_NAME, SignPlugin.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.setClasspath(p.getConfigurations().getByName(MARKETPLACE_CONFIGURATION_NAME));
                    task.getCertificateChain().set(configuration.getCertificateChainProperty());
                    task.getPrivateKey().set(configuration.getPrivateKeyProperty());
                    task.getPassword().set(configuration.getPasswordProperty());
                    task.getPluginFile().set(packagePlugin.getArchiveFile());
                    task.dependsOn(packagePlugin);
                });
            }
        });
    }

    private static void configurePublishPluginTask(final Project project, final TeamCityPluginExtension extension) {
        project.afterEvaluate(p -> {
            if (extension.getServer().getPublish() != null) {
                DefaultPublishConfiguration configuration = (DefaultPublishConfiguration) extension.getServer().getPublish();
                SignPlugin signTask = (SignPlugin) p.getTasks().findByName(SIGN_PLUGIN_TASK_NAME);
                Zip packagePlugin = (Zip) p.getTasks().getByName(SERVER_PLUGIN_TASK_NAME);
                Provider<RegularFile> distributionFile = signTask != null ? signTask.getSignedPluginFile() : packagePlugin.getArchiveFile();

                p.getTasks().register(PUBLISH_PLUGIN_TASK_NAME, PublishPlugin.class, task -> {
                    task.setGroup(TEAMCITY_GROUP);
                    task.getInputs().property(GRADLE_OFFLINE, project.getGradle().getStartParameter().isOffline());
                    task.setClasspath(p.getConfigurations().getByName(MARKETPLACE_CONFIGURATION_NAME));
                    task.getChannels().set(configuration.getChannels());
                    task.getToken().set(configuration.getTokenProperty());
                    task.getNotes().set(configuration.getNotesProperty());
                    task.getDistributionFile().set(distributionFile);
                    if (signTask != null) {
                        task.dependsOn(signTask);
                    } else {
                        task.dependsOn(packagePlugin);
                    }
                });
            }
        });
    }
}
