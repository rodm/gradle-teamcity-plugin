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
package com.github.rodm.teamcity

import com.github.rodm.teamcity.internal.DefaultPublishConfiguration
import com.github.rodm.teamcity.internal.DefaultSignConfiguration
import com.github.rodm.teamcity.internal.PluginDescriptorContentsValidationAction
import com.github.rodm.teamcity.internal.PluginDescriptorValidationAction
import com.github.rodm.teamcity.tasks.GenerateServerPluginDescriptor
import com.github.rodm.teamcity.tasks.ProcessDescriptor
import com.github.rodm.teamcity.tasks.PublishPlugin
import com.github.rodm.teamcity.tasks.SignPlugin
import com.github.rodm.teamcity.tasks.ServerPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip

import static com.github.rodm.teamcity.TeamCityAgentPlugin.AGENT_PLUGIN_TASK_NAME
import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_DIR
import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME
import static com.github.rodm.teamcity.TeamCityPlugin.TEAMCITY_GROUP
import static com.github.rodm.teamcity.TeamCityPlugin.configureJarTask
import static com.github.rodm.teamcity.TeamCityPlugin.configurePluginArchiveTask
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2020_1
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_9_0
import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME
import static org.gradle.language.base.plugins.LifecycleBasePlugin.ASSEMBLE_TASK_NAME

class TeamCityServerPlugin implements Plugin<Project> {

    public static final String PLUGIN_DEFINITION_PATTERN = "META-INF/build-server-plugin*.xml"

    public static final String SERVER_PLUGIN_DESCRIPTOR_DIR = PLUGIN_DESCRIPTOR_DIR + '/server'

    public static final String PROCESS_SERVER_DESCRIPTOR_TASK_NAME = 'processServerDescriptor'
    public static final String GENERATE_SERVER_DESCRIPTOR_TASK_NAME = 'generateServerDescriptor'
    public static final String SERVER_PLUGIN_TASK_NAME = 'serverPlugin'
    public static final String PUBLISH_PLUGIN_TASK_NAME = 'publishPlugin'
    public static final String SIGN_PLUGIN_TASK_NAME = 'signPlugin'

    void apply(Project project) {
        project.plugins.apply(TeamCityPlugin)

        project.plugins.withType(JavaPlugin) {
            if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
                throw new GradleException("Cannot apply both the teamcity-agent and teamcity-server plugins with the Java plugin")
            }
        }

