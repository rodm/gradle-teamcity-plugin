/*
 * Copyright 2015 Rod MacKenzie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rodm.teamcity

import com.github.rodm.teamcity.tasks.GenerateAgentPluginDescriptor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

class TeamCityAgentPlugin implements Plugin<Project> {

    static final String PLUGIN_DESCRIPTOR_FILENAME = 'teamcity-plugin.xml'

    static final String PLUGIN_DESCRIPTOR_DIR = 'descriptor'

    static final String TEAMCITY_EXTENSION_NAME = 'teamcity'

    static final String JETBRAINS_MAVEN_REPOSITORY = 'http://repository.jetbrains.com/all'

    void apply(Project project) {
        project.plugins.apply(BasePlugin)
        TeamCityPluginExtension extension = project.extensions.create(TEAMCITY_EXTENSION_NAME, TeamCityPluginExtension)

        configureRepositories(project)
        configureConfigurations(project)

        project.afterEvaluate {
            configureAgentPluginTasks(project, extension)
        }
    }

    private configureRepositories(Project project) {
        if (project.plugins.hasPlugin(JavaPlugin)) {
            project.repositories {
                mavenCentral()
                maven {
                    url = JETBRAINS_MAVEN_REPOSITORY
                }
            }
        }
    }

    void configureConfigurations(final Project project) {
        ConfigurationContainer configurations = project.getConfigurations();
        configurations.create('agent')
                .setVisible(false)
                .setTransitive(false)
                .setDescription("Configuration for agent plugin.");
        configurations.create('server')
                .setVisible(false)
                .setTransitive(false)
                .setDescription("Configuration for server plugin.");
        configurations.create('plugin')
                .setVisible(false)
                .setTransitive(false)
                .setDescription('Configuration for plugin artifact.')
        if (project.plugins.hasPlugin(JavaPlugin)) {
            Configuration providedCompileConfiguration = configurations.create('providedCompile')
                    .setVisible(false)
                    .setDescription('Additional compile classpath for TeamCity libraries that will not be part of the plugin archive.')
            configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME).extendsFrom(providedCompileConfiguration)
        }
    }

    void configureAgentPluginTasks(Project project, TeamCityPluginExtension extension) {
        if ("agent-plugin" == extension.type) {
            if (project.plugins.hasPlugin(JavaPlugin)) {
                project.dependencies {
                    providedCompile "org.jetbrains.teamcity:agent-api:${extension.version}"
                }
            }

            def packagePlugin = project.tasks.create('packageAgentPlugin', Zip)
            packagePlugin.description = 'Package TeamCity Agent plugin'
            packagePlugin.group = 'TeamCity'
            packagePlugin.with {
                into("lib") {
                    if (project.plugins.hasPlugin(JavaPlugin)) {
                        def jar = project.tasks[JavaPlugin.JAR_TASK_NAME]
                        from(jar)
                        from(project.configurations.runtime - project.configurations.providedCompile)
                    }
                    from(project.configurations.agent)
                }
                into('') {
                    from {
                        new File(project.getBuildDir(), PLUGIN_DESCRIPTOR_DIR + "/" + PLUGIN_DESCRIPTOR_FILENAME)
                    }
                    rename {
                        PLUGIN_DESCRIPTOR_FILENAME
                    }
                }
            }
            packagePlugin.onlyIf { extension.descriptor != null }

            def assemble = project.tasks['assemble']
            assemble.dependsOn packagePlugin

            if (extension.descriptor instanceof File) {
                def processDescriptor = project.tasks.create('processAgentDescriptor', Copy)
                processDescriptor.with {
                    from(extension.descriptor)
                    into("$project.buildDir/$PLUGIN_DESCRIPTOR_DIR")
                }
                processDescriptor.onlyIf { extension.descriptor != null }
                packagePlugin.dependsOn processDescriptor
            } else {
                def generateDescriptor = project.tasks.create('generateAgentDescriptor', GenerateAgentPluginDescriptor)
                generateDescriptor.onlyIf { extension.descriptor != null }
                packagePlugin.dependsOn generateDescriptor
            }

            ArchivePublishArtifact pluginArtifact = new ArchivePublishArtifact(packagePlugin);
            project.getConfigurations().getByName('plugin').getArtifacts().add(pluginArtifact)
        }
    }
}
