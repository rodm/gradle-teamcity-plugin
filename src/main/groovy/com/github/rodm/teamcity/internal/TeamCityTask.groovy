/*
 * Copyright 2015 the original author or authors.
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
package com.github.rodm.teamcity.internal

import com.github.rodm.teamcity.TeamCityVersion
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile

abstract class TeamCityTask extends DefaultTask {

    static final String VERSION_MISMATCH_WARNING = '%s: Version %s does not match the TeamCity version %s installed at %s.'
    static final String VERSION_INCOMPATIBLE = 'Version %s is not compatible with the TeamCity version %s installed at %s.'
    static final String INVALID_HOME_DIR = 'Invalid TeamCity installation at %s.'
    static final String MISSING_VERSION = 'Unable to read version of TeamCity installation at %s'

    @Input
    final Property<String> version = project.objects.property(String)

    @Input
    final Property<String> homeDir = project.objects.property(String)

    @Input
    final Property<String> javaHome = project.objects.property(String)

    @TaskAction
    void exec() {
        validate()
        def out = new ByteArrayOutputStream()
        project.exec(new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec execSpec) {
                configure(execSpec)
                execSpec.standardOutput = out
                execSpec.errorOutput = out
                execSpec.ignoreExitValue = true
            }
        })
        logger.info(out.toString())
    }

    abstract void configure(ExecSpec execSpec)

    void validate() {
        validTeamCityHomeDirectory(getVersion().get(), getHomeDir().get())
        validDirectory('javaHome', getJavaHome().get())
    }

    void validTeamCityHomeDirectory(String version, String homeDir) {
        validDirectory('homeDir', homeDir)
        def installationVersion = getTeamCityVersion(homeDir)
        if (installationVersion == version) {
            return
        }
        if (dataVersion(extractVersion(installationVersion)) == dataVersion(version)) {
            logger.warn(String.format(VERSION_MISMATCH_WARNING, path, version, installationVersion, homeDir))
            return
        }
        throw new InvalidUserDataException(String.format(VERSION_INCOMPATIBLE, version, installationVersion, homeDir))
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

    private static String dataVersion(String version) {
        return TeamCityVersion.version(version).dataVersion
    }

    private static String extractVersion(String version) {
        if (version.contains('EAP') || version.contains('RC')) {
            return version.split(' ')[0]
        } else {
            return version
        }
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
            jarFile.close()
        }
    }
}
