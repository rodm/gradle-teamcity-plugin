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
package com.github.rodm.teamcity

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.jetbrains.zip.signer.signer.CertificateUtils
import org.jetbrains.zip.signer.signer.PrivateKeyUtils

import java.security.PrivateKey
import java.security.cert.X509Certificate

class SignConfiguration {
    private ListProperty<X509Certificate> certificateChain
    private Property<PrivateKey> privateKey
    private Project project

    SignConfiguration(Project project) {
        this.project = project
        this.certificateChain = project.objects.listProperty(X509Certificate)
        this.privateKey = project.objects.property(PrivateKey)
    }

    ListProperty<X509Certificate> getCertificateChain() {
        return certificateChain
    }

    void setCertificateChain(List<X509Certificate> certificateChain) {
        this.certificateChain.set(certificateChain)
    }

    Property<PrivateKey> getPrivateKey() {
        return privateKey
    }

    void setPrivateKey(PrivateKey privateKey) {
        this.privateKey.set(privateKey)
    }

    List<X509Certificate> loadCertificateChain(File file) {
        return CertificateUtils.loadCertificatesFromFile(file)
    }

    PrivateKey loadPrivateKey(File file, String password = null) {
        return PrivateKeyUtils.loadPrivateKey(file, password?.toCharArray())
    }
}
