/*
 * Copyright 2021 the original author or authors.
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

import com.github.rodm.teamcity.tasks.PublishTask
import com.github.rodm.teamcity.tasks.ServerPlugin
import com.github.rodm.teamcity.tasks.SignPluginTask
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.nullValue
import static org.hamcrest.CoreMatchers.notNullValue
import static org.hamcrest.MatcherAssert.assertThat

class ServerSignConfigurationTest {

    @TempDir
    public Path projectDir

    Project project

    TeamCityPluginExtension extension

    @BeforeEach
    void setup() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
        project.apply plugin: 'com.github.rodm.teamcity-server'
        extension = project.getExtensions().getByType(TeamCityPluginExtension)
    }

    @Test
    void 'sign task is only created with sign configuration'() {
        project.teamcity {
            server {
            }
        }

        project.evaluate()

        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        assertThat(signPlugin, is(nullValue()))
    }

    @Test
    void 'sign task is configured with a certificate file'() {
        File dummyCertificateFile = projectDir.resolve('ca.crt').toFile()
        dummyCertificateFile << ""
        project.teamcity {
            server {
                sign {
                    certificateFile = project.file('ca.crt')
                }
            }
        }

        project.evaluate()

        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        def certificateFile = signPlugin.certificateFile.get().asFile.canonicalFile
        assertThat(certificateFile, equalTo(dummyCertificateFile.canonicalFile))
    }

    @Test
    void 'sign task is configured with a password file and no password'() {
        File dummyPrivateKeyFile = projectDir.resolve('private-key-file').toFile()
        dummyPrivateKeyFile << ""
        project.teamcity {
            server {
                sign {
                    privateKeyFile = project.file('private-key-file')
                }
            }
        }

        project.evaluate()

        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        def privateKeyFile = signPlugin.privateKeyFile.get().asFile.canonicalFile
        assertThat(privateKeyFile, equalTo(dummyPrivateKeyFile.canonicalFile))
        assertThat(signPlugin.password.orNull, is(nullValue()))
    }

    @Test
    void 'sign task is configured with a password file and password'() {
        File dummyPrivateKeyFile = projectDir.resolve('private-key-file').toFile()
        dummyPrivateKeyFile << ""
        project.teamcity {
            server {
                sign {
                    privateKeyFile = project.file('private-key-file')
                    password = 'password'
                }
            }
        }

        project.evaluate()

        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        assertThat(signPlugin.privateKeyFile.get().asFile.canonicalFile, equalTo(dummyPrivateKeyFile.canonicalFile))
        assertThat(signPlugin.password.orNull, equalTo('password'))
    }

    @Test
    void 'sign task is configured with output of package task'() {
        project.teamcity {
            server {
                sign {
                }
            }
        }

        project.evaluate()

        ServerPlugin serverPlugin = (ServerPlugin) project.tasks.findByPath(':serverPlugin')
        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        assertThat(signPlugin.pluginFile.get(), equalTo(serverPlugin.archiveFile.get()))
    }

    @Test
    void 'signed plugin file name defaults to server plugin task output with -signed appended'() {
        project.teamcity {
            server {
                descriptor {
                    archiveName = 'test-plugin-name.zip'
                }
                sign {
                }
            }
        }

        project.evaluate()

        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        assertThat(signPlugin.signedPluginFile.get().asFile.name, equalTo('test-plugin-name-signed.zip'))
    }

    @Test
    void 'publish task is configured with output of sign task'() {
        project.teamcity {
            server {
                sign {
                }
                publish {
                }
            }
        }

        project.evaluate()

        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        PublishTask publishPlugin = (PublishTask) project.tasks.findByPath(':publishPlugin')
        assertThat(publishPlugin.distributionFile.get(), equalTo(signPlugin.signedPluginFile.get()))
    }
}
