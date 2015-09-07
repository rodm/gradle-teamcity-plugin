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
import com.github.rodm.teamcity.tasks.GenerateServerPluginDescriptor
import com.github.rodm.teamcity.tasks.InstallTeamCity
import com.github.rodm.teamcity.tasks.StartAgent
import com.github.rodm.teamcity.tasks.StartServer
import com.github.rodm.teamcity.tasks.StopAgent
import com.github.rodm.teamcity.tasks.StopServer
import com.github.rodm.teamcity.tasks.TeamCityTask
import com.github.rodm.teamcity.tasks.UndeployPlugin
import com.github.rodm.teamcity.tasks.Unpack
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

class TeamCityServerPlugin extends TeamCityPlugin {

    public static final String SERVER_PLUGIN_DESCRIPTOR_DIR = PLUGIN_DESCRIPTOR_DIR + '/server'

    @Override
    void configureTasks(Project project, TeamCityPluginExtension extension) {
        configureServerPluginTasks(project, extension)
        configureTeamCityTasks(project, extension)
    }

    void configureServerPluginTasks(Project project, TeamCityPluginExtension extension) {
        if (project.plugins.hasPlugin(JavaPlugin)) {
            project.afterEvaluate {
                project.dependencies {
                    teamcity "org.jetbrains.teamcity:server-api:${extension.version}"

                    testCompile "org.jetbrains.teamcity:tests-support:${extension.version}"
                }
            }
        }

        def packagePlugin = project.tasks.create('serverPlugin', Zip)
        packagePlugin.description = 'Package TeamCity plugin'
        packagePlugin.group = 'TeamCity'
        packagePlugin.with {
            into('server') {
                if (project.plugins.hasPlugin(JavaPlugin)) {
                    def jar = project.tasks[JavaPlugin.JAR_TASK_NAME]
                    from(jar)
                    from(project.configurations.runtime - project.configurations.teamcity)
                }
                from(project.configurations.server)
            }
            into('agent') {
                from(project.configurations.agent)
            }
            into('') {
                from {
                    new File(project.getBuildDir(), SERVER_PLUGIN_DESCRIPTOR_DIR + "/" + PLUGIN_DESCRIPTOR_FILENAME)
                }
                rename {
                    PLUGIN_DESCRIPTOR_FILENAME
                }
            }
        }
        packagePlugin.onlyIf { extension.descriptor != null }

        def assemble = project.tasks['assemble']
        assemble.dependsOn packagePlugin

        def processDescriptor = project.tasks.create('processDescriptor', Copy)
        processDescriptor.with {
            from { extension.descriptor }
            into("$project.buildDir/$SERVER_PLUGIN_DESCRIPTOR_DIR")
        }
        processDescriptor.onlyIf { extension.descriptor != null && extension.descriptor instanceof File }
        packagePlugin.dependsOn processDescriptor

        def generateDescriptor = project.tasks.create('generateDescriptor', GenerateServerPluginDescriptor)
        generateDescriptor.onlyIf { extension.descriptor != null && extension.descriptor instanceof ServerPluginDescriptor }
        packagePlugin.dependsOn generateDescriptor
    }

    void configureTeamCityTasks(Project project, TeamCityPluginExtension extension) {
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
            conventionMapping.map('file') { project.tasks.getByName('serverPlugin').archivePath }
            conventionMapping.map('target') { project.file("${extension.dataDir}/plugins") }
        }
        project.tasks.create('undeployPlugin', UndeployPlugin) {
            conventionMapping.map('file') { project.tasks.getByName('serverPlugin').archiveName }
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
