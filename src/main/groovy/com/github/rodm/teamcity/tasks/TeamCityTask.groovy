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
package com.github.rodm.teamcity.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input

class TeamCityTask extends DefaultTask {

    @Input
    File homeDir

    @Input
    File javaHome

    TeamCityTask() {
        group 'TeamCity'
    }

    void validate() {
        validDirectory('homeDir', getHomeDir())
        validDirectory('javaHome', getJavaHome())
    }

    void validDirectory(String propertyName, File value) {
        if (value == null) {
            throw new InvalidUserDataException(String.format("Property '%s' not set.", propertyName));
        } else if (!value.exists()) {
            throw new InvalidUserDataException(String.format("Directory '%s' specified for property '%s' does not exist.", value, propertyName));
        } else if (!value.isDirectory()) {
            throw new InvalidUserDataException(String.format("Directory '%s' specified for property '%s' is not a directory.", value, propertyName));
        }
    }

    boolean isWindows() {
        def os = System.getProperty("os.name").toLowerCase()
        return os.contains("windows")
    }
}
