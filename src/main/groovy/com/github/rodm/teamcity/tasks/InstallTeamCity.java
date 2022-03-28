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
package com.github.rodm.teamcity.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public abstract class InstallTeamCity extends DefaultTask {

    public InstallTeamCity() {
        setDescription("Installs a TeamCity distribution");
    }

    @InputFile
    public abstract RegularFileProperty getSource();

    @OutputDirectory
    public abstract DirectoryProperty getTarget();

    @TaskAction
    public void install() {
        getLogger().info("Installing TeamCity from {} into {}", getSource().get(), getTarget().get());
        final String targetName = getTarget().get().getAsFile().getName();
        getProject().copy(copySpec -> {
            copySpec.from(getProject().tarTree(getSource().get()), copySpec1 -> {
                copySpec1.setIncludeEmptyDirs(false);
                copySpec1.eachFile(file -> file.setPath(targetName + "/" + getPath().split("/", 2)[1]));
            });
            copySpec.into(getTarget().get().getAsFile().getParentFile());
        });
    }
}
