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

import com.github.rodm.teamcity.AgentPluginDescriptor;
import com.github.rodm.teamcity.AgentPluginDescriptorGenerator;
import com.github.rodm.teamcity.TeamCityVersion;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.github.rodm.teamcity.TeamCityVersion.VERSION_9_0;

@CacheableTask
public abstract class GenerateAgentPluginDescriptor extends DefaultTask {

    public GenerateAgentPluginDescriptor() {
        setDescription("Generates the Agent-side plugin descriptor");
        onlyIf(task -> getDescriptor().isPresent());
    }

    @Input
    public abstract Property<TeamCityVersion> getVersion();

    @Nested
    public abstract Property<AgentPluginDescriptor> getDescriptor();

    @OutputFile
    public abstract RegularFileProperty getDestination();

    @TaskAction
    public void generateDescriptor() {
        final TeamCityVersion version = getVersion().get();
        final AgentPluginDescriptor descriptor = getDescriptor().get();
        if (version.compareTo(VERSION_9_0) < 0 && descriptor.getDependencies().hasDependencies()) {
            getLogger().warn(getPath() + ": Plugin descriptor does not support dependencies for version " + version);
        }

        final AgentPluginDescriptorGenerator generator = new AgentPluginDescriptorGenerator(descriptor);
        final File destinationFile = getDestination().get().getAsFile();
        try (Writer writer = Files.newBufferedWriter(destinationFile.toPath(), StandardCharsets.UTF_8)) {
            generator.writeTo(writer);
        }
        catch (IOException e) {
            throw new GradleException("Failure writing descriptor", e);
        }
    }
}
