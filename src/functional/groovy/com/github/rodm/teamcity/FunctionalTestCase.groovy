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

    BuildResult executeBuild(String... args = ['build']) {
        return executeBuild(testProjectDir.toFile(), args)
    }
}
