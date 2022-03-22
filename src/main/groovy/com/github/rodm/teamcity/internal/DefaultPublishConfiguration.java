/*
 * Copyright 2021 the original author or authors.
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
package com.github.rodm.teamcity.internal;

import com.github.rodm.teamcity.PublishConfiguration;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.util.Collections;
import java.util.List;

/**
 * Configuration for publishing a plugin to a plugin repository.
 */
public class DefaultPublishConfiguration implements PublishConfiguration {

    private static final List<String> DEFAULT_CHANNELS = Collections.singletonList("default");

    private final ListProperty<String> channels;
    private final Property<String> token;
    private final Property<String> notes;

    public DefaultPublishConfiguration(Project project) {
        this.channels = project.getObjects().listProperty(String.class).convention(DEFAULT_CHANNELS);
        this.token = project.getObjects().property(String.class);
        this.notes = project.getObjects().property(String.class);
    }

    /**
     * The list of channel names that the plugin will be published to on the plugin repository
     */
    public List<String> getChannels() {
        return channels.get();
    }

    public void setChannels(List<String> channels) {
        this.channels.set(channels);
    }

    public ListProperty<String> getChannelsProperty() {
        return channels;
    }

    /**
     * The token for uploading the plugin to the plugin repository
     */
    public String getToken() {
        return token.get();
    }

    public void setToken(String token) {
        this.token.set(token);
    }

    public Provider<String> getTokenProperty() {
        return token;
    }

    /**
     * The notes describing the changes made to the plugin
     */
    public String getNotes() {
        return notes.get();
    }

    public void setNotes(String notes) {
        this.notes.set(notes);
    }

    public Provider<String> getNotesProperty() {
        return notes;
    }
}
