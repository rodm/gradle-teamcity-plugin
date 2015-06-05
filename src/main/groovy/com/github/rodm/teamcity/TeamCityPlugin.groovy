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
import com.github.rodm.teamcity.tasks.GeneratePluginDescriptor
import com.github.rodm.teamcity.tasks.PackagePlugin
import com.github.rodm.teamcity.tasks.ProcessPluginDescriptor
import com.github.rodm.teamcity.tasks.StartAgent
import com.github.rodm.teamcity.tasks.StartServer
import com.github.rodm.teamcity.tasks.StopAgent
import com.github.rodm.teamcity.tasks.StopServer
import com.github.rodm.teamcity.tasks.TeamCityTask
import com.github.rodm.teamcity.tasks.UndeployPlugin
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.plugins.JavaPlugin

class TeamCityPlugin implements Plugin<Project> {

    static final String PLUGIN_DESCRIPTOR_FILENAME = 'teamcity-plugin.xml'

    static final String PLUGIN_DESCRIPTOR_DIR = 'descriptor'

    static final String TEAMCITY_EXTENSION_NAME = 'teamcity'

    static final String JETBRAINS_MAVEN_REPOSITORY = 'http://repository.jetbrains.com/all'

    void apply(Project project) {
        project.plugins.apply(JavaPlugin)
        project.extensions.create(TEAMCITY_EXTENSION_NAME, TeamCityPluginExtension)

        project.repositories.maven {
            url = JETBRAINS_MAVEN_REPOSITORY
        }

        TeamCityPluginExtension extension = project.extensions.getByName(TEAMCITY_EXTENSION_NAME)

        def jar = project.tasks[JavaPlugin.JAR_TASK_NAME]
        jar.exclude "**/" + PLUGIN_DESCRIPTOR_FILENAME

        def packagePlugin = project.tasks.create('packagePlugin', PackagePlugin)
        packagePlugin.dependsOn jar
        packagePlugin.serverComponents = jar.outputs.files

        def assemble = project.tasks['assemble']
        assemble.dependsOn packagePlugin

        project.afterEvaluate {
            project.dependencies {
                compile "org.jetbrains.teamcity:server-api:${extension.version}"

                testCompile "org.jetbrains.teamcity:tests-support:${extension.version}"
            }

            if (extension.descriptor instanceof File) {
                def processDescriptor = project.tasks.create('processDescriptor', ProcessPluginDescriptor) {
                    conventionMapping.map("descriptor") { extension.descriptor }
                }
                packagePlugin.dependsOn processDescriptor
            } else {
                def generateDescriptor = project.tasks.create('generateDescriptor', GeneratePluginDescriptor)
                packagePlugin.dependsOn generateDescriptor
            }
        }

        configureTeamCityTasks(project, extension)
    }

    private void configureTeamCityTasks(Project project, TeamCityPluginExtension extension) {
        project.tasks.withType(TeamCityTask) {
            conventionMapping.map('homeDir') { extension.homeDir }
            conventionMapping.map('dataDir') { extension.dataDir }
            conventionMapping.map('javaHome') { extension.javaHome }
        }

        project.tasks.create('startServer', StartServer)
        project.tasks.create('stopServer', StopServer)
        project.tasks.create('startAgent', StartAgent)
        project.tasks.create('stopAgent', StopAgent)

        def deployPlugin = project.tasks.create('deployPlugin', DeployPlugin)
        deployPlugin.file = project.tasks['packagePlugin'].archivePath

        def undeployPlugin = project.tasks.create('undeployPlugin', UndeployPlugin)
        undeployPlugin.file = project.tasks['packagePlugin'].archiveName
    }
}
