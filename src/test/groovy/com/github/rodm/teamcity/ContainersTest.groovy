/*
 * Copyright 2025 Rod MacKenzie
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

import com.github.rodm.teamcity.tasks.StartDockerContainer
import com.github.rodm.teamcity.tasks.StopDockerContainer
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

import java.nio.file.Path

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasEntry
import static org.hamcrest.Matchers.instanceOf
import static org.hamcrest.Matchers.nullValue
import static org.junit.jupiter.api.Assertions.assertThrows

@SuppressWarnings('ConfigurationAvoidance')
class ContainersTest {

    @TempDir
    public Path projectDir

    private Project project

    private Task task(String name) {
        project.tasks.findByName(name)
    }

    @BeforeEach
    void setup() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
        project.apply plugin: 'io.github.rodm.teamcity-environments'
    }

    @Test
    void 'container configuration and no teamcity environment does not create tasks'() {
        project.teamcity {
            environments {
                containers {
                    example {}
                }
            }
        }
        project.evaluate()

        assertThat(task('startTestExample'), nullValue())
        assertThat(task('stopTestExample'), nullValue())
    }

    @Test
    void 'container configuration and teamcity environment creates start and stop tasks for the container'() {
        project.teamcity {
            environments {
                containers {
                    example {}
                }
                test {}
            }
        }
        project.evaluate()

        assertThat(task('startTestExample'), instanceOf(StartDockerContainer))
        assertThat(task('stopTestExample'), instanceOf(StopDockerContainer))
    }

    @Test
    void 'container configuration and multiple teamcity environments creates container start and stop tasks for each environment'() {
        project.teamcity {
            environments {
                containers {
                    example {}
                }
                test1 {}
                test2 {}
            }
        }
        project.evaluate()

        assertThat(task('startTest1Example'), instanceOf(StartDockerContainer))
        assertThat(task('stopTest1Example'), instanceOf(StopDockerContainer))
        assertThat(task('startTest2Example'), instanceOf(StartDockerContainer))
        assertThat(task('stopTest2Example'), instanceOf(StopDockerContainer))
    }

    @Test
    void 'container start and stop tasks are configured with a container name'() {
        project.teamcity {
            environments {
                containers {
                    example {}
                }
                test {}
            }
        }
        project.evaluate()

        def startTestExample = task('startTestExample') as StartDockerContainer
        assertThat(startTestExample.containerName.get(), equalTo('example'))
        def stopTestExample = task('stopTestExample') as StopDockerContainer
        assertThat(stopTestExample.containerName.get(), equalTo('example'))
    }

    @Test
    void 'container start task is configured with a docker image'() {
        project.teamcity {
            environments {
                containers {
                    example {
                        image = "postgres:18.1"
                    }
                }
                test {}
            }
        }
        project.evaluate()

        def startTestExample = task('startTestExample') as StartDockerContainer
        assertThat(startTestExample.image.get(), equalTo('postgres:18.1'))
    }

    @Test
    void 'container start task is configured with a port mapping'() {
        project.teamcity {
            environments {
                containers {
                    example {
                        ports {
                            port ('2345', '5432')
                        }
                    }
                }
                test {}
            }
        }
        project.evaluate()


        def startTestExample = task('startTestExample') as StartDockerContainer
        assertThat(startTestExample.ports.get(), hasEntry('2345', '5432'))
    }

    @Test
    void 'container start task is configured with a volume mapping'() {
        project.teamcity {
            environments {
                containers {
                    example {
                        volumes {
                            volume ('data', '/var/lib/data')
                        }
                    }
                }
                test {}
            }
        }
        project.evaluate()

        def startTestExample = task('startTestExample') as StartDockerContainer
        assertThat(startTestExample.volumes.get(), hasEntry('data', '/var/lib/data'))
    }

    @ParameterizedTest
    @ValueSource(strings = ['', '/', '/data', 'path/data', 'data/', 'path\\data'])
    void 'container host path volume mapping should not contain a path separator'(String path) {
        def e= assertThrows(InvalidUserDataException, {
            project.teamcity {
                environments {
                    containers {
                        example {
                            volumes {
                                volume(path, '/var/lib/data')
                            }
                        }
                    }
                }
            }
        })

        assertThat(e.message, equalTo('Invalid host path: ' + path))
    }

    @Test
    void 'container start task is configured with an environment variable'() {
        project.teamcity {
            environments {
                containers {
                    example {
                        variables {
                            variable ('USER', 'tester')
                        }
                    }
                }
                test {}
            }
        }
        project.evaluate()

        def startTestExample = task('startTestExample') as StartDockerContainer
        assertThat(startTestExample.environmentVariables.get(), hasEntry('USER', 'tester'))
    }
}
