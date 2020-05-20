/*
 * Copyright 2015 Rod MacKenzie
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
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import javax.xml.XMLConstants

import static XPathMatcher.hasXPath
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.not

class ServerDescriptorGeneratorTest {

    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder()

    private Project project

    private StringWriter writer

    private TeamCityPluginExtension extension

    private ServerPluginDescriptor createDescriptor() {
        ServerPluginDescriptor descriptor = project.extensions.create('descriptor', ServerPluginDescriptor, project)
        descriptor.init()
        return descriptor
    }

    private ServerPluginDescriptorGenerator createGenerator() {
        ServerPluginDescriptor descriptor = extension.server.getDescriptor()
        createGenerator(descriptor)
    }

    private ServerPluginDescriptorGenerator createGenerator(ServerPluginDescriptor descriptor) {
        new ServerPluginDescriptorGenerator(descriptor, extension.getVersion())
    }

    @Before
    void setup() {
        project = ProjectBuilder.builder()
                .withProjectDir(projectDir.root)
                .withName('test-plugin')
                .build()
        project.apply plugin: 'com.github.rodm.teamcity-server'
        writer = new StringWriter()
        extension = project.getExtensions().getByType(TeamCityPluginExtension)
    }

    @Test
    void writesTeamCityPluginRootNode() {
        ServerPluginDescriptor descriptor = createDescriptor()
        ServerPluginDescriptorGenerator generator = createGenerator(descriptor)

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath("/teamcity-plugin"))
    }

    @Test
    void 'descriptor schema location'() {
        ServerPluginDescriptor descriptor = createDescriptor()
        ServerPluginDescriptorGenerator generator = createGenerator(descriptor)

        generator.writeTo(writer)

        def path = '/teamcity-plugin/@xsi:noNamespaceSchemaLocation'
        def schemaLocation = 'urn:schemas-jetbrains-com:teamcity-plugin-v1-xml'
        def context = ['xsi': XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI]
        def matcher = hasXPath(path, equalTo(schemaLocation)).withNamespaceContext(context)
        assertThat(writer.toString(), matcher)
    }

    @Test
    void writesRequiredInfoProperties() {
        project.teamcity {
            server {
                descriptor {
                    name = 'plugin name'
                    displayName = 'display name'
                    version = '1.2.3'
                    vendorName = 'vendor name'
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//info/name', equalTo('plugin name')))
        assertThat(writer.toString(), hasXPath('//info/display-name', equalTo('display name')))
        assertThat(writer.toString(), hasXPath('//info/version', equalTo('1.2.3')))
        assertThat(writer.toString(), hasXPath('//info/vendor/name', equalTo('vendor name')))
    }

    @Test
    void writesRequiredInfoPropertiesWhenNotSet() {
        ServerPluginDescriptor descriptor = createDescriptor()
        ServerPluginDescriptorGenerator generator = createGenerator(descriptor)

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//info/name'))
        assertThat(writer.toString(), hasXPath('//info/display-name'))
        assertThat(writer.toString(), hasXPath('//info/version'))
        assertThat(writer.toString(), hasXPath('//info/vendor/name'))
    }

    @Test
    void writesRequiredInfoPropertiesUsingDefaults() {
        project.version = '1.2.3'
        ServerPluginDescriptor descriptor = createDescriptor()
        ServerPluginDescriptorGenerator generator = createGenerator(descriptor)

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//info/name', equalTo('test-plugin')))
        assertThat(writer.toString(), hasXPath('//info/display-name', equalTo('test-plugin')))
        assertThat(writer.toString(), hasXPath('//info/version', equalTo('1.2.3')))
    }

    @Test
    void writesOptionalInfoProperties() {
        project.teamcity {
            server {
                descriptor {
                    description = 'plugin description'
                    downloadUrl = 'download url'
                    email = 'email'
                    vendorUrl = 'vendor url'
                    vendorLogo = 'vendor logo'
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//info/description', equalTo('plugin description')))
        assertThat(writer.toString(), hasXPath('//info/download-url', equalTo('download url')))
        assertThat(writer.toString(), hasXPath('//info/email', equalTo('email')))
        assertThat(writer.toString(), hasXPath('//info/vendor/url', equalTo('vendor url')))
        assertThat(writer.toString(), hasXPath('//info/vendor/logo', equalTo('vendor logo')))
    }

    @Test
    void optionalInfoPropertiesNotWrittenWhenNotSet() {
        project.teamcity {
            server {
                descriptor {
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), not(hasXPath('//info/description')))
        assertThat(writer.toString(), not(hasXPath('//info/download-url')))
        assertThat(writer.toString(), not(hasXPath('//info/email')))
        assertThat(writer.toString(), not(hasXPath('//info/vendor/url')))
        assertThat(writer.toString(), not(hasXPath('//info/vendor/logo')))
    }

    @Test
    void writeOptionalSeparateClassloader() {
        project.teamcity {
            server {
                descriptor {
                    useSeparateClassloader = true
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//deployment/@use-separate-classloader', equalTo('true')))
    }

    @Test
    void writeOptionalSeparateClassloaderWhenFalse() {
        project.teamcity {
            server {
                descriptor {
                    useSeparateClassloader = false
                }
            }
        }

        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//deployment/@use-separate-classloader', equalTo('false')))
    }

    @Test
    void writeOptionalSettingsOnlyIfSpecified() {
        project.teamcity {
            server {
                descriptor {
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), not(hasXPath('//deployment')))
        assertThat(writer.toString(), not(hasXPath('//parameters')))
    }

    @Test
    void 'writes optional allow-runtime-reload when set'() {
        project.teamcity {
            version = '2018.2'
            server {
                descriptor {
                    allowRuntimeReload = true
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//deployment/@allow-runtime-reload', equalTo('true')))
    }

    @Test
    void 'writes optional allow-runtime-reload when set to false'() {
        project.teamcity {
            version = '2018.2'
            server {
                descriptor {
                    allowRuntimeReload = false
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//deployment/@allow-runtime-reload', equalTo('false')))
    }

    @Test
    void 'writes optional node-responsibilities-aware when set'() {
        project.teamcity {
            version = '2020.1'
            server {
                descriptor {
                    nodeResponsibilitiesAware = true
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//deployment/@node-responsibilities-aware', equalTo('true')))
    }

    @Test
    void 'writes optional node-responsibilities-aware when set to false'() {
        project.teamcity {
            version = '2020.1'
            server {
                descriptor {
                    nodeResponsibilitiesAware = false
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//deployment/@node-responsibilities-aware', equalTo('false')))
    }

    @Test
    void writeParameters() {
        project.teamcity {
            server {
                descriptor {
                    parameters {
                        parameter 'name1', 'value1'
                        parameters name2: 'value2', name3: 'value3'
                    }
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath("//parameters/parameter[@name='name1']", equalTo('value1')))
        assertThat(writer.toString(), hasXPath("//parameters/parameter[@name='name2']", equalTo('value2')))
        assertThat(writer.toString(), hasXPath("//parameters/parameter[@name='name3']", equalTo('value3')))
    }

    @Test
    void writePluginDependency() {
        project.teamcity {
            server {
                descriptor {
                    dependencies {
                        plugin 'plugin-name'
                    }
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//dependencies/plugin/@name', equalTo('plugin-name')))
    }

    @Test
    void writeToolDependency() {
        project.teamcity {
            server {
                descriptor {
                    dependencies {
                        tool 'tool-name'
                    }
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//dependencies/tool/@name', equalTo('tool-name')))
    }

    @Test
    void writeDependenciesOnlyIfSpecified() {
        project.teamcity {
            server {
                descriptor {
                    dependencies {
                    }
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), not(hasXPath('//dependencies')))
    }

    @Test
    void writeDependenciesOnlyForTeamCity9() {
        project.teamcity {
            version = '8.1'
            server {
                descriptor {
                    dependencies {
                        plugin 'plugin-name'
                    }
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), not(hasXPath('//dependencies')))
    }

    @Test
    void 'write plugin dependencies for non-numeric TeamCity version'() {
        writePluginDependenciesForVersion('SNAPSHOT')
    }

    @Test
    void 'write plugin dependencies for TeamCity version 10'() {
        writePluginDependenciesForVersion('10.0')
    }

    private void writePluginDependenciesForVersion(String apiVersion) {
        project.teamcity {
            version = apiVersion
            server {
                descriptor {
                    dependencies {
                        plugin 'plugin-name'
                    }
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//dependencies'))
    }

    @Test
    void writeRequirements() {
        project.teamcity {
            server {
                descriptor {
                    minimumBuild = '1234'
                    maximumBuild = '2345'
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//requirements/@min-build', equalTo('1234')))
        assertThat(writer.toString(), hasXPath('//requirements/@max-build', equalTo('2345')))
    }

    @Test
    void writeRequirementsMinimumBuildOnly() {
        project.teamcity {
            server {
                descriptor {
                    minimumBuild = '1234'
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//requirements/@min-build', equalTo('1234')))
        assertThat(writer.toString(), not(hasXPath('//requirements/@max-build')))
    }

    @Test
    void writeRequirementsMaximumBuildOnly() {
        project.teamcity {
            server {
                descriptor {
                    maximumBuild = '1234'
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//requirements/@max-build', equalTo('1234')))
        assertThat(writer.toString(), not(hasXPath('//requirements/@min-build')))
    }

    @Test
    void writeRequirementsOnlyIfSpecified() {
        project.teamcity {
            server {
                descriptor {
                }
            }
        }
        ServerPluginDescriptorGenerator generator = createGenerator()

        generator.writeTo(writer)

        assertThat(writer.toString(), not(hasXPath('//requirements')))
    }
}
