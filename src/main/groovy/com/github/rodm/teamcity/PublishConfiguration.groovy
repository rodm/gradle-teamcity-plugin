/*
 * Copyright 2018 Rod MacKenzie
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

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Configuration for publishing a plugin to a plugin repository.
 */
class PublishConfiguration {

    public static final List<String> DEFAULT_CHANNELS = ['default'].asImmutable()

    private ListProperty<String> channels

    private Property<String> token

    private Property<String> notes

    private Project project

    PublishConfiguration(Project project) {
        this.project = project
        this.channels = project.objects.listProperty(String)
        this.channels.set(DEFAULT_CHANNELS)
        this.token = project.objects.property(String)
        this.notes = project.objects.property(String)
    }

    /**
     * The list of channel names that the plugin will be published to on the plugin repository
     */
    ListProperty<String> getChannels() {
        return channels
    }

    void setChannels(List<String> channels) {
        this.channels.set(channels)
    }

    /**
     * The token for uploading the plugin to the plugin repository
     */
    Property<String> getToken() {
        return token
    }

    void setToken(String token) {
        this.token.set(token)
    }

    /**
     * The notes describing the changes made to the plugin
     */
    Property<String> getNotes() {
        return notes
    }

    void setNotes(String notes) {
        this.notes.set(notes)
    }
}
