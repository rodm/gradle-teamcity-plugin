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
package com.github.rodm.teamcity;

import org.gradle.api.tasks.Input;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TeamCity plugin parameters
 */
public class Parameters {

    private final Map<String, String> entries = new LinkedHashMap<>();

    @Input
    public Map<String, String> getParameters() {
        return entries;
    }

    /**
     * Add a map of plugin parameters.
     *
     * @param parameters The map of parameters to add.
     */
    public void parameters(Map<String, String> parameters) {
        this.entries.putAll(parameters);
    }

    /**
     * Add a plugin parameter.
     *
     * @param name  The name of the parameter
     * @param value The value of the parameter
     */
    public void parameter(String name, String value) {
        entries.put(name, value);
    }

    public boolean hasParameters() {
        return !entries.isEmpty();
    }
}
