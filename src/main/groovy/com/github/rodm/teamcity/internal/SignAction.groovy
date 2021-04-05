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

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.zip.signer.signer.CertificateUtils
import org.jetbrains.zip.signer.signer.PrivateKeyUtils
import org.jetbrains.zip.signer.signer.PublicKeyUtils
import org.jetbrains.zip.signer.signing.DefaultSignatureProvider
import org.jetbrains.zip.signer.signing.ZipSigner

abstract class SignAction implements WorkAction<Parameters> {

    static interface Parameters extends WorkParameters {
        RegularFileProperty getPluginFile()
        Property<String> getCertificateChain()
        Property<String> getPrivateKey()
        Property<String> getPassword()
        RegularFileProperty getSignedPluginFile()
    }

    @Override
    void execute() {
        Parameters parameters = getParameters()
        def pluginFile = parameters.pluginFile.get().asFile
        def signedPluginFile = parameters.signedPluginFile.get().asFile
        def certificateChain = CertificateUtils.loadCertificates(parameters.certificateChain.get())
        def privateKey = PrivateKeyUtils.loadPrivateKey(parameters.privateKey.get(), parameters.password.orNull?.toCharArray())

        ZipSigner.sign(
            pluginFile,
            signedPluginFile,
            certificateChain,
            new DefaultSignatureProvider(
                PublicKeyUtils.INSTANCE.getSuggestedSignatureAlgorithm(certificateChain[0].publicKey),
                privateKey
            )
        )
    }
}
