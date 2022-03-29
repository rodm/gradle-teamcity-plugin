/*
 * Copyright 2016 Rod MacKenzie
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

import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.RegularFileProperty;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for plugin configuration.
 */
public abstract class PluginConfiguration {

    private Object descriptor;

    private final RegularFileProperty descriptorFile;

    private final CopySpec files;

    private Map<String, Object> tokens = new LinkedHashMap<>();

    private String archiveName;

    private final Project project;

    protected PluginConfiguration(Project project) {
        this.project = project;
        this.descriptorFile = project.getObjects().fileProperty();
        this.files = project.copySpec();
    }

    public void setDescriptor(Object descriptor) {
        if (descriptor instanceof CharSequence) {
            setDescriptorFile(project.file(descriptor.toString()));
        } else if (descriptor instanceof File) {
            setDescriptorFile((File) descriptor);
        } else {
            setDescriptorObject(descriptor);
        }
    }

    public Object getDescriptor() {
        return descriptor;
    }

    public RegularFileProperty getDescriptorFile() {
        return descriptorFile;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public String getArchiveName() {
        return archiveName;
    }

    /**
     * Adds additional files to the TeamCity plugin archive.
     *
     * <p>The given action is executed to configure a {@code CopySpec}.</p>
     *
     * @param configuration The action.
     * @return The created {@code CopySpec}
     */
    public CopySpec files(Action<? super CopySpec> configuration) {
        CopySpec copySpec = project.copySpec(configuration);
        files.with(copySpec);
        return copySpec;
    }

    public CopySpec getFiles() {
        return files;
    }

    public Map<String, Object> getTokens() {
        return tokens;
    }

    public void setTokens(Map<String, Object> tokens) {
        this.tokens = tokens;
    }

    /**
     * Adds tokens and values that are to be replaced in an external descriptor file.
     *
     * @param tokens The map of tokens.
     */
    public void tokens(Map<String, Object> tokens) {
        this.tokens.putAll(tokens);
    }

    public Project getProject() {
        return this.project;
    }

    private void setDescriptorFile(File file) {
        if (this.descriptor != null) {
            throw new InvalidUserDataException("An inline descriptor is already defined");
        }
        this.descriptorFile.set(file);
    }

    private void setDescriptorObject(Object descriptor) {
        if (this.descriptorFile.isPresent()) {
            throw new InvalidUserDataException("A file descriptor is already defined");
        }
        this.descriptor = descriptor;
    }
}
