/*
 * Copyright 2016 Rod MacKenzie
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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

class EnvironmentsTest {

    private String defaultOptions = '-Dteamcity.development.mode=true -Dteamcity.development.shadowCopyClasses=true'

    private Project project;

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    public void defaultProperties() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.environments.getDownloadsDir(), equalTo('downloads'))
        assertThat(extension.environments.getBaseDownloadUrl(), equalTo('http://download.jetbrains.com/teamcity'))
        assertThat(extension.environments.getBaseDataDir(), equalTo('data'))
        assertThat(extension.environments.getBaseHomeDir(), equalTo('servers'))
    }

    @Test
    public void alternativeDownloadsDir() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            environments {
                downloadsDir = '/tmp/downloads'
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.environments.getDownloadsDir(), equalTo('/tmp/downloads'))
    }

    @Test
    public void alternativeDownloadBaseUrl() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            environments {
                baseDownloadUrl = 'http://local-repository'
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.environments.getBaseDownloadUrl(), equalTo('http://local-repository'))
    }

    @Test
    public void alternativeBaseDataDir() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            environments {
                baseDataDir = '/tmp/data'
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.environments.getBaseDataDir(), equalTo('/tmp/data'))
    }

    @Test
    public void alternativeBaseHomeDir() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            environments {
                baseHomeDir = '/tmp/servers'
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        assertThat(extension.environments.getBaseHomeDir(), equalTo('/tmp/servers'))
    }

    @Test
    public void defaultOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            environments {
                test {
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(environment.serverOptions, equalTo(defaultOptions))
        assertThat(environment.agentOptions, equalTo(''))
    }

    @Test
    public void replaceDefaultServerOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            environments {
                test {
                    serverOptions = '-DnewOption=test'
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(environment.serverOptions, equalTo('-DnewOption=test'))
    }

    @Test
    public void replaceDefaultServerOptionsWithMultipleValues() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            environments {
                test {
                    serverOptions = ['-Doption1=value1', '-Doption2=value2']
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(environment.serverOptions, equalTo('-Doption1=value1 -Doption2=value2'))
    }

    @Test
    public void addToDefaultServerOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            environments {
                test {
                    serverOptions '-DadditionalOption=test'
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(environment.serverOptions, equalTo(defaultOptions + ' -DadditionalOption=test'))
    }

    @Test
    public void addMultipleValuesToDefaultServerOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            environments {
                test {
                    serverOptions '-DadditionalOption1=value1', '-DadditionalOption2=value2'
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(environment.serverOptions, equalTo(defaultOptions + ' -DadditionalOption1=value1 -DadditionalOption2=value2'))
    }

    @Test
    public void replaceDefaultAgentOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

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
        assertThat(environment.agentOptions, equalTo('-DnewOption2=value2'))
    }
    @Test
    public void replaceDefaultAdentOptionsWithMultipleValues() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            environments {
                test {
                    agentOptions = ['-Doption1=value1', '-Doption2=value2']
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(environment.agentOptions, equalTo('-Doption1=value1 -Doption2=value2'))
    }

    @Test
    public void addToDefaultAgentOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

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
        assertThat(environment.agentOptions.trim(), equalTo(expectedOptions))
    }

    @Test
    public void addMultipleValuesToDefaultAgentOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            environments {
                test {
                    agentOptions '-DadditionalOption1=value1', '-DadditionalOption2=value2'
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.environments.getByName('test')
        assertThat(environment.agentOptions, equalTo('-DadditionalOption1=value1 -DadditionalOption2=value2'))
    }

    @Test
    public void 'ConfigureEnvironmentTasks adds tasks for an environment'() {
        project.apply plugin: 'com.github.rodm.teamcity-server'
        project.teamcity {
            environments {
                teamcity9 {
                    version = '9.1.7'
                }
            }
        }
        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)
        Action<Project> configureEnvironmentTasks = new TeamCityServerPlugin.ConfigureEnvironmentTasksAction(extension)

        configureEnvironmentTasks.execute(project)

        assertThat(project.tasks, hasTask('downloadTeamcity9'))
        assertThat(project.tasks, hasTask('unpackTeamcity9'))
        assertThat(project.tasks, hasTask('installTeamcity9'))

        assertThat(project.tasks, hasTask('deployPluginToTeamcity9'))
        assertThat(project.tasks, hasTask('undeployPluginFromTeamcity9'))

        assertThat(project.tasks, hasTask('startTeamcity9Server'))
        assertThat(project.tasks, hasTask('stopTeamcity9Server'))
        assertThat(project.tasks, hasTask('startTeamcity9Agent'))
        assertThat(project.tasks, hasTask('stopTeamcity9Agent'))
    }

    @Test
    public void 'ConfigureEnvironmentTasks adds tasks for each separate environment'() {
        project.apply plugin: 'com.github.rodm.teamcity-server'
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
        Action<Project> configureEnvironmentTasks = new TeamCityServerPlugin.ConfigureEnvironmentTasksAction(extension)

        configureEnvironmentTasks.execute(project)

        assertThat(project.tasks, hasTask('startTeamcity9Server'))
        assertThat(project.tasks, hasTask('startTeamcity10Server'))
    }

    private static Matcher<TaskContainer> hasTask(String name) {
        return new TypeSafeDiagnosingMatcher<TaskContainer>() {
            @Override
            public void describeTo(final Description description) {
                description.appendText("TaskContainer should contain task ").appendValue(name)
            }

            @Override
            protected boolean matchesSafely(final TaskContainer item, final Description mismatchDescription) {
                mismatchDescription.appendText(" was ").appendValue(item)
                return item.findByName(name)
            }
        }
    }
}
