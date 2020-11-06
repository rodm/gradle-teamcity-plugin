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
package com.github.rodm.teamcity.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

class TeamCityTask extends DefaultTask {

    @Input
    final Property<String> homeDir = project.objects.property(String)

    @Input
    final Property<String> javaHome = project.objects.property(String)

    void validate() {
        validDirectory('homeDir', getHomeDir().get())
        validDirectory('javaHome', getJavaHome().get())
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
}