        configureConfigurations(project)

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        configureDependencies(project, extension)
        configureJarTask(project, extension, PLUGIN_DEFINITION_PATTERN)
        configureServerPluginTasks(project, extension)
        configureSignPluginTask(project, extension)
        configurePublishPluginTask(project, extension)
        configureEnvironmentTasks(project, extension)
    }

    static void configureConfigurations(final Project project) {
        ConfigurationContainer configurations = project.getConfigurations()
        configurations.maybeCreate('marketplace')
            .setVisible(false)
            .setDescription("Configuration for signing and publishing task dependencies.")
            .defaultDependencies { dependencies ->
                dependencies.add(project.dependencies.create('org.jetbrains:marketplace-zip-signer:0.1.3'))
                dependencies.add(project.dependencies.create('org.jetbrains.intellij.plugins:structure-base:3.171'))
                dependencies.add(project.dependencies.create('org.jetbrains.intellij.plugins:structure-teamcity:3.171'))
                dependencies.add(project.dependencies.create('org.jetbrains.intellij:plugin-repository-rest-client:2.0.17'))
            }
    }

    void configureDependencies(Project project, TeamCityPluginExtension extension) {
        project.plugins.withType(JavaPlugin) {
            project.afterEvaluate {
                project.dependencies {
                    provided "org.jetbrains.teamcity:server-api:${extension.version}"
                    if (TeamCityVersion.version(extension.version, extension.allowSnapshotVersions) >= VERSION_9_0) {
                        provided "org.jetbrains.teamcity:server-web-api:${extension.version}"
                    }

                    testImplementation "org.jetbrains.teamcity:tests-support:${extension.version}"
                }
            }
        }
    }

    @SuppressWarnings('GrMethodMayBeStatic')
    void configureServerPluginTasks(Project project, TeamCityPluginExtension extension) {
        project.plugins.withType(JavaPlugin) {
            project.tasks.named(JAR_TASK_NAME, Jar).configure {
                into('buildServerResources') {
                    from(extension.server.web)
                }
            }
        }

        def descriptorFile = project.layout.buildDirectory.file(SERVER_PLUGIN_DESCRIPTOR_DIR + '/' + PLUGIN_DESCRIPTOR_FILENAME)

        def processDescriptor = project.tasks.register(PROCESS_SERVER_DESCRIPTOR_TASK_NAME, ProcessDescriptor) {
            descriptor.set(extension.server.descriptorFile)
            tokens.set(project.providers.provider({ extension.server.tokens }))
            destination.set(descriptorFile)
        }

        def generateDescriptor = project.tasks.register(GENERATE_SERVER_DESCRIPTOR_TASK_NAME, GenerateServerPluginDescriptor) {
            version.set(project.providers.provider({ extension.version }))
            allowSnapshotVersions.set(project.providers.provider({ extension.allowSnapshotVersions }))
            descriptor.set(project.providers.provider({ extension.server.descriptor }))
            destination.set(descriptorFile)
        }

        def packagePlugin = project.tasks.register(SERVER_PLUGIN_TASK_NAME, ServerPlugin) {
            group = TEAMCITY_GROUP
            onlyIf { extension.server.descriptor != null || extension.server.descriptorFile.isPresent() }
            descriptor.set(descriptorFile)
            server.from(project.configurations.server)
            project.plugins.withType(JavaPlugin) {
                server.from(project.tasks.named(JAR_TASK_NAME))
                server.from(project.configurations.runtimeClasspath)
            }
            if (project.plugins.hasPlugin(TeamCityAgentPlugin)) {
                agent.from(project.tasks.named(AGENT_PLUGIN_TASK_NAME))
            } else {
                agent.from(project.configurations.agent)
            }
            with(extension.server.files)
            dependsOn processDescriptor, generateDescriptor
        }

        project.tasks.withType(ServerPlugin).configureEach {
            doLast(new PluginDescriptorValidationAction(getSchemaPath(extension.version, extension.allowSnapshotVersions)))
            doLast(new PluginDescriptorContentsValidationAction())
        }

        project.tasks.named(ASSEMBLE_TASK_NAME) {
            dependsOn packagePlugin
        }

        project.artifacts.add('plugin', packagePlugin)

        project.afterEvaluate {
            project.tasks.named(SERVER_PLUGIN_TASK_NAME) { Zip task ->
                configurePluginArchiveTask(task, extension.server.archiveName)
            }
        }
    }

    private static String getSchemaPath(String version, boolean allowSnapshots) {
        def teamcityVersion = TeamCityVersion.version(version, allowSnapshots)
        if (teamcityVersion >= VERSION_2020_1) {
            '2020.1/teamcity-server-plugin-descriptor.xsd'
        } else if (teamcityVersion >= VERSION_2018_2) {
            '2018.2/teamcity-server-plugin-descriptor.xsd'
        } else {
            'teamcity-server-plugin-descriptor.xsd'
        }
    }

    private static void configureEnvironmentTasks(Project project, TeamCityPluginExtension extension) {
        project.afterEvaluate {
            if (!project.plugins.hasPlugin(TeamCityEnvironmentsPlugin)) {
                new TeamCityEnvironmentsPlugin.ConfigureEnvironmentTasksAction(extension)
            }
        }
    }

    private static void configureSignPluginTask(Project project, TeamCityPluginExtension extension) {
        project.afterEvaluate {
            if (extension.server.sign) {
                DefaultSignConfiguration configuration = extension.server.sign as DefaultSignConfiguration
                Zip packagePlugin = project.tasks.findByName(SERVER_PLUGIN_TASK_NAME) as Zip

                project.tasks.register(SIGN_PLUGIN_TASK_NAME, SignPlugin) {
                    group = TEAMCITY_GROUP
                    classpath = project.configurations.marketplace
                    certificateChain.set(configuration.certificateChainProperty)
                    privateKey.set(configuration.privateKeyProperty)
                    password.set(configuration.passwordProperty)
                    pluginFile.set(packagePlugin.archiveFile)
                    dependsOn(packagePlugin)
                }
            }
        }
    }

    private static void configurePublishPluginTask(Project project, TeamCityPluginExtension extension) {
        project.afterEvaluate {
            if (extension.server.publish) {
                DefaultPublishConfiguration configuration = extension.server.publish as DefaultPublishConfiguration
                SignPlugin signTask = project.tasks.findByName(SIGN_PLUGIN_TASK_NAME) as SignPlugin
                Zip packagePlugin = project.tasks.findByName(SERVER_PLUGIN_TASK_NAME) as Zip

                project.tasks.register(PUBLISH_PLUGIN_TASK_NAME, PublishPlugin) {
                    group = TEAMCITY_GROUP
                    classpath = project.configurations.marketplace
                    channels.set(configuration.channelsProperty)
                    token.set(configuration.tokenProperty)
                    notes.set(configuration.notesProperty)
                    distributionFile.set(signTask != null ? signTask.signedPluginFile : packagePlugin.archiveFile)
                    if (signTask != null) {
                        dependsOn(signTask)
                    } else {
                        dependsOn(packagePlugin)
                    }
                }
            }
        }
    }
}
