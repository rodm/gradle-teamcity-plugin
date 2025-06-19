/*
 * Copyright 2016 the original author or authors.
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

import com.github.rodm.teamcity.internal.DisablePluginAction
import com.github.rodm.teamcity.docker.DockerSupport
import com.github.rodm.teamcity.internal.EnablePluginAction
import com.github.rodm.teamcity.internal.PluginAction
import com.github.rodm.teamcity.tasks.Deploy
import com.github.rodm.teamcity.tasks.DownloadTeamCity
import com.github.rodm.teamcity.tasks.InstallTeamCity
import com.github.rodm.teamcity.tasks.StartAgent
import com.github.rodm.teamcity.tasks.StartDockerAgent
import com.github.rodm.teamcity.tasks.StartDockerServer
import com.github.rodm.teamcity.tasks.StartServer
import com.github.rodm.teamcity.tasks.StopAgent
import com.github.rodm.teamcity.tasks.StopDockerAgent
import com.github.rodm.teamcity.tasks.StopDockerServer
import com.github.rodm.teamcity.tasks.StopServer
import com.github.rodm.teamcity.internal.TeamCityTask
import com.github.rodm.teamcity.tasks.Undeploy
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.initialization.GradlePropertiesController
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.api.io.TempDir
import org.slf4j.helpers.MessageFormatter

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import static com.github.rodm.teamcity.GradleMatchers.hasAction
import static com.github.rodm.teamcity.GradleMatchers.hasDefaultDependency
import static com.github.rodm.teamcity.GradleMatchers.hasTask
import static com.github.rodm.teamcity.TestSupport.createDirectory
import static com.github.rodm.teamcity.TestSupport.createFile
import static com.github.rodm.teamcity.TestSupport.normalize
import static com.github.rodm.teamcity.TestSupport.normalizePath
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.anyOf
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.endsWith
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.isA
import static org.hamcrest.Matchers.not
import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.Matchers.startsWith
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assertions.fail
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

@SuppressWarnings('ConfigurationAvoidance')
class EnvironmentsTest {

    @TempDir
    public Path projectDir

    private final ResettableOutputEventListener outputEventListener = new ResettableOutputEventListener()

    @RegisterExtension
    public final ConfigureLogging logging = new ConfigureLogging(outputEventListener)

    private String defaultOptions = [
        '-Dteamcity.development.mode=true',
        '-Dteamcity.development.shadowCopyClasses=true',
        '-Dteamcity.superUser.token.saveToFile=true',
        '-Dteamcity.kotlinConfigsDsl.generateDslDocs=false'
    ].join(' ')

    private String ENVIRONMENTS_WARNING = 'Configuring environments with the teamcity-server plugin is deprecated'

    private Project project

    private Task task(String name) {
        project.tasks.getByName(name)
    }

    @BeforeEach
    void setup() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
        // workaround for https://github.com/gradle/gradle/issues/13122
        (project as ProjectInternal).services.get(GradlePropertiesController).loadGradlePropertiesFrom(projectDir.toFile())
    }

    @Nested
    class WithEnvironmentsPlugin {

        @BeforeEach
        void setup() {
            project.apply plugin: 'io.github.rodm.teamcity-environments'
        }

        @Test
        void defaultProperties() {
            TeamCityEnvironments environments = project.extensions.getByType(TeamCityPluginExtension).environments
            assertThat(environments.downloadsDir, equalTo('downloads'))
            assertThat(environments.baseDownloadUrl, equalTo('https://download.jetbrains.com/teamcity'))
            assertThat(normalize(environments.baseHomeDir), endsWith('/servers'))
            assertThat(normalize(environments.baseDataDir), endsWith('/data'))
        }

        @Test
        void alternativeDownloadsDir() {
            project.teamcity {
                environments {
                    downloadsDir = '/tmp/downloads'
                }
            }

            TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
            assertThat(extension.environments.downloadsDir, equalTo('/tmp/downloads'))
        }

        @Test
        void alternativeDownloadBaseUrl() {
            project.teamcity {
                environments {
                    baseDownloadUrl = 'http://local-repository'
                }
            }

            TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
            assertThat(extension.environments.baseDownloadUrl, equalTo('http://local-repository'))
        }

        @Test
        void alternativeBaseDataDir() {
            project.teamcity {
                environments {
                    baseDataDir = '/tmp/data'
                }
            }

            TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
            assertThat(normalize(extension.environments.baseDataDir), endsWith('/tmp/data'))
        }

        @Test
        void alternativeBaseHomeDir() {
            project.teamcity {
                environments {
                    baseHomeDir = '/tmp/servers'
                }
            }

            TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
            assertThat(normalize(extension.environments.baseHomeDir), endsWith('/tmp/servers'))
        }

        @Test
        void defaultOptions() {
            project.teamcity {
                environments {
                    test {
                    }
                }
            }
            project.evaluate()

            def startTestServer = project.tasks.getByName('startTestServer') as StartServer
            assertThat(startTestServer.serverOptions.get(), equalTo(defaultOptions))
            def startTestAgent = project.tasks.getByName('startTestAgent') as StartAgent
            assertThat(startTestAgent.agentOptions.get(), equalTo(''))
        }

        @Test
        void 'reject invalid version'() {
            def expected = assertThrows(IllegalArgumentException, {
                project.teamcity {
                    environments {
                        test {
                            version = '123'
                        }
                    }
                }
            }, 'Invalid version not rejected')
            assertThat(expected.message, startsWith("'123' is not a valid TeamCity version"))
        }

        @Test
        void replaceDefaultServerOptions() {
            project.teamcity {
                environments {
                    test {
                        serverOptions = '-DnewOption=test'
                    }
                }
            }
            project.evaluate()

            def startTestServer = project.tasks.getByName('startTestServer') as StartServer
            assertThat(startTestServer.serverOptions.get(), equalTo('-DnewOption=test'))
        }

        @Test
        void replaceDefaultServerOptionsWithMultipleValues() {
            project.teamcity {
                environments {
                    test {
                        serverOptions = ['-Doption1=value1', '-Doption2=value2']
                    }
                }
            }
            project.evaluate()

            def startTestServer = project.tasks.getByName('startTestServer') as StartServer
            assertThat(startTestServer.serverOptions.get(), equalTo('-Doption1=value1 -Doption2=value2'))
        }

        @Test
        void addToDefaultServerOptions() {
            project.teamcity {
                environments {
                    test {
                        serverOptions '-DadditionalOption=test'
                    }
                }
            }
            project.evaluate()

            def expectedOptions = defaultOptions + ' -DadditionalOption=test'
            def startTestServer = project.tasks.getByName('startTestServer') as StartServer
            assertThat(startTestServer.serverOptions.get(), equalTo(expectedOptions))
        }

        @Test
        void addMultipleValuesToDefaultServerOptions() {
            project.teamcity {
                environments {
                    test {
                        serverOptions '-DadditionalOption1=value1', '-DadditionalOption2=value2'
                    }
                }
            }
            project.evaluate()

            def expectedOptions = defaultOptions + ' -DadditionalOption1=value1 -DadditionalOption2=value2'
            def startTestServer = project.tasks.getByName('startTestServer') as StartServer
            assertThat(startTestServer.serverOptions.get(), equalTo(expectedOptions))
        }

        @Test
        void replaceDefaultAgentOptions() {
            project.teamcity {
                environments {
                    test {
                        agentOptions = '-DnewOption1=value1'
                        agentOptions = '-DnewOption2=value2'
                    }
                }
            }
            project.evaluate()

            def startTestAgent = project.tasks.getByName('startTestAgent') as StartAgent
            assertThat(startTestAgent.agentOptions.get(), equalTo('-DnewOption2=value2'))
        }

        @Test
        void replaceDefaultAdentOptionsWithMultipleValues() {
            project.teamcity {
                environments {
                    test {
                        agentOptions = ['-Doption1=value1', '-Doption2=value2']
                    }
                }
            }
            project.evaluate()

            def startTestAgent = project.tasks.getByName('startTestAgent') as StartAgent
            assertThat(startTestAgent.agentOptions.get(), equalTo('-Doption1=value1 -Doption2=value2'))
        }

        @Test
        void addToDefaultAgentOptions() {
            project.teamcity {
                environments {
                    test {
                        agentOptions '-DadditionalOption1=value1'
                        agentOptions '-DadditionalOption2=value2'
                    }
                }
            }
            project.evaluate()

            String expectedOptions = '-DadditionalOption1=value1 -DadditionalOption2=value2'
            def startTestAgent = project.tasks.getByName('startTestAgent') as StartAgent
            assertThat(startTestAgent.agentOptions.get(), equalTo(expectedOptions))
        }

        @Test
        void addMultipleValuesToDefaultAgentOptions() {
            project.teamcity {
                environments {
                    test {
                        agentOptions '-DadditionalOption1=value1', '-DadditionalOption2=value2'
                    }
                }
            }
            project.evaluate()

            String expectedOptions = '-DadditionalOption1=value1 -DadditionalOption2=value2'
            def startTestAgent = project.tasks.getByName('startTestAgent') as StartAgent
            assertThat(startTestAgent.agentOptions.get(), equalTo(expectedOptions))
        }

        @Test
        void 'configuring environment with teamcity-environments plugin does not output warning'() {
            outputEventListener.reset()
            project.teamcity {
                environments {
                    test {
                    }
                }
            }

            assertThat(outputEventListener.toString(), not(containsString(ENVIRONMENTS_WARNING)))
        }

        @Test
        void 'configuring environment with teamcity-environments and teamcity-server plugins does not output warning'() {
            outputEventListener.reset()
            project.apply plugin: 'io.github.rodm.teamcity-server'
            project.apply plugin: 'io.github.rodm.teamcity-environments'
            project.teamcity {
                environments {
                    test {
                    }
                }
            }

            assertThat(outputEventListener.toString(), not(containsString(ENVIRONMENTS_WARNING)))
        }

        @Test
        void 'environments plugin configures environments with default properties'() {
            project.teamcity {
                environments {
                    test1 {
                        version = '9.1.7'
                    }
                    test2 {
                        version = '10.0.4'
                    }
                }
            }
            project.evaluate()

            def downloadTest1 = project.tasks.getByName('downloadTest1') as DownloadTeamCity
            assertThat(downloadTest1.src.toString(), equalTo('https://download.jetbrains.com/teamcity/TeamCity-9.1.7.tar.gz'))
            def startTest1Server = project.tasks.getByName('startTest1Server') as StartServer
            assertThat(normalize(startTest1Server.homeDir.get()), endsWith('/servers/TeamCity-9.1.7'))
            assertThat(normalize(startTest1Server.dataDir.get()), endsWith('/data/9.1'))
            assertThat(startTest1Server.javaHome.get(), equalTo(System.getProperty('java.home')))

            def downloadTest2 = project.tasks.getByName('downloadTest2') as DownloadTeamCity
            assertThat(downloadTest2.src.toString(), equalTo('https://download.jetbrains.com/teamcity/TeamCity-10.0.4.tar.gz'))
            def startTest2Server = project.tasks.getByName('startTest2Server') as StartServer
            assertThat(normalize(startTest2Server.homeDir.get()), endsWith('/servers/TeamCity-10.0.4'))
            assertThat(normalize(startTest2Server.dataDir.get()), endsWith('/data/10.0'))
            assertThat(startTest2Server.javaHome.get(), equalTo(System.getProperty('java.home')))
        }

        @Test
        void 'environments plugin configures environments using shared properties'() {
            project.teamcity {
                environments {
                    baseDownloadUrl = 'http://local-repository'
                    baseHomeDir = '/tmp/servers'
                    baseDataDir = '/tmp/data'
                    test1 {
                        version = '9.1.7'
                    }
                    test2 {
                        version = '10.0.4'
                    }
                }
            }
            project.evaluate()

            def downloadTest1 = project.tasks.getByName('downloadTest1') as DownloadTeamCity
            assertThat(downloadTest1.src.toString(), equalTo('http://local-repository/TeamCity-9.1.7.tar.gz'))

            def startTest1Server = project.tasks.getByName('startTest1Server') as StartServer
            assertThat(normalize(startTest1Server.homeDir.get()), endsWith('/tmp/servers/TeamCity-9.1.7'))
            assertThat(normalize(startTest1Server.dataDir.get()), endsWith('/tmp/data/9.1'))

            def downloadTest2 = project.tasks.getByName('downloadTest2') as DownloadTeamCity
            assertThat(downloadTest2.src.toString(), equalTo('http://local-repository/TeamCity-10.0.4.tar.gz'))

            def startTest2Server = project.tasks.getByName('startTest2Server') as StartServer
            assertThat(normalize(startTest2Server.homeDir.get()), endsWith('/tmp/servers/TeamCity-10.0.4'))
            assertThat(normalize(startTest2Server.dataDir.get()), endsWith('/tmp/data/10.0'))
        }

        @Test
        void 'environments plugin configures environments composed with shared properties'() {
            project.teamcity {
                environments {
                    baseHomeDir = '/tmp/servers'
                    baseDataDir = '/tmp/data'
                    test1 {
                        version = '2021.1'
                        homeDir = "${baseHomeDir}/Test1"
                        dataDir = "${baseDataDir}/Test1"
                    }
                }
            }
            project.evaluate()

            def startTest2Server = project.tasks.getByName('startTest1Server') as StartServer
            assertThat(normalize(startTest2Server.homeDir.get()), endsWith('/tmp/servers/Test1'))
            assertThat(normalize(startTest2Server.dataDir.get()), endsWith('/tmp/data/Test1'))
        }

        @Test
        void 'environments plugin configures environment tasks using environment properties'() {
            project.teamcity {
                environments {
                    test {
                        version = '9.1.7'
                        downloadUrl = 'http://local-repository/TeamCity-9.1.7.tar.gz'
                        homeDir = project.file('/tmp/servers/TeamCity-9.1.7')
                        dataDir = project.file('/tmp/data/teamcity9.1')
                        javaHome = project.file('/tmp/java')
                    }
                }
            }
            project.evaluate()

            def downloadTest = project.tasks.getByName('downloadTest') as DownloadTeamCity
            assertThat(downloadTest.src.toString(), equalTo('http://local-repository/TeamCity-9.1.7.tar.gz'))

            def startTestServer = project.tasks.getByName('startTestServer') as StartServer
            assertThat(normalize(startTestServer.homeDir.get()), endsWith('/tmp/servers/TeamCity-9.1.7'))
            assertThat(normalize(startTestServer.dataDir.get()), endsWith('/tmp/data/teamcity9.1'))
            assertThat(normalize(startTestServer.javaHome.get()), endsWith('/tmp/java'))
        }

        @Test
        void 'environments plugin adds tasks for an environment'() {
            project.teamcity {
                environments {
                    teamcity9 {
                        version = '9.1.7'
                    }
                }
            }
            project.evaluate()

            assertThat(project, hasTask('downloadTeamcity9'))
            assertThat(project, hasTask('installTeamcity9'))

            assertThat(project, hasTask('deployToTeamcity9'))
            assertThat(project, hasTask('undeployFromTeamcity9'))

            assertThat(project, hasTask('startTeamcity9Server'))
            assertThat(project, hasTask('stopTeamcity9Server'))
            assertThat(project, hasTask('startTeamcity9Agent'))
            assertThat(project, hasTask('stopTeamcity9Agent'))
            assertThat(project, hasTask('startTeamcity9'))
            assertThat(project, hasTask('stopTeamcity9'))
        }

        @Test
        void 'configures deploy task with actions to disable and enable plugin for version 2018_2 and later'() {
            project.teamcity {
                environments {
                    test {
                        version = '2018.2'
                    }
                }
            }
            project.evaluate()

            Copy deployPlugin = project.tasks.getByName('deployToTest') as Copy
            assertThat(deployPlugin, hasAction(DisablePluginAction))
            assertThat(deployPlugin, hasAction(EnablePluginAction))
        }

        @Test
        void 'configures deploy task without actions to disable and enable plugin for version 2018_1 and earlier'() {
            project.teamcity {
                environments {
                    test {
                        version = '2018.1'
                    }
                }
            }
            project.evaluate()

            Copy deployPlugin = project.tasks.getByName('deployToTest') as Copy
            assertThat(deployPlugin, not(hasAction(DisablePluginAction)))
            assertThat(deployPlugin, not(hasAction(EnablePluginAction)))
        }

        @Test
        void 'configures undeploy task with action to disable the plugin for version 2018_2 and later'() {
            project.teamcity {
                environments {
                    test {
                        version = '2018.2'
                    }
                }
            }
            project.evaluate()

            Delete undeployPlugin = project.tasks.getByName('undeployFromTest') as Delete
            assertThat(undeployPlugin, hasAction(DisablePluginAction))
        }

        @Test
        void 'configures undeploy task without action to disable the plugin for version 2018_1 and earlier'() {
            project.teamcity {
                environments {
                    test {
                        version = '2018.1'
                    }
                }
            }
            project.evaluate()

            Delete undeployPlugin = project.tasks.getByName('undeployFromTest') as Delete
            assertThat(undeployPlugin, not(hasAction(DisablePluginAction)))
        }

        @Test
        void 'environments plugin adds tasks for each separate environment'() {
            project.teamcity {
                environments {
                    teamcity9 {
                        version = '9.1.7'
                    }
                    teamcity10 {
                        version = '10.0.3'
                    }
                }
            }
            project.evaluate()

            assertThat(project, hasTask('startTeamcity9Server'))
            assertThat(project, hasTask('startTeamcity10Server'))
        }

        @Test
        void 'environments plugin configures deploy and undeploy tasks with project plugin'() {
            project.apply plugin: 'io.github.rodm.teamcity-server'
            project.teamcity {
                environments {
                    test {
                        version = '10.0.3'
                    }
                }
            }
            project.evaluate()

            def deployPlugin = project.tasks.getByName('deployToTest') as Deploy
            Set<File> deployFiles = deployPlugin.plugins.files
            assertThat(deployFiles, hasSize(1))
            assertThat(deployFiles, hasItem(project.file('build/distributions/test.zip')))
            assertThat(normalizePath(deployPlugin.pluginsDir), endsWith('data/10.0/plugins'))

            def undeployPlugin = project.tasks.getByName('undeployFromTest') as Undeploy
            Set<File> undeployFiles = undeployPlugin.plugins.files
            assertThat(undeployFiles, hasSize(1))
            assertThat(undeployFiles, hasItem(project.file('build/distributions/test.zip')))
            assertThat(normalizePath(undeployPlugin.pluginsDir), endsWith('data/10.0/plugins'))
        }

        @Test
        void 'environments plugin configures deploy and undeploy tasks with multiple plugins'() {
            project.teamcity {
                environments {
                    test {
                        version = '10.0.3'
                        plugins 'plugin1.zip'
                        plugins 'plugin2.zip'
                    }
                }
            }
            project.evaluate()

            def deployPlugin = project.tasks.getByName('deployToTest') as Deploy
            Set<File> deployFiles = deployPlugin.plugins.files
            assertThat(deployFiles, hasSize(2))
            assertThat(deployFiles, hasItem(project.file('plugin1.zip')))
            assertThat(deployFiles, hasItem(project.file('plugin2.zip')))

            def undeployPlugin = project.tasks.getByName('undeployFromTest') as Undeploy
            Set<File> undeployFiles = undeployPlugin.plugins.files
            assertThat(undeployFiles, hasSize(2))
            assertThat(undeployFiles, hasItem(project.file('plugin1.zip')))
            assertThat(undeployFiles, hasItem(project.file('plugin2.zip')))
        }

        @Test
        void 'configure plugin with assignment replaces'() {
            project.teamcity {
                environments {
                    test {
                        plugins = 'plugin1.zip'
                        plugins = 'plugin2.zip'
                    }
                }
            }
            project.evaluate()

            def deployPlugin = project.tasks.getByName('deployToTest') as Deploy
            Set<File> deployFiles = deployPlugin.plugins.files
            assertThat(deployFiles, hasSize(1))
            assertThat(deployFiles, hasItem(project.file('plugin2.zip')))
        }

        @Test
        void 'configure multiple plugins for an environment with a list'() {
            project.teamcity {
                environments {
                    test {
                        plugins = ['plugin1.zip', 'plugin2.zip']
                    }
                }
            }
            project.evaluate()

            def deployPlugin = project.tasks.getByName('deployToTest') as Deploy
            Set<File> deployFiles = deployPlugin.plugins.files
            assertThat(deployFiles, hasSize(2))
            assertThat(deployFiles, hasItem(project.file('plugin1.zip')))
            assertThat(deployFiles, hasItem(project.file('plugin2.zip')))
        }

        @Test
        void 'applying teamcity-server and teamcity-environments does not duplicate tasks'() {
            outputEventListener.reset()
            project.apply plugin: 'io.github.rodm.teamcity-server'
            project.teamcity {
                environments {
                    test {
                    }
                }
            }
            project.evaluate()

            assertThat(project, hasTask('downloadTest'))
        }

        private Closure TEAMCITY10_ENVIRONMENT = {
            environments {
                baseDownloadUrl = 'http://local-repository'
                baseHomeDir = '/tmp/servers'
                baseDataDir = '/tmp/data'
                teamcity10 {
                    version = '10.0.4'
                    javaHome = project.file('/opt/jdk1.8.0')
                    agentOptions = '-DagentOption=agentValue'
                }
            }
        }

        @Test
        void 'configures startServer task'() {
            project.teamcity TEAMCITY10_ENVIRONMENT

            project.evaluate()

            StartServer startServer = project.tasks.getByName('startTeamcity10Server') as StartServer
            assertThat(normalize(startServer.homeDir.get()), endsWith('servers/TeamCity-10.0.4'))
            assertThat(normalize(startServer.dataDir.get()), endsWith('data/10.0'))
            assertThat(normalize(startServer.javaHome.get()), endsWith('/opt/jdk1.8.0'))
            assertThat(startServer.serverOptions.get(), endsWith(defaultOptions))
        }

        @Test
        void 'configures stopServer task'() {
            project.teamcity TEAMCITY10_ENVIRONMENT

            project.evaluate()

            StopServer stopServer = project.tasks.getByName('stopTeamcity10Server') as StopServer
            assertThat(normalize(stopServer.homeDir.get()), endsWith('servers/TeamCity-10.0.4'))
            assertThat(normalize(stopServer.javaHome.get()), endsWith('/opt/jdk1.8.0'))
        }

        @Test
        void 'configures startAgent task'() {
            project.teamcity TEAMCITY10_ENVIRONMENT

            project.evaluate()

            StartAgent startAgent = project.tasks.getByName('startTeamcity10Agent') as StartAgent
            assertThat(normalize(startAgent.homeDir.get()), endsWith('servers/TeamCity-10.0.4'))
            assertThat(normalize(startAgent.javaHome.get()), endsWith('/opt/jdk1.8.0'))
            assertThat(startAgent.agentOptions.get(), endsWith('-DagentOption=agentValue'))
        }

        @Test
        void 'configures stopAgent task'() {
            project.teamcity TEAMCITY10_ENVIRONMENT

            project.evaluate()

            StopAgent stopAgent = project.tasks.getByName('stopTeamcity10Agent') as StopAgent
            assertThat(normalize(stopAgent.homeDir.get()), endsWith('servers/TeamCity-10.0.4'))
            assertThat(normalize(stopAgent.javaHome.get()), endsWith('/opt/jdk1.8.0'))
        }

        @Test
        void 'teamcity task validates home directory'() {
            project.teamcity {
                environments {
                    'teamcity2020.1' {
                        version = '2020.1'
                    }
                }
            }
            project.evaluate()

            StartServer startServer = project.tasks.getByName('startTeamcity2020.1Server') as StartServer
            def e = assertThrows(InvalidUserDataException) { startServer.validate() }
            assertThat(normalize(e.message), containsString('/servers/TeamCity-2020.1'))
            assertThat(e.message, containsString("specified for property 'homeDir' does not exist."))
        }

        @Test
        void 'teamcity task validates home directory not a file'() {
            project.teamcity {
                environments {
                    'teamcity2020.1' {
                        version = '2020.1'
                    }
                }
            }
            project.evaluate()
            def serverDir = createDirectory(projectDir.resolve('servers'))
            createFile(serverDir.toPath().resolve('TeamCity-2020.1'))

            StartServer startServer = project.tasks.getByName('startTeamcity2020.1Server') as StartServer
            def e = assertThrows(InvalidUserDataException) { startServer.validate() }
            assertThat(normalize(e.message), containsString('/servers/TeamCity-2020.1'))
            assertThat(e.message, containsString("specified for property 'homeDir' is not a directory."))
        }

        @Test
        void 'teamcity task validates home directory for TeamCity 2023_11'() {
            createFakeTeamCityInstall(projectDir, 'servers', '2023.11.2')
            project.teamcity {
                environments {
                    'teamcity2023.11' {
                        version = '2023.11.2'
                    }
                }
            }
            project.evaluate()

            StartServer startServer = project.tasks.getByName('startTeamcity2023.11Server') as StartServer
            assertDoesNotThrow((Executable) { startServer.validate() })
        }

        @Test @SuppressWarnings('GroovyAccessibility')
        void 'teamcity task outputs no warning when environment version matches installed version'() {
            createFakeTeamCityInstall(projectDir, 'servers', '2020.2.3')
            project.teamcity {
                environments {
                    'teamcity2020.2' {
                        version = '2020.2.3'
                    }
                }
            }
            project.evaluate()

            StartServer startServer = project.tasks.getByName('startTeamcity2020.2Server') as StartServer
            startServer.validate()

            String expectedMessage = String.format(TeamCityTask.VERSION_MISMATCH_WARNING[4..-4], '2020.2.3', '2020.2.3')
            assertThat(outputEventListener.toString(), not(containsString(expectedMessage)))
        }

        @Test @SuppressWarnings('GroovyAccessibility')
        void 'teamcity task outputs a warning when environment version does not match at bugfix level'() {
            File fakeHomeDir = createFakeTeamCityInstall(projectDir, 'servers', '2020.2.3')
            project.teamcity {
                environments {
                    'teamcity2020.2' {
                        version = '2020.2.5'
                        homeDir = fakeHomeDir.absolutePath
                    }
                }
            }
            project.evaluate()

            StartServer startServer = project.tasks.getByName('startTeamcity2020.2Server') as StartServer
            startServer.validate()

            String expectedMessage = MessageFormatter.format(TeamCityTask.VERSION_MISMATCH_WARNING[4..-4], '2020.2.5', '2020.2.3').message
            assertThat(outputEventListener.toString(), containsString(expectedMessage))
        }

        @Test @SuppressWarnings('GroovyAccessibility')
        void 'teamcity task outputs a warning when environment version does not match EAP level'() {
            File fakeHomeDir = createFakeTeamCityInstall(projectDir, 'servers', '2021.2 EAP1')
            project.teamcity {
                environments {
                    'teamcity2021.2' {
                        version = '2021.2'
                        homeDir = fakeHomeDir.absolutePath
                    }
                }
            }
            project.evaluate()

            StartServer startServer = project.tasks.getByName('startTeamcity2021.2Server') as StartServer
            startServer.validate()

            String expectedMessage = MessageFormatter.format(TeamCityTask.VERSION_MISMATCH_WARNING[4..-4], '2021.2', '2021.2 EAP1').message
            assertThat(outputEventListener.toString(), containsString(expectedMessage))
        }

        @Test @SuppressWarnings('GroovyAccessibility')
        void 'teamcity task outputs a warning when environment version does not match RC level'() {
            File fakeHomeDir = createFakeTeamCityInstall(projectDir, 'servers', '2021.2 RC')
            project.teamcity {
                environments {
                    'teamcity2021.2' {
                        version = '2021.2'
                        homeDir = fakeHomeDir.absolutePath
                    }
                }
            }
            project.evaluate()

            StartServer startServer = project.tasks.getByName('startTeamcity2021.2Server') as StartServer
            startServer.validate()

            String expectedMessage = MessageFormatter.format(TeamCityTask.VERSION_MISMATCH_WARNING[4..-4], '2021.2', '2021.2 RC').message
            assertThat(outputEventListener.toString(), containsString(expectedMessage))
        }

        @Test @SuppressWarnings('GroovyAccessibility')
        void 'teamcity task fail when environment version is not data compatible with installed version'() {
            File fakeHomeDir = createFakeTeamCityInstall(projectDir, 'servers', '2020.2.3')
            project.teamcity {
                environments {
                    'teamcity2020.2' {
                        version = '2020.1.2'
                        homeDir = fakeHomeDir.absolutePath
                    }
                }
            }
            project.evaluate()

            StartServer startServer = project.tasks.getByName('startTeamcity2020.2Server') as StartServer
            def e = assertThrows(InvalidUserDataException) { startServer.validate() }

            String expectedMessage = String.format(TeamCityTask.VERSION_INCOMPATIBLE[0..-4], '2020.1.2', '2020.2.3')
            assertThat(e.message, containsString(expectedMessage))
        }

        @Test
        void 'teamcity task validates Java home directory'() {
            project.teamcity {
                environments {
                    'teamcity2020.1' {
                        version = '2020.1'
                        javaHome = project.file('/tmp/opt/jdk1.8.0')
                    }
                }
            }

            project.evaluate()
            createFakeTeamCityInstall(projectDir, 'servers', '2020.1')

            StartServer startServer = project.tasks.getByName('startTeamcity2020.1Server') as StartServer
            def e = assertThrows(InvalidUserDataException) { startServer.validate() }
            assertThat(normalize(e.message), containsString('/tmp/opt/jdk1.8.0'))
            assertThat(e.message, containsString("specified for property 'javaHome' does not exist."))
        }

        @Test
        void 'configures install task'() {
            project.teamcity TEAMCITY10_ENVIRONMENT

            project.evaluate()

            InstallTeamCity install = project.tasks.getByName('installTeamcity10') as InstallTeamCity
            assertThat(normalizePath(install.getSource()), endsWith('downloads/TeamCity-10.0.4.tar.gz'))
            assertThat(normalizePath(install.getTarget()), endsWith('servers/TeamCity-10.0.4'))
        }

        @Test
        void 'configures download task'() {
            project.teamcity TEAMCITY10_ENVIRONMENT

            project.evaluate()

            DownloadTeamCity download = project.tasks.getByName('downloadTeamcity10') as DownloadTeamCity
            assertThat(download.getSrc().toString(), equalTo('http://local-repository/TeamCity-10.0.4.tar.gz'))
            assertThat(normalizePath(download.getDest()), endsWith('downloads/TeamCity-10.0.4.tar.gz'))
        }

        @Test
        void 'environments plugin adds named extension'() {
            TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
            def environments = extension.extensions.findByName('environments')
            assertThat(environments, is(notNullValue()))
            assertThat(environments, isA(TeamCityEnvironments))
        }

        @Test
        void 'environment tasks are for a local installation'() {
            project.teamcity {
                environments {
                    test {
                        version = '2021.2.3'
                    }
                }
            }
            project.evaluate()

            assertThat(task('downloadTest'), isA(DownloadTeamCity))
            assertThat(task('installTest'), isA(InstallTeamCity))
            assertThat(task('startTestServer'), isA(StartServer))
            assertThat(task('stopTestServer'), isA(StopServer))
            assertThat(task('startTestAgent'), isA(StartAgent))
            assertThat(task('stopTestAgent'), isA(StopAgent))
        }

        @Test
        void 'default environment type is local'() {
            project.teamcity {
                environments {
                    test1 {
                        version = '2021.2.1'
                        homeDir = '/opt/teamcity-server'
                    }
                    create('test2') {
                        version = '2021.2.2'
                        homeDir = '/opt/teamcity-server'
                    }
                    register('test3') {
                        version = '2021.2.3'
                        homeDir = '/opt/teamcity-server'
                    }
                }
            }
            project.evaluate()

            TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
            def environments = extension.extensions.findByName('environments') as TeamCityEnvironments

            assertThat(environments.getByName('test1'), isA(LocalTeamCityEnvironment))
            assertThat(environments.getByName('test2'), isA(LocalTeamCityEnvironment))
            assertThat(environments.getByName('test3'), isA(LocalTeamCityEnvironment))
        }

        @Test
        void 'configure local environment type'() {
            project.teamcity {
                environments {
                    test1(LocalTeamCityEnvironment) {
                        version = '2021.2.1'
                        homeDir = '/opt/teamcity-server'
                    }
                    create('test2', LocalTeamCityEnvironment) {
                        version = '2021.2.2'
                        homeDir = '/opt/teamcity-server'
                    }
                    register('test3', LocalTeamCityEnvironment) {
                        version = '2021.2.3'
                        homeDir = '/opt/teamcity-server'
                    }
                }
            }
            project.evaluate()

            TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
            def environments = extension.extensions.findByName('environments') as TeamCityEnvironments

            assertThat(environments.getByName('test1'), isA(LocalTeamCityEnvironment))
            assertThat(environments.getByName('test2'), isA(LocalTeamCityEnvironment))
            assertThat(environments.getByName('test3'), isA(LocalTeamCityEnvironment))
        }
    }

    @Nested
    class WithoutEnvironmentsPlugin {

        @Test
        void 'applying teamcity-environments before teamcity-server does not duplicate environment tasks'() {
            outputEventListener.reset()
            project.apply plugin: 'io.github.rodm.teamcity-environments'
            project.apply plugin: 'io.github.rodm.teamcity-server'
            project.teamcity {
                environments {
                    test {
                    }
                }
            }
            project.evaluate()

            assertThat(project, hasTask('downloadTest'))
        }

        @Disabled
        @Test
        void 'gradle properties should override default shared properties'() {
            projectDir.resolve('gradle.properties').toFile() << """
            teamcity.environments.downloadsDir = /alt/downloads
            teamcity.environments.baseDownloadUrl = http://alt-repository
            teamcity.environments.baseHomeDir = /alt/servers
            teamcity.environments.baseDataDir = /alt/data
            """
            project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
            // workaround for https://github.com/gradle/gradle/issues/13122
            (project as ProjectInternal).services.get(GradlePropertiesController).loadGradlePropertiesFrom(projectDir.toFile())

            project.apply plugin: 'io.github.rodm.teamcity-environments'
            project.teamcity {
                environments {
                    test {
                        version = '2021.2.3'
                    }
                }
            }
            project.evaluate()

            def downloadTest = project.tasks.getByName('downloadTest') as DownloadTeamCity
            assertThat(downloadTest.src.toString(), startsWith('http://alt-repository/'))
            assertThat(normalizePath(downloadTest.getDest()), endsWith('/alt/downloads/TeamCity-2021.2.3.tar.gz'))

            def startTestServer = project.tasks.getByName('startTestServer') as StartServer
            assertThat(startTestServer.homeDir.get(), endsWith('/alt/servers/TeamCity-2021.2.3'))
            assertThat(startTestServer.dataDir.get(), endsWith('/alt/data/2021.2'))
        }

        @Disabled
        @Test
        void 'gradle properties should override shared properties'() {
            projectDir.resolve('gradle.properties').toFile() << """
            teamcity.environments.downloadsDir = /alt/downloads
            teamcity.environments.baseDownloadUrl = http://alt-repository
            teamcity.environments.baseHomeDir = /alt/servers
            teamcity.environments.baseDataDir = /alt/data
            """
            project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
            // workaround for https://github.com/gradle/gradle/issues/13122
            (project as ProjectInternal).services.get(GradlePropertiesController).loadGradlePropertiesFrom(projectDir.toFile())

            project.apply plugin: 'io.github.rodm.teamcity-environments'
            project.teamcity {
                environments {
                    downloadsDir = '/tmp/downloads'
                    baseDownloadUrl = 'http://local-repository'
                    baseHomeDir = '/tmp/servers'
                    baseDataDir = '/tmp/data'

                    test {
                        version = '2021.2.3'
                    }
                }
            }
            project.evaluate()

            def downloadTest = project.tasks.getByName('downloadTest') as DownloadTeamCity
            assertThat(downloadTest.src.toString(), startsWith('http://alt-repository/'))
            assertThat(normalizePath(downloadTest.getDest()), endsWith('/alt/downloads/TeamCity-2021.2.3.tar.gz'))

            def startTestServer = project.tasks.getByName('startTestServer') as StartServer
            assertThat(normalize(startTestServer.homeDir.get()), endsWith('/alt/servers/TeamCity-2021.2.3'))
            assertThat(startTestServer.dataDir.get(), endsWith('/alt/data/2021.2'))
        }

        @Disabled
        @Test
        void 'environments plugin configures environment tasks using overrides from gradle properties'() {
            projectDir.resolve('gradle.properties').toFile() << """
            teamcity.environments.test.downloadUrl = https://alt-repository/TeamCity-9.1.7.tar.gz
            teamcity.environments.test.javaHome = /alt/java
            teamcity.environments.test.homeDir = /alt/servers/TeamCity-9.1.7
            teamcity.environments.test.dataDir = /alt/data/9.1
            teamcity.environments.test.serverOptions = -DserverOption1=value1 -DserverOption2=value2
            teamcity.environments.test.agentOptions = -DagentOption1=value1 -DagentOption2=value2
            """
            project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
            // workaround for https://github.com/gradle/gradle/issues/13122
            (project as ProjectInternal).services.get(GradlePropertiesController).loadGradlePropertiesFrom(projectDir.toFile())

            project.apply plugin: 'io.github.rodm.teamcity-environments'
            project.teamcity {
                environments {
                    test {
                        version = '9.1.7'
                        downloadUrl = 'http://local-repository/TeamCity-9.1.7.tar.gz'
                        javaHome = project.file('/tmp/java')
                        homeDir = project.file('/tmp/servers/TeamCity-9.1.7')
                        dataDir = project.file('/tmp/data/teamcity9.1')
                    }
                }
            }
            project.evaluate()

            def downloadTest = project.tasks.getByName('downloadTest') as DownloadTeamCity
            assertThat(downloadTest.src.toString(), equalTo('https://alt-repository/TeamCity-9.1.7.tar.gz'))

            def startTestServer = project.tasks.getByName('startTestServer') as StartServer
            assertThat(startTestServer.homeDir.get(), endsWith('/alt/servers/TeamCity-9.1.7'))
            assertThat(startTestServer.dataDir.get(), endsWith('/alt/data/9.1'))
            assertThat(startTestServer.javaHome.get(), equalTo('/alt/java'))
            assertThat(startTestServer.serverOptions.get(), equalTo('-DserverOption1=value1 -DserverOption2=value2'))

            def startTestAgent = project.tasks.getByName('startTestAgent') as StartAgent
            assertThat(startTestAgent.agentOptions.get(), equalTo('-DagentOption1=value1 -DagentOption2=value2'))
        }
    }

    @Nested
    class UsingDocker {

        @BeforeEach
        void init() {
            project.apply plugin: 'io.github.rodm.teamcity-environments'
        }

        @Test
        void 'adds MavenCentral repository for resolving Docker dependencies'() {
            project.evaluate()

            List<String> urls = project.repositories.collect { repository -> repository.url.toString() }
            assertThat(urls, anyOf(hasItem('https://repo1.maven.org/maven2/'), hasItem('https://repo.maven.apache.org/maven2/')))
        }

        @Test
        @EnabledIfSystemProperty(named = "docker.version", matches = ".*")
        void 'apply adds docker dependencies to the docker configuration'() {
            project.evaluate()

            def dockerVersion = System.properties['docker.version'] as String

            Configuration configuration = project.configurations.getByName('docker')
            assertThat(configuration, hasDefaultDependency('com.github.docker-java', 'docker-java-core', dockerVersion))
            assertThat(configuration, hasDefaultDependency('com.github.docker-java', 'docker-java-transport-httpclient5', dockerVersion))
        }

        @Test
        void 'configure docker environment type'() {
            project.teamcity {
                environments {
                    test1(DockerTeamCityEnvironment) {
                        version = '2021.2.1'
                        serverImage = 'teamcity-server'
                    }
                    create('test2', DockerTeamCityEnvironment) {
                        version = '2021.2.2'
                        serverImage = 'teamcity-server'
                    }
                    register('test3', DockerTeamCityEnvironment) {
                        version = '2021.2.3'
                        serverImage = 'teamcity-server'
                    }
                }
            }
            project.evaluate()

            TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
            def environments = extension.extensions.findByName('environments') as TeamCityEnvironments

            assertThat(environments.getByName('test1'), isA(DockerTeamCityEnvironment))
            assertThat(environments.getByName('test2'), isA(DockerTeamCityEnvironment))
            assertThat(environments.getByName('test3'), isA(DockerTeamCityEnvironment))
        }

        @Test
        void 'configure environment types using convenience properties'() {
            project.teamcity {
                environments {
                    register('test1', Docker) {
                        version = '2021.2.1'
                    }
                    register('test2', Local) {
                        version = '2021.2.3'
                    }
                }
            }
            project.evaluate()

            TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
            def environments = extension.extensions.findByName('environments') as TeamCityEnvironments

            assertThat(environments.getByName('test1'), isA(DockerTeamCityEnvironment))
            assertThat(environments.getByName('test2'), isA(LocalTeamCityEnvironment))
        }

        @Test
        void 'creates tasks for a docker environment'() {
            project.teamcity {
                environments {
                    test(DockerTeamCityEnvironment) {
                        version = '2021.2.3'
                    }
                }
            }
            project.evaluate()

            assertThat(task('startTestServer'), isA(StartDockerServer))
            assertThat(task('stopTestServer'), isA(StopDockerServer))
            assertThat(task('startTestAgent'), isA(StartDockerAgent))
            assertThat(task('stopTestAgent'), isA(StopDockerAgent))
            assertThat(project, hasTask('startTest'))
            assertThat(project, hasTask('stopTest'))
        }

        @Test
        void 'configures startServer task'() {
            project.teamcity {
                environments {
                    test(DockerTeamCityEnvironment) {
                        version = '2021.2.3'
                        serverOptions '-DadditionalOption=value'
                    }
                }
            }
            project.evaluate()

            def startServer = project.tasks.getByName('startTestServer') as StartDockerServer
            assertThat(normalize(startServer.dataDir.get()), endsWith('data/2021.2'))
            def expectedOptions = defaultOptions + ' -DadditionalOption=value'
            assertThat(startServer.serverOptions.get(), equalTo(expectedOptions))
        }

        @Test
        void 'configures startAgent task'() {
            project.teamcity {
                environments {
                    test(DockerTeamCityEnvironment) {
                        version = '2021.2.3'
                        agentOptions = '-DadditionalOption=value'
                    }
                }
            }
            project.evaluate()

            def startAgent = project.tasks.getByName('startTestAgent') as StartDockerAgent
            def expectedOptions = '-DadditionalOption=value'
            assertThat(startAgent.agentOptions.get(), equalTo(expectedOptions))
        }

        @Test
        void 'configures tasks with default Docker images and container names'() {
            project.teamcity {
                environments {
                    test(DockerTeamCityEnvironment) {
                        version = '2021.2.3'
                    }
                }
            }
            project.evaluate()

            def startServer = project.tasks.getByName('startTestServer') as StartDockerServer
            assertThat(startServer.imageName.get(), equalTo('jetbrains/teamcity-server'))
            assertThat(startServer.imageTag.get(), equalTo('2021.2.3'))
            assertThat(startServer.containerName.get(), equalTo('teamcity-server'))
            def startAgent = project.tasks.getByName('startTestAgent') as StartDockerAgent
            assertThat(startAgent.imageName.get(), equalTo('jetbrains/teamcity-agent'))
            assertThat(startAgent.imageTag.get(), equalTo('2021.2.3'))
            assertThat(startAgent.containerName.get(), equalTo('teamcity-agent'))
        }

        @Test
        void 'configures tasks with alternative Docker images'() {
            project.teamcity {
                environments {
                    test(DockerTeamCityEnvironment) {
                        version = '2021.2.3'
                        serverImage = 'alt-teamcity-server'
                        agentImage = 'alt-teamcity-agent'
                    }
                }
            }
            project.evaluate()

            def startServer = project.tasks.getByName('startTestServer') as StartDockerServer
            assertThat(startServer.imageName.get(), equalTo('alt-teamcity-server'))
            def startAgent = project.tasks.getByName('startTestAgent') as StartDockerAgent
            assertThat(startAgent.imageName.get(), equalTo('alt-teamcity-agent'))
        }

        @Test
        void 'configures tasks with alternative Docker image tags'() {
            project.teamcity {
                environments {
                    test(DockerTeamCityEnvironment) {
                        version = '2021.2.3'
                        serverTag = '2021.2.3-linux'
                        agentTag = '2021.2.3-linux-sudo'
                    }
                }
            }
            project.evaluate()

            def startServer = project.tasks.getByName('startTestServer') as StartDockerServer
            assertThat(startServer.imageTag.get(), equalTo('2021.2.3-linux'))
            def startAgent = project.tasks.getByName('startTestAgent') as StartDockerAgent
            assertThat(startAgent.imageTag.get(), equalTo('2021.2.3-linux-sudo'))
        }

        @Test
        void 'configures tasks with alternative Docker container names'() {
            project.teamcity {
                environments {
                    test(DockerTeamCityEnvironment) {
                        version = '2021.2.3'
                        serverName = 'tc-server'
                        agentName = 'tc-agent'
                    }
                }
            }
            project.evaluate()

            def startServer = project.tasks.getByName('startTestServer') as StartDockerServer
            assertThat(startServer.containerName.get(), equalTo('tc-server'))
            def startAgent = project.tasks.getByName('startTestAgent') as StartDockerAgent
            assertThat(startAgent.containerName.get(), equalTo('tc-agent'))
            assertThat(startAgent.serverContainerName.get(), equalTo('tc-server'))

            def stopServer = project.tasks.getByName('stopTestServer') as StopDockerServer
            assertThat(stopServer.containerName.get(), equalTo('tc-server'))
            def stopAgent = project.tasks.getByName('stopTestAgent') as StopDockerAgent
            assertThat(stopAgent.containerName.get(), equalTo('tc-agent'))
        }

        @Test
        void 'configures tasks with default port'() {
            project.teamcity {
                environments {
                    test(DockerTeamCityEnvironment) {
                        version = '2021.2.3'
                    }
                }
            }
            project.evaluate()

            def startServer = project.tasks.getByName('startTestServer') as StartDockerServer
            assertThat(startServer.port.get(), equalTo('8111'))
            def deployPlugin = project.tasks.getByName('deployToTest') as Deploy
            assertThat(deployPlugin.serverPort.get(), equalTo('8111'))
            def undeployPlugin = project.tasks.getByName('undeployFromTest') as Undeploy
            assertThat(undeployPlugin.serverPort.get(), equalTo('8111'))
        }

        @Test
        void 'configures tasks with alternative server port'() {
            project.teamcity {
                environments {
                    test(DockerTeamCityEnvironment) {
                        version = '2021.2.3'
                        port = '8080'
                    }
                }
            }
            project.evaluate()

            def startServer = project.tasks.getByName('startTestServer') as StartDockerServer
            assertThat(startServer.port.get(), equalTo('8080'))
            def deployPlugin = project.tasks.getByName('deployToTest') as Deploy
            assertThat(deployPlugin.serverPort.get(), equalTo('8080'))
            def undeployPlugin = project.tasks.getByName('undeployFromTest') as Undeploy
            assertThat(undeployPlugin.serverPort.get(), equalTo('8080'))
        }

        @Test
        void 'configures start server task with default logs directory'() {
            project.teamcity {
                environments {
                    test(DockerTeamCityEnvironment) {
                        version = '2021.2.3'
                    }
                }
            }
            project.evaluate()

            def startServer = project.tasks.getByName('startTestServer') as StartDockerServer
            assertThat(normalize(startServer.logsDir.get()), endsWith('/data/2021.2/logs'))
        }

        @Test
        void 'configures start agent task with default agent configuration directory'() {
            project.teamcity {
                environments {
                    test(DockerTeamCityEnvironment) {
                        version = '2021.2.3'
                    }
                }
            }
            project.evaluate()

            def startAgent = project.tasks.getByName('startTestAgent') as StartDockerAgent
            assertThat(normalize(startAgent.configDir.get()), endsWith('/data/2021.2/agent/conf'))
        }

        @Disabled
        @Test
        void 'environments plugin configures environment tasks using overrides from gradle properties'() {
            projectDir.resolve('gradle.properties').toFile() << """
            teamcity.environments.test.serverImage = alt-teamcity-server
            teamcity.environments.test.agentImage = alt-teamcity-agent
            teamcity.environments.test.serverName = alt-server
            teamcity.environments.test.agentName = alt-agent
            teamcity.environments.test.port = 7070
            """
            project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
            // workaround for https://github.com/gradle/gradle/issues/13122
            (project as ProjectInternal).services.get(GradlePropertiesController).loadGradlePropertiesFrom(projectDir.toFile())

            project.apply plugin: 'io.github.rodm.teamcity-environments'
            project.teamcity {
                environments {
                    test(DockerTeamCityEnvironment) {
                        version = '2021.2.3'
                        port = '8080'
                        serverImage = 'my-teamcity-server'
                        agentImage = 'my-teamcity-agent'
                        serverName = 'my-teamcity-server'
                        agentName = 'my-teamcity-agent'
                    }
                }
            }
            project.evaluate()

            def startServer = project.tasks.getByName('startTestServer') as StartDockerServer
            assertThat(startServer.imageName.get(), equalTo('alt-teamcity-server'))
            assertThat(startServer.containerName.get(), equalTo('alt-server'))
            assertThat(startServer.port.get(), equalTo('7070'))
            def startAgent = project.tasks.getByName('startTestAgent') as StartDockerAgent
            assertThat(startAgent.imageName.get(), equalTo('alt-teamcity-agent'))
            assertThat(startAgent.containerName.get(), equalTo('alt-agent'))
            assertThat(startAgent.serverContainerName.get(), equalTo('alt-server'))
        }

        @Test
        void 'server image must not include a tag'() {
            def e = assertThrows(InvalidUserDataException, () -> {
                project.teamcity {
                    environments {
                        test(DockerTeamCityEnvironment) {
                            version = '2021.2.3'
                            serverImage = 'jetbrains/teamcity-server:2022.04'
                        }
                    }
                }
            })

            assertThat(e.message, equalTo('serverImage must not include a tag.'))
        }

        @Test
        void 'agent image must not include a tag'() {
            def e = assertThrows(InvalidUserDataException, () -> {
                project.teamcity {
                    environments {
                        test(DockerTeamCityEnvironment) {
                            version = '2021.2.3'
                            agentImage = 'jetbrains/teamcity-agent:2022.04'
                        }
                    }
                }
            })

            assertThat(e.message, equalTo('agentImage must not include a tag.'))
        }

        @Test
        void 'options with a debug port'() {
            // JDK 5-8
            def options1 = '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005'
            // JDK 9 or later
            def options2 =  '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006,allow=*'
            def options3 =  '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=teamcity-server:5007,timeout=1000'

            assertEquals('5005', getDebugPort(options1))
            assertEquals('5006', getDebugPort(options2))
            assertEquals('5007', getDebugPort(options3))
        }

        @Test
        void 'options without a debug port'() {
            assertEquals('', getDebugPort(''))
            assertEquals('', getDebugPort('jdwp'))
            assertEquals('', getDebugPort('address=1234'))
        }

        private static String getDebugPort(String options) {
            return DockerSupport.getDebugPort(options).orElse('')
        }
    }

    class TestPluginAction extends PluginAction {

        HttpURLConnection request
        String pluginName

        TestPluginAction(Logger logger, File dataDir, boolean enable) {
            super(logger, dataDir, [] as Set, [], enable)
        }

        @Override
        boolean canExecuteAction(Task task, String pluginName) {
            return false
        }

        void executeAction(String pluginName) {
            executeAction(pluginName, 'localhost', 8111)
        }

        @Override
        void sendRequest(HttpURLConnection request, String pluginName) {
            this.request = request
            this.pluginName = pluginName
        }

        boolean isServerAvailable(String host, int port) {
            return true
        }
    }

    private void createMaintenanceTokenFile() {
        File pluginDir = createDirectory(projectDir.resolve('system/pluginData/superUser'))
        File maintenanceTokenFile = new File(pluginDir, 'token.txt')
        maintenanceTokenFile << '0123456789012345'
    }

    @Test
    void 'does not send plugin action request when maintenance token file is not available'() {
        def action = new TestPluginAction(project.logger, projectDir.toFile(), false) {
            @Override
            void sendRequest(HttpURLConnection request, String pluginName) {
                fail('Should not send request when maintenance token file not available')
            }
        }

        action.executeAction('test-plugin.zip')

        assertThat(outputEventListener.toString(), containsString('Maintenance token file does not exist'))
        assertThat(outputEventListener.toString(), containsString('Cannot reload plugin.'))
    }

    @Test
    void 'sends plugin action to correct path'() {
        def action = new TestPluginAction(project.logger, projectDir.toFile(), false)
        createMaintenanceTokenFile()

        action.executeAction('test-plugin.zip')

        def url = action.request.URL
        assertThat(url.path, equalTo('/httpAuth/admin/plugins.html') )
    }

    @Test @SuppressWarnings('GroovyAccessibility')
    void 'sends plugin action with authorization token from maintenance file'() {
        def action = new TestPluginAction(project.logger, projectDir.toFile(), true)
        createMaintenanceTokenFile()

        action.executeAction('test-plugin.zip')

        def authToken = action.request.requests.findValue('Authorization')
        assertThat(authToken, containsString('Basic OjEyMzQ1Njc4OTAxMjM0NQ==') )
    }

    @Test
    void 'sends plugin action with settings to disable plugin'() {
        def action = new TestPluginAction(project.logger, projectDir.toFile(), false)
        createMaintenanceTokenFile()

        action.executeAction('test-plugin.zip')

        def url = action.request.URL
        assertThat(url.query, containsString('action=setEnabled&enabled=false') )
    }

    @Test
    void 'sends plugin action with settings to enable plugin'() {
        def action = new TestPluginAction(project.logger, projectDir.toFile(), true)
        createMaintenanceTokenFile()

        action.executeAction('test-plugin.zip')

        def url = action.request.URL
        assertThat(url.query, containsString('action=setEnabled&enabled=true') )
    }

    @Test
    void 'sends plugin action with encoded plugin path'() {
        def action = new TestPluginAction(project.logger, projectDir.toFile(), true)
        createMaintenanceTokenFile()

        action.executeAction('plugin-1.0.0+test.zip')

        def url = action.request.URL
        assertThat(url.query, containsString('pluginPath=%3CTeamCity+Data+Directory%3E%2Fplugins%2Fplugin-1.0.0%2Btest.zip'))
    }

    @Test
    void 'disabling plugin unload response logs success'() {
        def action = new DisablePluginAction(project.logger, projectDir.toFile(), [] as Set, [])
        createMaintenanceTokenFile()

        def request = mock(HttpURLConnection)
        when(request.inputStream).thenReturn(new ByteArrayInputStream("Plugin unloaded successfully".bytes))

        action.sendRequest(request, 'plugin-name.zip')

        assertThat(outputEventListener.toString(), containsString("Plugin 'plugin-name.zip' successfully unloaded"))
        assertThat(outputEventListener.toString(), not(containsString('partially unloaded')))
        assertThat(outputEventListener.toString(), not(containsString('Disabling plugin')))
    }

    @Test
    void 'disabling plugin unexpected response logs failure'() {
        def action = new DisablePluginAction(project.logger, projectDir.toFile(), [] as Set, [])
        createMaintenanceTokenFile()

        def request = mock(HttpURLConnection)
        when(request.inputStream).thenReturn(new ByteArrayInputStream("Unexpected response".bytes))

        action.sendRequest(request, 'plugin-name.zip')

        assertThat(outputEventListener.toString(), containsString("Disabling plugin 'plugin-name.zip' failed:"))
    }

    @Test
    void 'enabling plugin loaded response logs success'() {
        def action = new EnablePluginAction(project.logger, projectDir.toFile(), [] as Set, [])
        createMaintenanceTokenFile()

        def request = mock(HttpURLConnection)
        when(request.inputStream).thenReturn(new ByteArrayInputStream("Plugin loaded successfully".bytes))

        action.sendRequest(request, 'plugin-name.zip')

        assertThat(outputEventListener.toString(), containsString("Plugin 'plugin-name.zip' successfully loaded"))
    }

    @Test
    void 'enabling plugin unexpected response logs failure'() {
        def action = new EnablePluginAction(project.logger, projectDir.toFile(), [] as Set, [])
        createMaintenanceTokenFile()

        def request = mock(HttpURLConnection)
        when(request.inputStream).thenReturn(new ByteArrayInputStream("Unexpected response".bytes))

        action.sendRequest(request, 'plugin-name.zip')

        assertThat(outputEventListener.toString(), containsString("Enabling plugin 'plugin-name.zip' failed:"))
    }

    boolean wasRequestSent = false

    private DisablePluginAction createDisablePluginAction(def plugins, def unloaded) {
        createDisablePluginAction(plugins, unloaded, 'Plugin unloaded successfully')
    }

    private DisablePluginAction createDisablePluginAction(def plugins, def unloaded, String response) {
        def request = mock(HttpURLConnection)
        when(request.inputStream).thenReturn(new ByteArrayInputStream(response.bytes))
        new DisablePluginAction(project.logger, projectDir.toFile(), plugins , unloaded) {
            void executeAction(String pluginName, String host, int port) {
                sendRequest(request, pluginName)
                EnvironmentsTest.this.wasRequestSent = true
            }
        }
    }

    private EnablePluginAction createEnablePluginAction(def plugins, def unloaded) {
        def request = mock(HttpURLConnection)
        def response = 'Plugin loaded successfully'
        when(request.inputStream).thenReturn(new ByteArrayInputStream(response.bytes))
        new EnablePluginAction(project.logger, projectDir.toFile(), plugins, unloaded) {
            void executeAction(String pluginName, String host, int port) {
                sendRequest(request, pluginName)
                EnvironmentsTest.this.wasRequestSent = true
            }
        }
    }

    @Test
    void 'disable plugin request not sent for a new plugin'() {
        def pluginName = 'test-plugin.zip'
        File pluginDir = createDirectory(projectDir.resolve('plugins'))
        File pluginFile = createFile(projectDir.resolve(pluginName))
        def deploy = project.tasks.create('deploy', Deploy) {
            from { "${pluginFile.name}" }
            into { pluginDir }
        }

        Set<File> plugins = [pluginFile] as Set
        List<String> unloaded = []
        def action = createDisablePluginAction(plugins, unloaded)

        action.execute(deploy)

        assertFalse(wasRequestSent)
        assertThat('new plugin requires enabling', unloaded, hasItem(pluginName))
    }

    @Test
    void 'disable plugin request sent for an existing plugin'() {
        def pluginName = 'test-plugin.zip'
        File pluginDir = createDirectory(projectDir.resolve('plugins'))
        File pluginFile = createFile(pluginDir.toPath().resolve(pluginName))
        def deploy = project.tasks.create('deploy', Deploy) {
            from { "${pluginFile.name}" }
            into { pluginDir }
        }

        Set<File> plugins = [pluginFile] as Set
        List<String> unloaded = []
        def action = createDisablePluginAction(plugins, unloaded)

        action.execute(deploy)

        assertTrue(wasRequestSent)
        assertThat('existing plugin requires re-enabling', unloaded, hasItem(pluginName))
    }

    @Test
    void 'disable plugin request partially unloads existing plugin'() {
        def pluginName = 'test-plugin.zip'
        File pluginDir = createDirectory(projectDir.resolve('plugins'))
        File pluginFile = createFile(pluginDir.toPath().resolve(pluginName))
        def deploy = project.tasks.create('deploy', Deploy) {
            from { "${pluginFile.name}" }
            into { pluginDir }
        }

        Set<File> plugins = [pluginFile] as Set
        List<String> unloaded = []
        def action = createDisablePluginAction(plugins, unloaded, 'Plugin unloaded partially')

        action.execute(deploy)

        assertTrue(wasRequestSent)
        assertThat('partially unloaded plugin should be in reload list', unloaded, hasItem(pluginName))
    }

    @Test
    void 'enable plugin request not sent if plugin was not disabled'() {
        def pluginName = 'test-plugin.zip'
        File pluginDir = createDirectory(projectDir.resolve('plugins'))
        File pluginFile = createFile(projectDir.resolve(pluginName))
        def deploy = project.tasks.create('deploy', Deploy) {
            from { "${pluginFile.name}" }
            into { pluginDir }
        }
        Set<File> plugins = [pluginFile] as Set
        List<String> unloaded = []
        def action = createEnablePluginAction(plugins, unloaded)

        action.execute(deploy)

        assertFalse(wasRequestSent)
    }

    @Test
    void 'enable plugin request sent if plugin was disabled'() {
        def pluginName = 'test-plugin.zip'
        File pluginDir = createDirectory(projectDir.resolve('plugins'))
        File pluginFile = createFile(projectDir.resolve(pluginName))
        def deploy = project.tasks.create('deploy', Deploy) {
            from { "${pluginFile.name}" }
            into { pluginDir }
        }
        Set<File> plugins = [pluginFile] as Set
        // disabled and new plugins are added to list by disable action
        List<String> unloaded = [pluginName]
        def action = createEnablePluginAction(plugins, unloaded)

        action.execute(deploy)

        assertTrue(wasRequestSent)
    }

    class ConfigurationTestPluginAction extends PluginAction {

        String host
        int port

        ConfigurationTestPluginAction(Logger logger, File dataDir, Set<File> plugins) {
            super(logger, dataDir, plugins, [], true)
        }

        @Override
        boolean canExecuteAction(Task task, String name) {
            return true
        }

        @Override
        void executeAction(String name, String host, int port) {
            this.host = host
            this.port = port
        }

        @Override
        void sendRequest(HttpURLConnection request, String pluginName) {}
    }

    @Test
    void 'plugin action configured with host and port from deploy task'() {
        def pluginName = 'test-plugin.zip'
        File testPluginDir = createDirectory(projectDir.resolve('plugins'))
        File testPluginFile = createFile(projectDir.resolve(pluginName))
        Set<File> testPlugins = [testPluginFile] as Set
        def deploy = project.tasks.create('deploy', Deploy) {
            from { "${testPluginFile.name}" }
            into { testPluginDir }
            serverHost = 'teamcity-server'
            serverPort = '7111'
        }

        def action = new ConfigurationTestPluginAction(project.logger, projectDir.toFile(), testPlugins)
        action.execute(deploy)

        assertThat(action.host, equalTo('teamcity-server'))
        assertThat(action.port, equalTo(7111))
    }

    @Test
    void 'plugin action configured with host and port from undeploy task'() {
        def pluginName = 'test-plugin.zip'
        File testPluginDir = createDirectory(projectDir.resolve('plugins'))
        File testPluginFile = createFile(projectDir.resolve(pluginName))
        Set<File> testPlugins = [testPluginFile] as Set
        def undeploy = project.tasks.create('undeploy', Undeploy) {
            plugins.from(testPlugins)
            pluginsDir.set(testPluginDir)
            serverHost = 'teamcity-server'
            serverPort = '7111'
        }

        def action = new ConfigurationTestPluginAction(project.logger, projectDir.toFile(), testPlugins)
        action.execute(undeploy)

        assertThat(action.host, equalTo('teamcity-server'))
        assertThat(action.port, equalTo(7111))
    }

    private static File createFakeTeamCityInstall(Path folder, String baseDir, String version) {
        File homeDir = createDirectory(folder, "${baseDir}/TeamCity-${version}".toString())
        def releaseVersion = version.contains(' ') ? version.takeBefore(' ') : version
        if (TeamCityVersion.version(releaseVersion).lessThan(TeamCityVersion.version('2023.11'))) {
            createCommonApiJar(homeDir.toPath(), version)
        } else {
            createBuildVersionJar(homeDir.toPath(), version)
        }
        return homeDir
    }

    private static File createDirectory(Path folder, String name) {
        Files.createDirectories(folder.resolve(name)).toFile()
    }

    private static void createCommonApiJar(Path folder, String version) {
        Path jarPath = folder.resolve('webapps/ROOT/WEB-INF/lib/common-api.jar')
        createJar(jarPath, version)
    }

    private static void createBuildVersionJar(Path folder, String version) {
        Path jarPath = folder.resolve('webapps/ROOT/WEB-INF/lib/build-version.jar')
        createJar(jarPath, version)
    }

    private static void createJar(Path jarPath, String version) {
        jarPath.toFile().parentFile.mkdirs()

        Properties props = new Properties()
        props.put('Display_Version', version)
        FileOutputStream fos = new FileOutputStream(jarPath.toFile())
        ZipOutputStream zos = new ZipOutputStream(fos)
        ZipEntry ze = new ZipEntry('serverVersion.properties.xml')
        zos.putNextEntry(ze)
        props.storeToXML(zos, null)
        zos.closeEntry()
        zos.close()
    }
}
