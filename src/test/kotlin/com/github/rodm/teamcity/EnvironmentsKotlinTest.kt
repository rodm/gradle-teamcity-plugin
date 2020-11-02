/*
 * Copyright 2020 Rod MacKenzie
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

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EnvironmentsKotlinTest {

    @TempDir
    lateinit var projectDir: File

    private lateinit var project: Project
    private lateinit var teamcity: TeamCityPluginExtension

    private fun optionsAsString(options: Any): String {
        if (options is ListProperty<*>) {
            return options.get().joinToString(" ").trim()
        } else {
            return "";
        }
    }

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.plugins.apply("com.github.rodm.teamcity-environments")
        teamcity = project.extensions.getByType(TeamCityPluginExtension::class.java)
    }

    operator fun TeamCityPluginExtension.invoke(block: TeamCityPluginExtension.() -> Unit) = block()

    @Test
    fun `replace default agent options`() {
        teamcity {
            environments {
                operator fun String.invoke(block: TeamCityEnvironment.() -> Unit) = it.environments.create(this, block)

                "test" {
                    agentOptions = "-DnewOption1=value1"
                    agentOptions = "-DnewOption2=value2"
                }
            }
        }

        val environment = teamcity.environments.getByName("test")
        assertThat(optionsAsString(environment.agentOptions), equalTo("-DnewOption2=value2"))
    }

    @Test
    fun `replace default agent options with multiple values`() {
        teamcity {
            environments {
                operator fun String.invoke(block: TeamCityEnvironment.() -> Unit) = it.environments.create(this, block)

                "test" {
                    agentOptions = listOf("-Doption1=value1", "-Doption2=value2")
                }
            }
        }

        val environment = teamcity.environments.getByName("test")
        assertThat(optionsAsString(environment.agentOptions), equalTo("-Doption1=value1 -Doption2=value2"))
    }

    @Test
    fun `clear default server options`() {
        teamcity {
            environments {
                operator fun String.invoke(block: TeamCityEnvironment.() -> Unit) = it.environments.create(this, block)

                "test" {
                    serverOptions = emptyList<String>()
                }
            }
        }

        val environment = teamcity.environments.getByName("test")
        assertThat(optionsAsString(environment.serverOptions), equalTo(""))
    }

    @Test
    fun `replace default server options`() {
        teamcity {
            environments {
                operator fun String.invoke(block: TeamCityEnvironment.() -> Unit) = it.environments.create(this, block)

                "test" {
                    serverOptions = "-DnewOption=test"
                }
            }
        }

        val environment = teamcity.environments.getByName("test")
        assertThat(optionsAsString(environment.serverOptions), equalTo("-DnewOption=test"))
    }

    @Test
    fun `replace default server options with multiple values`() {
        teamcity {
            environments {
                operator fun String.invoke(block: TeamCityEnvironment.() -> Unit) = it.environments.create(this, block)

                "test" {
                    serverOptions = listOf("-Doption1=value1", "-Doption2=value2")
                }
            }
        }

        val environment = teamcity.environments.getByName("test")
        assertThat(optionsAsString(environment.serverOptions), equalTo("-Doption1=value1 -Doption2=value2"))
    }

    @Test
    fun `improve kotlin dsl by supporting create method`() {
        teamcity {
            environments {
                it.create("test") {
                    it.version = "2020.1"
                }
            }
        }

        val environment = teamcity.environments.named("test")
        assertThat(environment.get().version as String, equalTo("2020.1"))
    }

    @Test
    fun `improve kotlin dsl by supporting register method`() {
        teamcity {
            environments {
                it.register("test") {
                    it.version = "2020.1"
                }
            }
        }

        val environment = teamcity.environments.named("test")
        assertThat(environment.get().version as String, equalTo("2020.1"))
    }
}
