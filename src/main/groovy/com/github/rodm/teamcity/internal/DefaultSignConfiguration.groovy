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

import com.github.rodm.teamcity.SignConfiguration
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

@CompileStatic
class DefaultSignConfiguration implements SignConfiguration {

    private Property<String> certificateChain
    private Property<String> privateKey
    private Property<String> password

    DefaultSignConfiguration(Project project) {
        this.certificateChain = project.objects.property(String)
        this.privateKey = project.objects.property(String)
        this.password = project.objects.property(String)
    }

    String getCertificateChain() {
        return certificateChain.get()
    }

    @Override
    void setCertificateChain(String certificateChain) {
        this.certificateChain.set(certificateChain)
    }

    Provider<String> getCertificateChainProperty() {
        return certificateChain
    }

    String getPrivateKey() {
        return privateKey.get()
    }

    @Override
    void setPrivateKey(String privateKey) {
        this.privateKey.set(privateKey)
    }

    Provider<String> getPrivateKeyProperty() {
        return privateKey
    }

    String getPassword() {
        return password.get()
    }

    @Override
    void setPassword(String password) {
        this.password.set(password)
    }

    Provider<String> getPasswordProperty() {
        return password
    }
}
