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

public class PluginDescriptorGeneratorTest {

    private Project project;

    @Before
    public void setup() {
        project = ProjectBuilder.builder()
                .withName('test-plugin')
                .build()
        project.apply plugin: 'com.github.rodm.teamcity'
    }

    @Test
    public void writesTeamCityPluginRootNode() {
        StringWriter writer = new StringWriter();
        PluginDescriptor descriptor = new PluginDescriptor()
        PluginDescriptorGenerator generator = new PluginDescriptorGenerator(descriptor)

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
        PluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        PluginDescriptorGenerator generator = new PluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer);

        assertXpathEvaluatesTo("plugin name", "//info/name", writer.toString());
        assertXpathEvaluatesTo("display name", "//info/display-name", writer.toString());
        assertXpathEvaluatesTo("1.2.3", "//info/version", writer.toString());
        assertXpathEvaluatesTo("vendor name", "//info/vendor/name", writer.toString());
    }

    @Test
    public void writesRequiredInfoPropertiesWhenNotSet() {
        project.teamcity {
            descriptor {
            }
        }
        PluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        PluginDescriptorGenerator generator = new PluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertXpathExists("//info/name", writer.toString());
        assertXpathExists("//info/display-name", writer.toString());
        assertXpathExists("//info/version", writer.toString());
        assertXpathExists("//info/vendor/name", writer.toString());
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
        PluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        PluginDescriptorGenerator generator = new PluginDescriptorGenerator(descriptor)
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
        PluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        PluginDescriptorGenerator generator = new PluginDescriptorGenerator(descriptor)
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
        PluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        PluginDescriptorGenerator generator = new PluginDescriptorGenerator(descriptor)
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

        PluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        PluginDescriptorGenerator generator = new PluginDescriptorGenerator(descriptor)
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
        PluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        PluginDescriptorGenerator generator = new PluginDescriptorGenerator(descriptor)
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
                    parameter 'name2', 'value2'
                }
            }
        }
        PluginDescriptor descriptor = project.getExtensions().getByType(TeamCityPluginExtension).getDescriptor()
        PluginDescriptorGenerator generator = new PluginDescriptorGenerator(descriptor)
        StringWriter writer = new StringWriter();

        generator.writeTo(writer)

        assertXpathEvaluatesTo("value1", "//parameters/parameter[@name='name1']", writer.toString());
        assertXpathEvaluatesTo("value2", "//parameters/parameter[@name='name2']", writer.toString());
    }
}
