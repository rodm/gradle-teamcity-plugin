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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Test
import org.xmlunit.matchers.EvaluateXPathMatcher
import org.xmlunit.matchers.HasXPathMatcher

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.not

public class ServerDescriptorGeneratorTest {

    private Project project;

    private static HasXPathMatcher hasXPath(String xPath) {
        return HasXPathMatcher.hasXPath(xPath)
    }

    private static EvaluateXPathMatcher hasXPath(String xPath, Matcher<String> valueMatcher) {
        return EvaluateXPathMatcher.hasXPath(xPath, valueMatcher)
    }

    @Before
    public void setup() {
        project = ProjectBuilder.builder()
                .withName('test-plugin')
                .build()
        project.apply plugin: 'com.github.rodm.teamcity-server'
    }

    @Test
    public void writesTeamCityPluginRootNode() {
        StringWriter writer = new StringWriter();
        ServerPluginDescriptor descriptor = new ServerPluginDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)

        generator.writeTo(writer);

        assertThat(writer.toString(), hasXPath("/teamcity-plugin"))
    }

    @Test
    public void writesRequiredInfoProperties() {
        project.teamcity {
            descriptor {
                name = 'plugin name'
                displayName = 'display name'
                version = '1.2.3'
                vendorName = 'vendor name'
            }
        }
        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer);

        assertThat(writer.toString(), hasXPath('//info/name', equalTo('plugin name')))
        assertThat(writer.toString(), hasXPath('//info/display-name', equalTo('display name')))
        assertThat(writer.toString(), hasXPath('//info/version', equalTo('1.2.3')))
        assertThat(writer.toString(), hasXPath('//info/vendor/name', equalTo('vendor name')))
    }

    @Test
    public void writesRequiredInfoPropertiesWhenNotSet() {
        ServerPluginDescriptor descriptor = new ServerPluginDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//info/name'))
        assertThat(writer.toString(), hasXPath('//info/display-name'))
        assertThat(writer.toString(), hasXPath('//info/version'))
        assertThat(writer.toString(), hasXPath('//info/vendor/name'))
    }

    @Test
    public void writesRequiredInfoPropertiesUsingDefaults() {
        def defaults = [:]
        defaults << ['name': 'test-plugin name']
        defaults << ['displayName': 'test-plugin display name']
        defaults << ['version': '1.2.3']
        ServerPluginDescriptor descriptor = new ServerPluginDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor, "9.0", defaults)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//info/name', equalTo('test-plugin name')))
        assertThat(writer.toString(), hasXPath('//info/display-name', equalTo('test-plugin display name')))
        assertThat(writer.toString(), hasXPath('//info/version', equalTo('1.2.3')))
    }

    @Test
    public void writesOptionalInfoProperties() {
        project.teamcity {
            descriptor {
                description = 'plugin description'
                downloadUrl = 'download url'
                email = 'email'
                vendorUrl = 'vendor url'
                vendorLogo = 'vendor logo'
            }
        }
        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//info/description', equalTo('plugin description')))
        assertThat(writer.toString(), hasXPath('//info/download-url', equalTo('download url')))
        assertThat(writer.toString(), hasXPath('//info/email', equalTo('email')))
        assertThat(writer.toString(), hasXPath('//info/vendor/url', equalTo('vendor url')))
        assertThat(writer.toString(), hasXPath('//info/vendor/logo', equalTo('vendor logo')))
    }

    @Test
    public void optionalInfoPropertiesNotWrittenWhenNotSet() {
        project.teamcity {
            descriptor {
            }
        }
        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), not(hasXPath('//info/description')))
        assertThat(writer.toString(), not(hasXPath('//info/download-url')))
        assertThat(writer.toString(), not(hasXPath('//info/email')))
        assertThat(writer.toString(), not(hasXPath('//info/vendor/url')))
        assertThat(writer.toString(), not(hasXPath('//info/vendor/logo')))
    }

    @Test
    public void writeOptionalSeparateClassloader() {
        project.teamcity {
            descriptor {
                useSeparateClassloader = true
            }
        }
        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//deployment/@use-separate-classloader', equalTo('true')))
    }

    @Test
    public void writeOptionalSeparateClassloaderWhenFalse() {
        project.teamcity {
            descriptor {
                useSeparateClassloader = false
            }
        }

        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//deployment/@use-separate-classloader', equalTo('false')))
    }

    @Test
    public void writeOptionalSettingsOnlyIfSpecified() {
        project.teamcity {
            descriptor {
            }
        }
        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), not(hasXPath('//deployment')))
        assertThat(writer.toString(), not(hasXPath('//parameters')))
    }

    @Test
    public void writeParameters() {
        project.teamcity {
            descriptor {
                parameters {
                    parameter 'name1', 'value1'
                    parameters name2: 'value2', name3: 'value3'
                }
            }
        }
        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath("//parameters/parameter[@name='name1']", equalTo('value1')))
        assertThat(writer.toString(), hasXPath("//parameters/parameter[@name='name2']", equalTo('value2')))
        assertThat(writer.toString(), hasXPath("//parameters/parameter[@name='name3']", equalTo('value3')))
    }

    @Test
    public void writePluginDependency() {
        project.teamcity {
            descriptor {
                dependencies {
                    plugin 'plugin-name'
                }
            }
        }
        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//dependencies/plugin/@name', equalTo('plugin-name')))
    }

    @Test
    public void writeToolDependency() {
        project.teamcity {
            descriptor {
                dependencies {
                    tool 'tool-name'
                }
            }
        }
        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//dependencies/tool/@name', equalTo('tool-name')))
    }

    @Test
    public void writeDependenciesOnlyIfSpecified() {
        project.teamcity {
            descriptor {
                dependencies {
                }
            }
        }
        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), not(hasXPath('//dependencies')))
    }

    @Test
    public void writeDependenciesOnlyForTeamCity9() {
        project.teamcity {
            version = '8.1'
            descriptor {
                dependencies {
                    plugin 'plugin-name'
                }
            }
        }
        TeamCityPluginExtension extension = project.getExtensions().getByType(TeamCityPluginExtension)
        ServerPluginDescriptor descriptor = extension.getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor, extension.getVersion())
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), not(hasXPath('//dependencies')))
    }

    @Test
    public void writeRequirements() {
        project.teamcity {
            descriptor {
                minimumBuild = '1234'
                maximumBuild = '2345'
            }
        }
        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//requirements/@min-build', equalTo('1234')))
        assertThat(writer.toString(), hasXPath('//requirements/@max-build', equalTo('2345')))
    }

    @Test
    public void writeRequirementsMinimumBuildOnly() {
        project.teamcity {
            descriptor {
                minimumBuild = '1234'
            }
        }
        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//requirements/@min-build', equalTo('1234')))
        assertThat(writer.toString(), not(hasXPath('//requirements/@max-build')))
    }

    @Test
    public void writeRequirementsMaximumBuildOnly() {
        project.teamcity {
            descriptor {
                maximumBuild = '1234'
            }
        }
        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), hasXPath('//requirements/@max-build', equalTo('1234')))
        assertThat(writer.toString(), not(hasXPath('//requirements/@min-build')))
    }

    @Test
    public void writeRequirementsOnlyIfSpecified() {
        project.teamcity {
            descriptor {
            }
        }
        ServerPluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertThat(writer.toString(), not(hasXPath('//requirements')))
    }
}
