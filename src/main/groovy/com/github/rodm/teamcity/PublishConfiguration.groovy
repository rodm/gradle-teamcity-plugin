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

/**
 * Configuration for publishing a plugin to a plugin repository.
 */
class PublishConfiguration {

    public static final List<String> DEFAULT_CHANNELS = ['default'].asImmutable()

    private List<String> channels = DEFAULT_CHANNELS

    private String username

    private String password

    /**
     * The list of channel names that the plugin will be published to on the plugin repository
     */
    List<String> getChannels() {
        return channels
    }

    void setChannels(List<String> channels) {
        this.channels = channels
    }

    /**
     * The username for uploading the plugin to the plugin repository
     */
    String getUsername() {
        return username
    }

    void setUsername(String username) {
        this.username = username
    }

    /**
     * The password for uploading the plugin to the plugin repository
     */
    String getPassword() {
        return password
    }

    void setPassword(String password) {
        this.password = password
    }
}
