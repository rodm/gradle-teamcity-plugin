/*
 * Copyright 2016 Rod MacKenzie
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

import groovy.transform.CompileStatic

@CompileStatic
class TeamCityEnvironment {

    final String name

    String version = '9.0'

    String downloadUrl

    File homeDir

    File dataDir

    File javaHome

    private List<String> serverOptions = ['-Dteamcity.development.mode=true', '-Dteamcity.development.shadowCopyClasses=true']

    private List<String> agentOptions = []

    TeamCityEnvironment() {
        this('default')
    }

    TeamCityEnvironment(String name) {
        this.name = name
    }

    def getServerOptions() {
        return serverOptions.join(' ')
    }

    def setServerOptions(String options) {
        this.serverOptions.clear()
        this.serverOptions.addAll(options)
    }

    def serverOptions(String options) {
        this.serverOptions.addAll(options)
    }

    def getAgentOptions() {
        return agentOptions.join(' ')
    }

    def setAgentOptions(String options) {
        this.agentOptions.clear()
        this.agentOptions.add(options)
    }

    def agentOptions(String options) {
        this.agentOptions.add(options)
    }
}
