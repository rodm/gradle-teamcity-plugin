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
package com.github.rodm.teamcity.internal

import com.github.rodm.teamcity.PublishConfiguration
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * Configuration for publishing a plugin to a plugin repository.
 */
@CompileStatic
class DefaultPublishConfiguration implements PublishConfiguration {

    private static final List<String> DEFAULT_CHANNELS = ['default'].asImmutable()

    private ListProperty<String> channels
    private Property<String> token
    private Property<String> notes

    DefaultPublishConfiguration(Project project) {
        this.channels = project.objects.listProperty(String).convention(DEFAULT_CHANNELS)
        this.token = project.objects.property(String)
        this.notes = project.objects.property(String)
    }

    /**
     * The list of channel names that the plugin will be published to on the plugin repository
     */
    List<String> getChannels() {
        return channels.get()
    }

    void setChannels(List<String> channels) {
        this.channels.set(channels)
    }

    ListProperty<String> getChannelsProperty() {
        return channels
    }

    /**
     * The token for uploading the plugin to the plugin repository
     */
    String getToken() {
        return token.get()
    }

    void setToken(String token) {
        this.token.set(token)
    }

    Provider<String> getTokenProperty() {
        return token
    }


    /**
     * The notes describing the changes made to the plugin
     */
    String getNotes() {
        return notes.get()
    }

    void setNotes(String notes) {
        this.notes.set(notes)
    }

    Provider<String> getNotesProperty() {
        return notes
    }
}
