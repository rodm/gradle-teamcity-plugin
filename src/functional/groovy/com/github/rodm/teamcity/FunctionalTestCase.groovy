/*
 * Copyright 2021 Rod MacKenzie
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

import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

import static com.github.rodm.teamcity.TestSupport.executeBuild

class FunctionalTestCase {

    @TempDir
    public Path testProjectDir

    File buildFile
    File settingsFile

    @BeforeEach
    void init() throws IOException {
        buildFile = createFile("build.gradle")
        settingsFile = createFile('settings.gradle')
    }

    File createFile(String name) {
        createFile(testProjectDir, name)
    }

    File createDirectory(String name) {
        createDirectory(testProjectDir, name)
    }

    static File createFile(Path folder, String name) {
        Files.createFile(folder.resolve(name)).toFile()
    }

    static File createDirectory(Path folder, String name) {
        Files.createDirectories(folder.resolve(name)).toFile()
    }

    static File createFile(Path folder, String name, String contents) {
        File file = createFile(folder, name)
        file << contents
        return file
    }

    BuildResult executeBuild(String... args = ['build']) {
        return executeBuild(testProjectDir.toFile(), args)
    }

    List<String> archiveEntries(String path) {
        ZipFile archiveFile = new ZipFile(testProjectDir.resolve(path).toFile())
        List<String> agentEntries = archiveFile.entries().collect { it.name }
        archiveFile.close()
        return agentEntries
    }

    File createFakeTeamCityInstall(String baseDir, String version) {
        createFakeTeamCityInstall(testProjectDir, baseDir, version)
    }

    private static final String SERVER_SHELL_FILE = """
        #!/bin/bash
        echo "Fake TeamCity startup script"
    """
    private static final String SERVER_BATCH_FILE = """
        @echo off
        echo "Fake TeamCity startup script"
    """
    private static final String AGENT_SHELL_FILE = """
        #!/bin/bash
        echo "Fake TeamCity Agent startup script"
    """
    private static final String AGENT_BATCH_FILE = """
        @echo off
        echo "Fake TeamCity Agent startup script"
    """

    static File createFakeTeamCityInstall(Path folder, String baseDir, String version) {
        File homeDir = createDirectory(folder, "${baseDir}/TeamCity-${version}".toString())
        createCommonApiJar(homeDir.toPath(), version)

        Path binDir = createDirectory(homeDir.toPath(), 'bin').toPath()
        File serverShellFile = createFile(binDir, 'teamcity-server.sh', SERVER_SHELL_FILE)
        serverShellFile.executable = true
        createFile(binDir, 'teamcity-server.bat', SERVER_BATCH_FILE)

        Path agentBinDir = createDirectory(homeDir.toPath(), 'buildAgent/bin').toPath()
        File agentShellFile = createFile(agentBinDir, 'agent.sh', AGENT_SHELL_FILE)
        agentShellFile.executable = true

        createFile(agentBinDir, 'agent.bat', AGENT_BATCH_FILE)
        return homeDir
    }

    private static void createCommonApiJar(Path folder, String version) {
        Path jarPath = folder.resolve('webapps/ROOT/WEB-INF/lib/common-api.jar')
        Files.createDirectories(jarPath.parent)

        Properties props = new Properties()
        props.put('Display_Version', version)
        FileOutputStream fos = new FileOutputStream(jarPath.toFile())
        ZipOutputStream zos = new ZipOutputStream(fos)
        ZipEntry ze = new ZipEntry('serverVersion.properties.xml')
        zos.putNextEntry(ze)
        props.storeToXML(zos, null)
        zos.closeEntry()
        zos.close()
    }
}
