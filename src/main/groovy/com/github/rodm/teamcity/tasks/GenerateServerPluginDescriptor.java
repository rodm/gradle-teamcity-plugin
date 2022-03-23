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

import com.github.rodm.teamcity.ServerPluginDescriptor;
import com.github.rodm.teamcity.ServerPluginDescriptorGenerator;
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

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2018_2;
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_2020_1;
import static com.github.rodm.teamcity.TeamCityVersion.VERSION_9_0;

@CacheableTask
public class GenerateServerPluginDescriptor extends DefaultTask {

    @Input
    private final Property<TeamCityVersion> version = getProject().getObjects().property(TeamCityVersion.class);

    @Nested
    private final Property<ServerPluginDescriptor> descriptor = getProject().getObjects().property(ServerPluginDescriptor.class);

    @OutputFile
    private final RegularFileProperty destination = getProject().getObjects().fileProperty();

    public GenerateServerPluginDescriptor() {
        setDescription("Generates the Server-side plugin descriptor");
        onlyIf(task -> getDescriptor().isPresent());
    }

    @TaskAction
    public void generateDescriptor() {
        if (version.get().compareTo(VERSION_9_0) < 0 && descriptor.get().getDependencies().hasDependencies()) {
            getLogger().warn(getPath() + ": Plugin descriptor does not support dependencies for version " + getVersion().get());
        }

        if (version.get().compareTo(VERSION_2018_2) < 0 && descriptor.get().getAllowRuntimeReload() != null) {
            getLogger().warn(getPath() + ": Plugin descriptor does not support allowRuntimeReload for version " + getVersion().get());
        }

        if (version.get().compareTo(VERSION_2020_1) < 0 && descriptor.get().getNodeResponsibilitiesAware() != null) {
            getLogger().warn(getPath() + ": Plugin descriptor does not support nodeResponsibilitiesAware for version " + getVersion().get());
        }

        final ServerPluginDescriptorGenerator generator = new ServerPluginDescriptorGenerator(descriptor.get(), version.get());
        try {
            Writer writer = Files.newBufferedWriter(getDestination().get().getAsFile().toPath(), StandardCharsets.UTF_8);
            generator.writeTo(writer);
        }
        catch (IOException e) {
            throw new GradleException("Failure writing descriptor", e);
        }
    }

    public final Property<TeamCityVersion> getVersion() {
        return version;
    }

    public final Property<ServerPluginDescriptor> getDescriptor() {
        return descriptor;
    }

    public final RegularFileProperty getDestination() {
        return destination;
    }
}
