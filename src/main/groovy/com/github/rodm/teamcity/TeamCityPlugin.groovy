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

import com.github.rodm.teamcity.tasks.DeployPlugin
import com.github.rodm.teamcity.tasks.Download
import com.github.rodm.teamcity.tasks.GenerateAgentPluginDescriptor
import com.github.rodm.teamcity.tasks.GenerateServerPluginDescriptor
import com.github.rodm.teamcity.tasks.InstallTeamCity
import com.github.rodm.teamcity.tasks.PackageAgentPlugin
import com.github.rodm.teamcity.tasks.PackagePlugin
import com.github.rodm.teamcity.tasks.ProcessPluginDescriptor
import com.github.rodm.teamcity.tasks.StartAgent
import com.github.rodm.teamcity.tasks.StartServer
import com.github.rodm.teamcity.tasks.StopAgent
import com.github.rodm.teamcity.tasks.StopServer
import com.github.rodm.teamcity.tasks.TeamCityTask
import com.github.rodm.teamcity.tasks.UndeployPlugin
import com.github.rodm.teamcity.tasks.Unpack
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin

import java.util.concurrent.Callable

class TeamCityPlugin implements Plugin<Project> {

    static final String PLUGIN_DESCRIPTOR_FILENAME = 'teamcity-plugin.xml'

    static final String PLUGIN_DESCRIPTOR_DIR = 'descriptor'

    static final String TEAMCITY_EXTENSION_NAME = 'teamcity'

    static final String JETBRAINS_MAVEN_REPOSITORY = 'http://repository.jetbrains.com/all'

    void apply(Project project) {
        project.plugins.apply(JavaPlugin)
        TeamCityPluginExtension extension = project.extensions.create(TEAMCITY_EXTENSION_NAME, TeamCityPluginExtension)

        configureRepositories(project)
        configureConfigurations(project)

        project.afterEvaluate {
            configureAgentPluginTasks(project, extension)
            configureServerPluginTasks(project, extension)
            configureTeamCityTasks(project, extension)
        }
    }

    private configureRepositories(Project project) {
        project.repositories {
            mavenCentral()
            maven {
                url = JETBRAINS_MAVEN_REPOSITORY
            }
        }
    }

    void configureConfigurations(final Project project) {
        ConfigurationContainer configurations = project.getConfigurations();
        configurations.create('agent')
                .setVisible(false)
                .setTransitive(false)
                .setDescription("Configuration for agent plugin.");
    }

    void configureAgentPluginTasks(Project project, TeamCityPluginExtension extension) {
        if ("agent-plugin" == extension.type) {
            project.dependencies {
                compile "org.jetbrains.teamcity:agent-api:${extension.version}"
            }

            def jar = project.tasks[JavaPlugin.JAR_TASK_NAME]
            def packagePlugin = project.tasks.create('packageAgentPlugin', PackageAgentPlugin)
            packagePlugin.dependsOn jar
            packagePlugin.agentComponents = jar.outputs.files

            def assemble = project.tasks['assemble']
            assemble.dependsOn packagePlugin

            if (extension.descriptor instanceof File) {
                def processDescriptor = project.tasks.create('processAgentDescriptor', ProcessPluginDescriptor) {
                    conventionMapping.map("descriptor") { extension.descriptor }
                }
                packagePlugin.dependsOn processDescriptor
            } else {
                def generateDescriptor = project.tasks.create('generateAgentDescriptor', GenerateAgentPluginDescriptor)
                packagePlugin.dependsOn generateDescriptor
            }
        }
    }

    void configureServerPluginTasks(Project project, TeamCityPluginExtension extension) {
        if ("server-plugin" == extension.type) {
            project.dependencies {
                compile "org.jetbrains.teamcity:server-api:${extension.version}"

                testCompile "org.jetbrains.teamcity:tests-support:${extension.version}"
            }

            def jar = project.tasks[JavaPlugin.JAR_TASK_NAME]
            def packagePlugin = project.tasks.create('packagePlugin', PackagePlugin) {
                conventionMapping.map('serverComponents') { jar.outputs.files }
            }
            packagePlugin.dependsOn jar

            project.getTasks().withType(PackagePlugin.class, new Action<PackagePlugin>() {
                public void execute(PackagePlugin task) {
                    task.getAgent().from(new Callable<FileCollection>() {
                        public FileCollection call() throws Exception {
                            return project.getConfigurations().getByName('agent')
                        }
                    })
                }
            })

            def assemble = project.tasks['assemble']
            assemble.dependsOn packagePlugin

            if (extension.descriptor instanceof File) {
                def processDescriptor = project.tasks.create('processDescriptor', ProcessPluginDescriptor) {
                    conventionMapping.map("descriptor") { extension.descriptor }
                }
                packagePlugin.dependsOn processDescriptor
            } else {
                def generateDescriptor = project.tasks.create('generateDescriptor', GenerateServerPluginDescriptor)
                packagePlugin.dependsOn generateDescriptor
            }
        }
    }

    void configureTeamCityTasks(Project project, TeamCityPluginExtension extension) {
        if ("server-plugin" == extension.type) {
            project.tasks.withType(TeamCityTask) {
                conventionMapping.map('homeDir') { extension.homeDir }
                conventionMapping.map('dataDir') { extension.dataDir }
                conventionMapping.map('javaHome') { extension.javaHome }
            }

            project.tasks.create('startServer', StartServer)
            project.tasks.create('stopServer', StopServer)
            project.tasks.create('startAgent', StartAgent)
            project.tasks.create('stopAgent', StopAgent)

            project.tasks.create('deployPlugin', DeployPlugin) {
                conventionMapping.map('file') { project.tasks.getByName('packagePlugin').archivePath }
                conventionMapping.map('target') { project.file("${extension.dataDir}/plugins") }
            }
            project.tasks.create('undeployPlugin', UndeployPlugin) {
                conventionMapping.map('file') { project.tasks.getByName('packagePlugin').archiveName }
            }

            def download = project.tasks.create("downloadTeamCity", Download) {
                conventionMapping.map('source') { extension.downloadUrl }
                conventionMapping.map('target') { project.file(extension.downloadFile) }
            }
            def unpack = project.tasks.create("unpackTeamCity", Unpack) {
                conventionMapping.map('source') { project.file(extension.downloadFile) }
                conventionMapping.map('target') { extension.homeDir }
            }
            unpack.dependsOn download

            def install = project.tasks.create("installTeamCity", InstallTeamCity)
            install.dependsOn unpack
        }
    }
}
