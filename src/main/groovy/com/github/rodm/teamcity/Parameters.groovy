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
 * TeamCity plugin parameters
 */
@CompileStatic
class Parameters {
    Map<String, Object> parameters = [:]

    @Input
    Map<String, Object> getParameters() {
        return parameters
    }

    /**
     * Add a plugin parameter.
     *
     * @param name The name of the parameter
     * @param value The value of the parameter
     */
    void parameter(String name, Object value) {
        parameters[name] = value
    }

    /**
     * Add a map of plugin parameters.
     *
     * @param parameters The map of parameters to add.
     */
    void parameters(Map<String, ?> parameters) {
        this.parameters.putAll(parameters)
    }
}
