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
package com.github.rodm.teamcity.tasks

import com.github.rodm.teamcity.TeamCityVersion
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile

class TeamCityTask extends DefaultTask {

    static final String VERSION_MISMATCH_WARNING = '%s: Environment version %s does not match the version of TeamCity installed at %s.'
    static final String VERSION_INCOMPATIBLE = 'Environment version %s is not compatible with the version of TeamCity installed at %s.'
    static final String INVALID_HOME_DIR = 'Invalid TeamCity installation at %s.'
    static final String MISSING_VERSION = 'Unable to read version of TeamCity installation at %s'

    @Input
    final Property<String> version = project.objects.property(String)

    @Input
    final Property<String> homeDir = project.objects.property(String)

    @Input
    final Property<String> javaHome = project.objects.property(String)

    void validate() {
        validTeamCityHomeDirectory(getVersion().get(), getHomeDir().get())
        validDirectory('javaHome', getJavaHome().get())
    }

    void validTeamCityHomeDirectory(String version, String homeDir) {
        validDirectory('homeDir', homeDir)
        def installationVersion = getTeamCityVersion(homeDir)
        if (TeamCityVersion.version(installationVersion) == TeamCityVersion.version(version)) {
            return
        }
        if (TeamCityVersion.version(installationVersion).dataVersion == TeamCityVersion.version(version).dataVersion) {
            logger.warn(String.format(VERSION_MISMATCH_WARNING, path, version, homeDir))
            return
        }
        throw new InvalidUserDataException(String.format(VERSION_INCOMPATIBLE, version, homeDir))
    }

    static void validDirectory(String propertyName, String value) {
        if (value == null) {
            throw new InvalidUserDataException(String.format("Property '%s' not set.", propertyName))
        }
        File path = new File(value)
        if (!path.exists()) {
            throw new InvalidUserDataException(String.format("Directory '%s' specified for property '%s' does not exist.", value, propertyName))
        } else if (!path.isDirectory()) {
            throw new InvalidUserDataException(String.format("Directory '%s' specified for property '%s' is not a directory.", value, propertyName))
        }
    }

    static boolean isWindows() {
        def os = System.getProperty("os.name").toLowerCase()
        return os.contains("windows")
    }

    private static String getTeamCityVersion(String homeDir) {
        Path jarPath = Paths.get(homeDir, 'webapps/ROOT/WEB-INF/lib/common-api.jar')
        if (!Files.isRegularFile(jarPath)) {
            throw new InvalidUserDataException(String.format(INVALID_HOME_DIR, homeDir))
        }

        JarFile jarFile = new JarFile(jarPath.toFile())
        def entry = jarFile.getEntry("serverVersion.properties.xml")
        if (entry == null) {
            throw new InvalidUserDataException(String.format(MISSING_VERSION, homeDir))
        }

        def is = jarFile.getInputStream(entry)
        try {
            def props = new Properties()
            props.loadFromXML(is)
            return props['Display_Version']
        }
        finally {
            is.close()
        }
    }
}
