/*
 * Copyright 2016 Rod MacKenzie
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

import com.github.rodm.teamcity.tasks.DownloadTeamCity
import com.github.rodm.teamcity.tasks.InstallTeamCity
import com.github.rodm.teamcity.tasks.StartAgent
import com.github.rodm.teamcity.tasks.StartServer
import com.github.rodm.teamcity.tasks.StopAgent
import com.github.rodm.teamcity.tasks.StopServer
import com.github.rodm.teamcity.tasks.TeamCityTask
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import static com.github.rodm.teamcity.GradleMatchers.hasTask
import static com.github.rodm.teamcity.TestSupport.createDirectory
import static com.github.rodm.teamcity.TestSupport.createFile
import static com.github.rodm.teamcity.TestSupport.normalize
import static com.github.rodm.teamcity.TestSupport.normalizePath
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.endsWith
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.isA
import static org.hamcrest.Matchers.not
import static org.hamcrest.Matchers.startsWith
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assertions.fail
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

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

    private Project project

    static String optionsAsString(def options) {
        if (options instanceof ListProperty) {
            ListProperty<String> optionsList = (ListProperty) options
            return optionsList.get().join(' ').trim()
        } else {
            return ''
        }
    }

    @BeforeEach
    void setup() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
    }

    private static Action<Project>  createConfigureAction(TeamCityPluginExtension extension) {
        new TeamCityEnvironmentsPlugin.ConfigureEnvironmentTasksAction(extension)
    }

    @Test
    void defaultProperties() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'

        TeamCityEnvironments environments = project.extensions.getByType(TeamCityPluginExtension).environments
        assertThat(environments.downloadsDir, equalTo('downloads'))
        assertThat(environments.baseDownloadUrl, equalTo('https://download.jetbrains.com/teamcity'))
        assertThat(normalize(environments.baseHomeDir), endsWith('/servers'))
        assertThat(normalize(environments.baseDataDir), endsWith('/data'))
    }

    @Test
    void alternativeDownloadsDir() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'

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
        project.apply plugin: 'com.github.rodm.teamcity-environments'

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
        project.apply plugin: 'com.github.rodm.teamcity-environments'

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
        project.apply plugin: 'com.github.rodm.teamcity-environments'

        project.teamcity {
            environments {
                baseHomeDir = '/tmp/servers'
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(normalize(extension.environments.baseHomeDir), endsWith('/tmp/servers'))
    }

    @Test
    void 'gradle properties should override default shared properties'() {
        project.ext['teamcity.environments.downloadsDir'] = '/alt/downloads'
        project.ext['teamcity.environments.baseDownloadUrl'] = 'http://alt-repository'
        project.ext['teamcity.environments.baseHomeDir'] = '/alt/servers'
        project.ext['teamcity.environments.baseDataDir'] = '/alt/data'

        project.apply plugin: 'com.github.rodm.teamcity-environments'
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        assertThat(extension.environments.getDefaultDownloadsDir().get(), equalTo('/alt/downloads'))
        assertThat(extension.environments.getDefaultBaseDownloadUrl().get(), equalTo('http://alt-repository'))
        assertThat(extension.environments.getDefaultBaseHomeDir().get(), equalTo('/alt/servers'))
        assertThat(extension.environments.getDefaultBaseDataDir().get(), equalTo('/alt/data'))
    }

    @Test
    void 'gradle properties should override shared properties'() {
        project.ext['teamcity.environments.downloadsDir'] = '/alt/downloads'
        project.ext['teamcity.environments.baseDownloadUrl'] = 'http://alt-repository'
        project.ext['teamcity.environments.baseHomeDir'] = '/alt/servers'
        project.ext['teamcity.environments.baseDataDir'] = '/alt/data'

        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                downloadsDir = '/tmp/downloads'
                baseDownloadUrl = 'http://local-repository'
                baseHomeDir = '/tmp/servers'
                baseDataDir = '/tmp/data'
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        assertThat(extension.environments.getDefaultDownloadsDir().get(), equalTo('/alt/downloads'))
        assertThat(extension.environments.getDefaultBaseDownloadUrl().get(), equalTo('http://alt-repository'))
        assertThat(extension.environments.getDefaultBaseHomeDir().get(), equalTo('/alt/servers'))
        assertThat(extension.environments.getDefaultBaseDataDir().get(), equalTo('/alt/data'))
    }

    @Test
    void defaultOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'

        project.teamcity {
            environments {
                test {
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(optionsAsString(environment.serverOptions), equalTo(defaultOptions))
        assertThat(optionsAsString(environment.agentOptions), equalTo(''))
    }

    @Test
    void 'reject invalid version'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'

        try {
            project.teamcity {
                environments {
                    test {
                        version = '123'
                    }
                }
            }
            fail('Invalid version not rejected')
        }
        catch (IllegalArgumentException expected ) {
            assertThat(expected.message, startsWith("'123' is not a valid TeamCity version"))
        }
    }

    @Test
    void replaceDefaultServerOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'

        project.teamcity {
            environments {
                test {
                    serverOptions = '-DnewOption=test'
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(optionsAsString(environment.serverOptions), equalTo('-DnewOption=test'))
    }

    @Test
    void replaceDefaultServerOptionsWithMultipleValues() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'

        project.teamcity {
            environments {
                test {
                    serverOptions = ['-Doption1=value1', '-Doption2=value2']
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(optionsAsString(environment.serverOptions), equalTo('-Doption1=value1 -Doption2=value2'))
    }

    @Test
    void addToDefaultServerOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'

        project.teamcity {
            environments {
                test {
                    serverOptions '-DadditionalOption=test'
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(optionsAsString(environment.serverOptions), equalTo(defaultOptions + ' -DadditionalOption=test'))
    }

    @Test
    void addMultipleValuesToDefaultServerOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'

        project.teamcity {
            environments {
                test {
                    serverOptions '-DadditionalOption1=value1', '-DadditionalOption2=value2'
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(optionsAsString(environment.serverOptions), equalTo(defaultOptions + ' -DadditionalOption1=value1 -DadditionalOption2=value2'))
    }

    @Test
    void replaceDefaultAgentOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'

        project.teamcity {
            environments {
                test {
                    agentOptions = '-DnewOption1=value1'
                    agentOptions = '-DnewOption2=value2'
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(optionsAsString(environment.agentOptions), equalTo('-DnewOption2=value2'))
    }

    @Test
    void replaceDefaultAdentOptionsWithMultipleValues() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'

        project.teamcity {
            environments {
                test {
                    agentOptions = ['-Doption1=value1', '-Doption2=value2']
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(optionsAsString(environment.agentOptions), equalTo('-Doption1=value1 -Doption2=value2'))
    }

    @Test
    void addToDefaultAgentOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'

        project.teamcity {
            environments {
                test {
                    agentOptions '-DadditionalOption1=value1'
                    agentOptions '-DadditionalOption2=value2'
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        String expectedOptions = '-DadditionalOption1=value1 -DadditionalOption2=value2'
        assertThat(optionsAsString(environment.agentOptions), equalTo(expectedOptions))
    }

    @Test
    void addMultipleValuesToDefaultAgentOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'

        project.teamcity {
            environments {
                test {
                    agentOptions '-DadditionalOption1=value1', '-DadditionalOption2=value2'
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        String expectedOptions = '-DadditionalOption1=value1 -DadditionalOption2=value2'
        assertThat(optionsAsString(environment.agentOptions), equalTo(expectedOptions))
    }

    @Test
    void 'configure multiple plugins for an environment'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                test {
                    plugins 'plugin1.zip'
                    plugins 'plugin2.zip'
                }
            }
        }
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        TeamCityEnvironment environment = extension.environments.getByName('test')
        List<File> plugins = environment.plugins.toList()
        assertThat(plugins, hasSize(2))
        assertThat(plugins, hasItem(project.file('plugin1.zip')))
        assertThat(plugins, hasItem(project.file('plugin2.zip')))
    }

    @Test
    void 'configure plugin with assignment replaces'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                test {
                    plugins = 'plugin1.zip'
                    plugins = 'plugin2.zip'
                }
            }
        }
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        TeamCityEnvironment environment = extension.environments.getByName('test')
        List<File> plugins = environment.plugins.toList()
        assertThat(plugins, hasSize(1))
        assertThat(plugins, hasItem(project.file('plugin2.zip')))
    }

    @Test
    void 'configure multiple plugins for an environment with a list'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                test {
                    plugins = ['plugin1.zip', 'plugin2.zip']
                }
            }
        }
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        TeamCityEnvironment environment = extension.environments.getByName('test')
        def plugins = environment.plugins.toList()
        assertThat(plugins, hasSize(2))
        assertThat(plugins, hasItem(project.file('plugin1.zip')))
        assertThat(plugins, hasItem(project.file('plugin2.zip')))
    }

    private String ENVIRONMENTS_WARNING = 'Configuring environments with the teamcity-server plugin is deprecated'

    @Test
    void 'configuring environment with teamcity-environments plugin does not output warning'() {
        outputEventListener.reset()
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                test {
                }
            }
        }

        assertThat(outputEventListener.toString(), not(containsString(ENVIRONMENTS_WARNING)))
    }

    @Test
    void 'configuring environment with teamcity-server plugin outputs warning'() {
        outputEventListener.reset()
        project.apply plugin: 'com.github.rodm.teamcity-server'
        project.teamcity {
            environments {
                test {
                }
            }
        }

        assertThat(outputEventListener.toString(), containsString(ENVIRONMENTS_WARNING))
    }

    @Test
    void 'configuring environment with teamcity-environments and teamcity-server plugins does not output warning'() {
        outputEventListener.reset()
        project.apply plugin: 'com.github.rodm.teamcity-server'
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                test {
                }
            }
        }

        assertThat(outputEventListener.toString(), not(containsString(ENVIRONMENTS_WARNING)))
    }

    @Test
    void 'ConfigureEnvironmentTasks configures environments with default properties'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
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
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = createConfigureAction(extension)

        configureEnvironmentTasks.execute(project)

        def environment1 = extension.environments.getByName('test1')
        assertThat(environment1.downloadUrl, equalTo('https://download.jetbrains.com/teamcity/TeamCity-9.1.7.tar.gz'))
        assertThat(normalize(environment1.homeDir), endsWith('/servers/TeamCity-9.1.7'))
        assertThat(normalize(environment1.dataDir), endsWith('/data/9.1'))
        assertThat(normalize(environment1.javaHome), equalTo(normalize(System.getProperty('java.home'))))

        def environment2 = extension.environments.getByName('test2')
        assertThat(environment2.downloadUrl, equalTo('https://download.jetbrains.com/teamcity/TeamCity-10.0.4.tar.gz'))
        assertThat(normalize(environment2.homeDir), endsWith('/servers/TeamCity-10.0.4'))
        assertThat(normalize(environment2.dataDir), endsWith('/data/10.0'))
        assertThat(normalize(environment2.javaHome), equalTo(normalize(System.getProperty('java.home'))))
    }

    @Test
    void 'ConfigureEnvironmentTasks configures environments using shared properties'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
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
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = createConfigureAction(extension)

        configureEnvironmentTasks.execute(project)

        def environment1 = extension.environments.getByName('test1')
        assertThat(environment1.downloadUrl, equalTo('http://local-repository/TeamCity-9.1.7.tar.gz'))
        assertThat(normalize(environment1.homeDir), endsWith('/tmp/servers/TeamCity-9.1.7'))
        assertThat(normalize(environment1.dataDir), endsWith('/tmp/data/9.1'))

        def environment2 = extension.environments.getByName('test2')
        assertThat(environment2.downloadUrl, equalTo('http://local-repository/TeamCity-10.0.4.tar.gz'))
        assertThat(normalize(environment2.homeDir), endsWith('/tmp/servers/TeamCity-10.0.4'))
        assertThat(normalize(environment2.dataDir), endsWith('/tmp/data/10.0'))
    }

    @Test
    void 'ConfigureEnvironmentTasks configures environments composed with shared properties'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
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
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = createConfigureAction(extension)

        configureEnvironmentTasks.execute(project)

        def environment1 = extension.environments.getByName('test1')
        assertThat(normalize(environment1.homeDir), endsWith('/tmp/servers/Test1'))
        assertThat(normalize(environment1.dataDir), endsWith('/tmp/data/Test1'))
    }

    @Test
    void 'ConfigureEnvironmentTasks configures environment using environment properties'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
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
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = createConfigureAction(extension)

        configureEnvironmentTasks.execute(project)

        def environment = extension.environments.getByName('test')
        assertThat(environment.downloadUrl, equalTo('http://local-repository/TeamCity-9.1.7.tar.gz'))
        assertThat(normalize(environment.homeDir), endsWith('/tmp/servers/TeamCity-9.1.7'))
        assertThat(normalize(environment.dataDir), endsWith('/tmp/data/teamcity9.1'))
        assertThat(normalize(environment.javaHome), endsWith('/tmp/java'))
    }

    @Test
    void 'ConfigureEnvironmentTasks configures environment using overrides from gradle properties'() {
        project.ext['teamcity.environments.test.downloadUrl'] = 'https://alt-repository/TeamCity-9.1.7.tar.gz'
        project.ext['teamcity.environments.test.javaHome'] = '/alt/java'
        project.ext['teamcity.environments.test.homeDir'] = '/alt/servers/TeamCity-9.1.7'
        project.ext['teamcity.environments.test.dataDir'] = '/alt/data/9.1'

        project.apply plugin: 'com.github.rodm.teamcity-environments'
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
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = createConfigureAction(extension)

        configureEnvironmentTasks.execute(project)

        def environment = extension.environments.getByName('test')
        assertThat(environment.downloadUrl, equalTo('https://alt-repository/TeamCity-9.1.7.tar.gz'))
        assertThat(normalize(environment.javaHome), endsWith('/alt/java'))
        assertThat(normalize(environment.homeDir), endsWith('/alt/servers/TeamCity-9.1.7'))
        assertThat(normalize(environment.dataDir), endsWith('/alt/data/9.1'))
    }

    @Test
    void 'ConfigureEnvironmentTasks adds tasks for an environment'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                teamcity9 {
                    version = '9.1.7'
                }
            }
        }
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = createConfigureAction(extension)

        configureEnvironmentTasks.execute(project)

        assertThat(project.tasks, hasTask('downloadTeamcity9'))
        assertThat(project.tasks, hasTask('installTeamcity9'))

        assertThat(project.tasks, hasTask('deployToTeamcity9'))
        assertThat(project.tasks, hasTask('undeployFromTeamcity9'))

        assertThat(project.tasks, hasTask('startTeamcity9Server'))
        assertThat(project.tasks, hasTask('stopTeamcity9Server'))
        assertThat(project.tasks, hasTask('startTeamcity9Agent'))
        assertThat(project.tasks, hasTask('stopTeamcity9Agent'))
        assertThat(project.tasks, hasTask('startTeamcity9'))
        assertThat(project.tasks, hasTask('stopTeamcity9'))
    }

    @Test
    void 'configures deploy task with actions to disable and enable plugin for version 2018_2 and later'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                test {
                    version = '2018.2'
                }
            }
        }
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = createConfigureAction(extension)

        configureEnvironmentTasks.execute(project)

        Copy deployPlugin = project.tasks.getByName('deployToTest') as Copy
        List<String> taskActionClassNames = deployPlugin.taskActions.collect { it.action.getClass().name }
        assertThat(taskActionClassNames, hasItem(TeamCityEnvironmentsPlugin.DisablePluginAction.name))
        assertThat(taskActionClassNames, hasItem(TeamCityEnvironmentsPlugin.EnablePluginAction.name))
    }

    @Test
    void 'configures deploy task without actions to disable and enable plugin for version 2018_1 and earlier'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                test {
                    version = '2018.1'
                }
            }
        }
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = createConfigureAction(extension)

        configureEnvironmentTasks.execute(project)

        Copy deployPlugin = project.tasks.getByName('deployToTest') as Copy
        List<String> taskActionClassNames = deployPlugin.taskActions.collect { it.action.getClass().name }
        assertThat(taskActionClassNames, not(hasItem(TeamCityEnvironmentsPlugin.DisablePluginAction.name)))
        assertThat(taskActionClassNames, not(hasItem(TeamCityEnvironmentsPlugin.EnablePluginAction.name)))
    }

    @Test
    void 'configures undeploy task with action to disable the plugin for version 2018_2 and later'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                test {
                    version = '2018.2'
                }
            }
        }
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = createConfigureAction(extension)

        configureEnvironmentTasks.execute(project)

        Delete undeployPlugin = project.tasks.getByName('undeployFromTest') as Delete
        List<String> taskActionClassNames = undeployPlugin.taskActions.collect { it.action.getClass().name }
        assertThat(taskActionClassNames, hasItem(TeamCityEnvironmentsPlugin.DisablePluginAction.name))
    }

    @Test
    void 'configures undeploy task without action to disable the plugin for version 2018_1 and earlier'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                test {
                    version = '2018.1'
                }
            }
        }
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = createConfigureAction(extension)

        configureEnvironmentTasks.execute(project)

        Delete undeployPlugin = project.tasks.getByName('undeployFromTest') as Delete
        List<String> taskActionClassNames = undeployPlugin.taskActions.collect { it.action.getClass().name }
        assertThat(taskActionClassNames, not(hasItem(TeamCityEnvironmentsPlugin.DisablePluginAction.name)))
    }

    @Test
    void 'ConfigureEnvironmentTasks adds tasks for each separate environment'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
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
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = createConfigureAction(extension)

        configureEnvironmentTasks.execute(project)

        assertThat(project.tasks, hasTask('startTeamcity9Server'))
        assertThat(project.tasks, hasTask('startTeamcity10Server'))
    }

    @Test
    void 'ConfigureEnvironmentTasks configures deploy and undeploy tasks with project plugin'() {
        project.apply plugin: 'com.github.rodm.teamcity-server'
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                teamcity10 {
                    version = '10.0.3'
                }
            }
        }
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = createConfigureAction(extension)

        configureEnvironmentTasks.execute(project)

        Copy deployPlugin = project.tasks.getByName('deployToTeamcity10') as Copy
        List deployFiles = deployPlugin.mainSpec.sourcePaths[0].call().toList()
        assertThat(deployFiles, hasSize(1))
        assertThat(deployFiles, hasItem(new File(project.rootDir, 'build/distributions/test.zip')))
        assertThat(normalizePath(deployPlugin.rootSpec.destinationDir), endsWith('data/10.0/plugins'))

        Delete undeployPlugin = project.tasks.getByName('undeployFromTeamcity10') as Delete
        ConfigurableFileTree fileTree = undeployPlugin.delete[0].call()
        assertThat(normalizePath(fileTree.dir), endsWith('data/10.0/plugins'))
        assertThat(fileTree.patterns.includes, hasSize(1))
        assertThat(fileTree.patterns.includes, hasItem('test.zip'))
    }

    @Test
    void 'ConfigureEnvironmentTasks configures deploy and undeploy tasks with environment plugins'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                teamcity10 {
                    version = '10.0.3'
                    plugins 'plugin1.zip'
                    plugins 'plugin2.zip'
                }
            }
        }
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = createConfigureAction(extension)

        configureEnvironmentTasks.execute(project)

        Copy deployPlugin = project.tasks.getByName('deployToTeamcity10') as Copy
        List deployFiles = deployPlugin.mainSpec.sourcePaths[0].call().toList()
        assertThat(deployFiles, hasSize(2))
        assertThat(deployFiles, hasItem(project.file('plugin1.zip')))
        assertThat(deployFiles, hasItem(project.file('plugin2.zip')))

        Delete undeployPlugin = project.tasks.getByName('undeployFromTeamcity10') as Delete
        ConfigurableFileTree fileTree = undeployPlugin.delete[0].call()
        assertThat(fileTree.patterns.includes, hasSize(2))
        assertThat(fileTree.patterns.includes, hasItem('plugin1.zip'))
        assertThat(fileTree.patterns.includes, hasItem('plugin2.zip'))
    }

    @Test
    void 'applying teamcity-server and teamcity-environments does not duplicate tasks'() {
        outputEventListener.reset()
        project.apply plugin: 'com.github.rodm.teamcity-server'
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity {
            environments {
                test {
                }
            }
        }
        project.evaluate()

        assertThat(project.tasks, hasTask('downloadTest'))
    }

    @Test
    void 'applying teamcity-environments before teamcity-server does not duplicate environment tasks'() {
        outputEventListener.reset()
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.apply plugin: 'com.github.rodm.teamcity-server'
        project.teamcity {
            environments {
                test {
                }
            }
        }
        project.evaluate()

        assertThat(project.tasks, hasTask('downloadTest'))
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
        project.apply plugin: 'com.github.rodm.teamcity-environments'
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
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity TEAMCITY10_ENVIRONMENT

        project.evaluate()

        StopServer stopServer = project.tasks.getByName('stopTeamcity10Server') as StopServer
        assertThat(normalize(stopServer.homeDir.get()), endsWith('servers/TeamCity-10.0.4'))
        assertThat(normalize(stopServer.javaHome.get()), endsWith('/opt/jdk1.8.0'))
    }

    @Test
    void 'configures startAgent task'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity TEAMCITY10_ENVIRONMENT

        project.evaluate()

        StartAgent startAgent = project.tasks.getByName('startTeamcity10Agent') as StartAgent
        assertThat(normalize(startAgent.homeDir.get()), endsWith('servers/TeamCity-10.0.4'))
        assertThat(normalize(startAgent.javaHome.get()), endsWith('/opt/jdk1.8.0'))
        assertThat(startAgent.agentOptions.get(), endsWith('-DagentOption=agentValue'))
    }

    @Test
    void 'configures stopAgent task'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity TEAMCITY10_ENVIRONMENT

        project.evaluate()

        StopAgent stopAgent = project.tasks.getByName('stopTeamcity10Agent') as StopAgent
        assertThat(normalize(stopAgent.homeDir.get()), endsWith('servers/TeamCity-10.0.4'))
        assertThat(normalize(stopAgent.javaHome.get()), endsWith('/opt/jdk1.8.0'))
    }

    @Test
    void 'teamcity task validates home directory'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
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
        project.apply plugin: 'com.github.rodm.teamcity-environments'
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
    void 'teamcity task outputs no warning when environment version matches installed version'() {
        createFakeTeamCityInstall(projectDir, 'servers', '2020.2.3')
        project.apply plugin: 'com.github.rodm.teamcity-environments'
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

        String expectedMessage = String.format(TeamCityTask.VERSION_MISMATCH_WARNING[4..-4], '2020.2.3')
        assertThat(outputEventListener.toString(), not(containsString(expectedMessage)))
    }

    @Test
    void 'teamcity task output a warning when environment version does not match at bugfix level'() {
        File fakeHomeDir = createFakeTeamCityInstall(projectDir, 'servers', '2020.2.3')
        project.apply plugin: 'com.github.rodm.teamcity-environments'
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

        String expectedMessage = String.format(TeamCityTask.VERSION_MISMATCH_WARNING[4..-4], '2020.2.5')
        assertThat(outputEventListener.toString(), containsString(expectedMessage))
    }

    @Test
    void 'teamcity task fail when environment version is not data compatible with installed version'() {
        File fakeHomeDir = createFakeTeamCityInstall(projectDir, 'servers', '2020.2.3')
        project.apply plugin: 'com.github.rodm.teamcity-environments'
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

        String expectedMessage = String.format(TeamCityTask.VERSION_INCOMPATIBLE[0..-4], '2020.1.2')
        assertThat(e.message, containsString(expectedMessage))
    }

    @Test
    void 'teamcity task validates Java home directory'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
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
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity TEAMCITY10_ENVIRONMENT

        project.evaluate()

        InstallTeamCity install = project.tasks.getByName('installTeamcity10') as InstallTeamCity
        assertThat(normalizePath(install.getSource()), endsWith('downloads/TeamCity-10.0.4.tar.gz'))
        assertThat(normalizePath(install.getTarget()), endsWith('servers/TeamCity-10.0.4'))
    }

    @Test
    void 'configures download task'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity TEAMCITY10_ENVIRONMENT

        project.evaluate()

        DownloadTeamCity download = project.tasks.getByName('downloadTeamcity10') as DownloadTeamCity
        assertThat(download.getSrc().toString(), equalTo('http://local-repository/TeamCity-10.0.4.tar.gz'))
        assertThat(normalizePath(download.getDest()), endsWith('downloads/TeamCity-10.0.4.tar.gz'))
    }

    @Test
    void 'extension has named child extensions'() {
        project.apply plugin: 'com.github.rodm.teamcity-environments'
        project.teamcity { }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        def agent = extension.extensions.findByName('agent')
        def server = extension.extensions.findByName('server')
        def environments = extension.extensions.findByName('environments')

        assertThat(agent, isA(AgentPluginConfiguration))
        assertThat(server, isA(ServerPluginConfiguration))
        assertThat(environments, isA(TeamCityEnvironments))
    }

    class TestPluginAction extends TeamCityEnvironmentsPlugin.PluginAction {

        HttpURLConnection request
        String pluginName

        TestPluginAction(Logger logger, File dataDir, boolean enable) {
            super(logger, dataDir, [] as Set, [], enable)
        }

        @Override
        boolean canExecuteAction(Task task, String pluginName) {
            return false
        }

        @Override
        void sendRequest(HttpURLConnection request, String pluginName) {
            this.request = request
            this.pluginName = pluginName
        }

        boolean isServerAvailable() {
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

    @Test
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
        def action = new TeamCityEnvironmentsPlugin.DisablePluginAction(project.logger, projectDir.toFile(), [] as Set, [])
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
        def action = new TeamCityEnvironmentsPlugin.DisablePluginAction(project.logger, projectDir.toFile(), [] as Set, [])
        createMaintenanceTokenFile()

        def request = mock(HttpURLConnection)
        when(request.inputStream).thenReturn(new ByteArrayInputStream("Unexpected response".bytes))

        action.sendRequest(request, 'plugin-name.zip')

        assertThat(outputEventListener.toString(), containsString("Disabling plugin 'plugin-name.zip' failed:"))
    }

    @Test
    void 'enabling plugin loaded response logs success'() {
        def action = new TeamCityEnvironmentsPlugin.EnablePluginAction(project.logger, projectDir.toFile(), [] as Set, [])
        createMaintenanceTokenFile()

        def request = mock(HttpURLConnection)
        when(request.inputStream).thenReturn(new ByteArrayInputStream("Plugin loaded successfully".bytes))

        action.sendRequest(request, 'plugin-name.zip')

        assertThat(outputEventListener.toString(), containsString("Plugin 'plugin-name.zip' successfully loaded"))
    }

    @Test
    void 'enabling plugin unexpected response logs failure'() {
        def action = new TeamCityEnvironmentsPlugin.EnablePluginAction(project.logger, projectDir.toFile(), [] as Set, [])
        createMaintenanceTokenFile()

        def request = mock(HttpURLConnection)
        when(request.inputStream).thenReturn(new ByteArrayInputStream("Unexpected response".bytes))

        action.sendRequest(request, 'plugin-name.zip')

        assertThat(outputEventListener.toString(), containsString("Enabling plugin 'plugin-name.zip' failed:"))
    }

    boolean wasRequestSent = false

    private TeamCityEnvironmentsPlugin.DisablePluginAction createDisablePluginAction(def plugins, def unloaded) {
        createDisablePluginAction(plugins, unloaded, 'Plugin unloaded successfully')
    }

    private TeamCityEnvironmentsPlugin.DisablePluginAction createDisablePluginAction(def plugins, def unloaded, String response) {
        def request = mock(HttpURLConnection)
        when(request.inputStream).thenReturn(new ByteArrayInputStream(response.bytes))
        new TeamCityEnvironmentsPlugin.DisablePluginAction(project.logger, projectDir.toFile(), plugins , unloaded) {
            void executeAction(String pluginName) {
                sendRequest(request, pluginName)
                wasRequestSent = true
            }
        }
    }

    private TeamCityEnvironmentsPlugin.EnablePluginAction createEnablePluginAction(def plugins, def unloaded) {
        def request = mock(HttpURLConnection)
        def response = 'Plugin loaded successfully'
        when(request.inputStream).thenReturn(new ByteArrayInputStream(response.bytes))
        new TeamCityEnvironmentsPlugin.EnablePluginAction(project.logger, projectDir.toFile(), plugins, unloaded) {
            void executeAction(String pluginName) {
                sendRequest(request, pluginName)
                wasRequestSent = true
            }
        }
    }

    @Test
    void 'disable plugin request not sent for a new plugin'() {
        def pluginName = 'test-plugin.zip'
        File pluginDir = createDirectory(projectDir.resolve('plugins'))
        File pluginFile = createFile(projectDir.resolve(pluginName))
        def deploy = project.tasks.create('deploy', Copy) {
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
        def deploy = project.tasks.create('deploy', Copy) {
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
        def deploy = project.tasks.create('deploy', Copy) {
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
        def deploy = project.tasks.create('deploy', Copy) {
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
        def deploy = project.tasks.create('deploy', Copy) {
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

    private static File createFakeTeamCityInstall(Path folder, String baseDir, String version) {
        File homeDir = createDirectory(folder, "${baseDir}/TeamCity-${version}".toString())
        createCommonApiJar(homeDir.toPath(), version)
        return homeDir
    }

    private static File createDirectory(Path folder, String name) {
        Files.createDirectories(folder.resolve(name)).toFile()
    }

    private static void createCommonApiJar(Path folder, String version) {
        Path jarPath = folder.resolve('webapps/ROOT/WEB-INF/lib/common-api.jar')
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
