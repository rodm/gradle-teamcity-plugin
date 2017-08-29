/*
 * Copyright 2015 Rod MacKenzie
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
import org.gradle.api.Action
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

import static groovy.transform.TypeCheckingMode.SKIP

@CompileStatic
class ServerPluginDescriptor {

    @Input
    String name

    @Input
    String displayName

    @Input
    String version

    @Input
    @Optional
    String description

    @Input
    @Optional
    String downloadUrl

    @Input
    @Optional
    String email

    @Input
    String vendorName

    @Input
    @Optional
    String vendorUrl

    @Input
    @Optional
    String vendorLogo

    @Input
    @Optional
    String minimumBuild

    @Input
    @Optional
    String maximumBuild

    @Input
    @Optional
    Boolean useSeparateClassloader

    @Nested
    Parameters parameters

    @Nested
    Dependencies dependencies

    @CompileStatic(SKIP)
    ServerPluginDescriptor() {
        this.parameters = extensions.create('parameters', Parameters)
        this.dependencies = extensions.create('dependencies', Dependencies)
    }

    def parameters(Action<Parameters> configuration) {
        configuration.execute(parameters)
    }

    def dependencies(Action<Dependencies> configuration) {
        configuration.execute(dependencies)
    }
}
