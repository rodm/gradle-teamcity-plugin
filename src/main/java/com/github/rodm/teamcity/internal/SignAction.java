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

import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.jetbrains.zip.signer.signer.CertificateUtils;
import org.jetbrains.zip.signer.signer.PrivateKeyUtils;
import org.jetbrains.zip.signer.signer.PublicKeyUtils;
import org.jetbrains.zip.signer.signing.DefaultSignatureProvider;
import org.jetbrains.zip.signer.signing.ZipSigner;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

public abstract class SignAction implements WorkAction<SignAction.SignParameters> {

    public interface SignParameters extends WorkParameters {
        RegularFileProperty getPluginFile();
        Property<String> getCertificateChain();
        Property<String> getPrivateKey();
        Property<String> getPassword();
        RegularFileProperty getSignedPluginFile();
    }

    @Override
    public void execute() {
        try {
            SignParameters parameters = getParameters();
            File pluginFile = parameters.getPluginFile().get().getAsFile();
            File signedPluginFile = parameters.getSignedPluginFile().get().getAsFile();
            String certificate = parameters.getCertificateChain().get();
            List<X509Certificate> certificateChain = CertificateUtils.loadCertificates(certificate);
            String encodedPrivateKey = parameters.getPrivateKey().get();
            String password = parameters.getPassword().getOrNull();
            PrivateKey privateKey = PrivateKeyUtils.loadPrivateKey(encodedPrivateKey, (password == null) ? null : password.toCharArray());

            ZipSigner.sign(
                pluginFile,
                signedPluginFile,
                certificateChain,
                new DefaultSignatureProvider(
                    PublicKeyUtils.INSTANCE.getSuggestedSignatureAlgorithm(certificateChain.get(0).getPublicKey()),
                    privateKey));
        }
        catch (IOException | CertificateException e) {
            throw new GradleException("Failure signing plugin", e);
        }
    }
}
