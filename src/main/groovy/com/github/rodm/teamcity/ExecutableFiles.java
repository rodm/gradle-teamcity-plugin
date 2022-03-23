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
package com.github.rodm.teamcity;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.tasks.Input;

import java.util.ArrayList;
import java.util.List;

/**
 * Executable files
 */
public class ExecutableFiles {

    private List<String> includes = new ArrayList<>();

    @Input
    public List<String> getIncludes() {
        return includes;
    }

    /**
     * Add a path to an executable.
     *
     * @param path The relative path of a file to be set as executable after unpacking.
     */
    public void include(String path) {
        includes.add(path);
    }

    public boolean hasFiles() {
        return includes.size() > 0;
    }
}
