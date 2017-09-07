/*
 * Copyright 2016 Rod MacKenzie
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

import org.gradle.api.file.CopySpec
import org.gradle.util.ConfigureUtil

/**
 * Base class for plugin configuration.
 */
abstract class PluginConfiguration {

    def descriptor

    private CopySpec files

    private Map<String, Object> tokens = [:]

    String archiveName

    PluginConfiguration(CopySpec copySpec) {
        this.files = copySpec
    }

    /**
     * Adds additional files to the TeamCity plugin archive.
     *
     * <p>The given closure is executed to configure a {@code CopySpec}.</p>
     *
     * @param closure The configuration closure.
     * @return The created {@code CopySpec}
     */
    def files(Closure closure) {
        ConfigureUtil.configure(closure, files.addChild())
    }

    CopySpec getFiles() {
        return files
    }

    Map<String, Object> getTokens() {
        return tokens
    }

    def setTokens(Map<String, Object> tokens) {
        this.tokens = tokens
    }

    /**
     * Adds tokens and values that are to be replaced in an external descriptor file.
     *
     * @param tokens The map of tokens.
     */
    def tokens(Map<String, Object> tokens) {
        this.tokens += tokens
    }
}
