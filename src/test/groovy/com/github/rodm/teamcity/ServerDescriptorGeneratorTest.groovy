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
import org.junit.Before
import org.junit.Test

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists

public class ServerDescriptorGeneratorTest {

    private Project project;

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

        assertXpathExists("/teamcity-plugin", writer.toString());
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

        assertXpathEvaluatesTo("plugin name", "//info/name", writer.toString());
        assertXpathEvaluatesTo("display name", "//info/display-name", writer.toString());
        assertXpathEvaluatesTo("1.2.3", "//info/version", writer.toString());
        assertXpathEvaluatesTo("vendor name", "//info/vendor/name", writer.toString());
    }

    @Test
    public void writesRequiredInfoPropertiesWhenNotSet() {
        ServerPluginDescriptor descriptor = new ServerPluginDescriptor()
        ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertXpathExists("//info/name", writer.toString());
        assertXpathExists("//info/display-name", writer.toString());
        assertXpathExists("//info/version", writer.toString());
        assertXpathExists("//info/vendor/name", writer.toString());
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

        assertXpathEvaluatesTo("test-plugin name", "//info/name", writer.toString());
        assertXpathEvaluatesTo("test-plugin display name", "//info/display-name", writer.toString());
        assertXpathEvaluatesTo("1.2.3", "//info/version", writer.toString());
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

        assertXpathEvaluatesTo("plugin description", "//info/description", writer.toString());
        assertXpathEvaluatesTo("download url", "//info/download-url", writer.toString());
        assertXpathEvaluatesTo("email", "//info/email", writer.toString());
        assertXpathEvaluatesTo("vendor url", "//info/vendor/url", writer.toString());
        assertXpathEvaluatesTo("vendor logo", "//info/vendor/logo", writer.toString());
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

        assertXpathNotExists("//info/description", writer.toString());
        assertXpathNotExists("//info/download-url", writer.toString());
        assertXpathNotExists("//info/email", writer.toString());
        assertXpathNotExists("//info/vendor/url", writer.toString());
        assertXpathNotExists("//info/vendor/logo", writer.toString());
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

        assertXpathEvaluatesTo("true", "//deployment/@use-separate-classloader", writer.toString());
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

        assertXpathEvaluatesTo("false", "//deployment/@use-separate-classloader", writer.toString());
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

        assertXpathNotExists("//deployment", writer.toString());
        assertXpathNotExists("//parameters", writer.toString());
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

        assertXpathEvaluatesTo("value1", "//parameters/parameter[@name='name1']", writer.toString());
        assertXpathEvaluatesTo("value2", "//parameters/parameter[@name='name2']", writer.toString());
        assertXpathEvaluatesTo("value3", "//parameters/parameter[@name='name3']", writer.toString());
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

        assertXpathEvaluatesTo("plugin-name", "//dependencies/plugin/@name", writer.toString());
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

        assertXpathEvaluatesTo("tool-name", "//dependencies/tool/@name", writer.toString());
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

        assertXpathNotExists("//dependencies", writer.toString());
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

        assertXpathNotExists("//dependencies", writer.toString());
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

        assertXpathEvaluatesTo("1234", "//requirements/@min-build", writer.toString());
        assertXpathEvaluatesTo("2345", "//requirements/@max-build", writer.toString());
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

        assertXpathEvaluatesTo("1234", "//requirements/@min-build", writer.toString());
        assertXpathNotExists("//requirements/@max-build", writer.toString());
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

        assertXpathEvaluatesTo("1234", "//requirements/@max-build", writer.toString());
        assertXpathNotExists("//requirements/@min-build", writer.toString());
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

        assertXpathNotExists("//requirements", writer.toString());
    }
}
