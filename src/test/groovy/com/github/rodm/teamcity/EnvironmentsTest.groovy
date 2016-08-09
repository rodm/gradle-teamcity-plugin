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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
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
    public void defaultOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            server {
                environments {
                    test {
                    }
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.server.environments.getByName('test')
        assertThat(environment.serverOptions, equalTo(defaultOptions))
        assertThat(environment.agentOptions, equalTo(''))
    }

    @Test
    public void replaceDefaultServerOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            server {
                environments {
                    test {
                        serverOptions = '-DnewOption=test'
                    }
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.server.environments.getByName('test')
        assertThat(environment.serverOptions, equalTo('-DnewOption=test'))
    }

    @Test
    public void addToDefaultServerOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            server {
                environments {
                    test {
                        serverOptions '-DadditionalOption=test'
                    }
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.server.environments.getByName('test')
        assertThat(environment.serverOptions, equalTo(defaultOptions + ' -DadditionalOption=test'))
    }

    @Test
    public void replaceDefaultAgentOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            server {
                environments {
                    test {
                        agentOptions = '-DnewOption1=value1'
                        agentOptions = '-DnewOption2=value2'
                    }
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.server.environments.getByName('test')
        assertThat(environment.agentOptions, equalTo('-DnewOption2=value2'))
    }

    @Test
    public void addToDefaultAgentOptions() {
        project.apply plugin: 'com.github.rodm.teamcity-server'

        project.teamcity {
            server {
                environments {
                    test {
                        agentOptions '-DadditionalOption1=value1'
                        agentOptions '-DadditionalOption2=value2'
                    }
                }
            }
        }

        TeamCityPluginExtension extension = project.extensions.getByType(TeamCityPluginExtension)

        def environment = extension.server.environments.getByName('test')
        String expectedOptions = '-DadditionalOption1=value1 -DadditionalOption2=value2'
        assertThat(environment.agentOptions.trim(), equalTo(expectedOptions))
    }
}
