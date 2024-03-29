/*
 * Copyright 2017 Rod MacKenzie
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

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

class TestSupport {

    static File createFile(Path path) {
        Files.createFile(path).toFile()
    }

    static File createDirectory(Path path) {
        Files.createDirectories(path).toFile()
    }

    static String normalizePath(DirectoryProperty directoryProperty) {
        normalizePath(directoryProperty.get().asFile)
    }

    static String normalizePath(RegularFileProperty fileProperty) {
        normalizePath(fileProperty.get().asFile)
    }

    static String normalizePath(File path) {
        normalize(path.absolutePath)
    }

    static String normalize(String path) {
        path.replace('\\', '/')
    }

    static List<String> archiveEntries(Path path) {
        try (ZipFile archiveFile = new ZipFile(path.toFile())) {
            return archiveFile.entries().collect { it.name }
        }
    }
}
