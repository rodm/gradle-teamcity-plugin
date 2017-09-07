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

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Input

/**
 * Plugin dependencies
 */
@CompileStatic
class Dependencies {

    List<String> plugins = []

    @Input
    List<String> getPlugins() {
        return plugins
    }

    /**
     * Add a plugin as a dependency.
     *
     * @param name The name of the plugin to add as a dependency.
     */
    void plugin(String name) {
        plugins << name
    }

    List<String> tools = []

    @Input
    List<String> getTools() {
        return tools
    }

    /**
     * Add a tool as a dependency.
     *
     * @param name The name of the tool to add as a dependency.
     */
    void tool(String name) {
        tools << name
    }

    boolean hasDependencies() {
        return plugins.size() > 0 || tools.size() > 0
    }
}
