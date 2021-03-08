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
package com.github.rodm.teamcity.tasks

import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.zip.signer.signer.CertificateUtils
import org.jetbrains.zip.signer.signer.PrivateKeyUtils
import org.jetbrains.zip.signer.signer.PublicKeyUtils
import org.jetbrains.zip.signer.signing.DefaultSignatureProvider
import org.jetbrains.zip.signer.signing.ZipSigner

import java.security.PrivateKey
import java.security.cert.X509Certificate

class SignPluginTask extends DefaultTask {

    private RegularFileProperty pluginFile = project.objects.fileProperty()
    private RegularFileProperty signedPluginFile = project.objects.fileProperty()
    private RegularFileProperty certificateFile = project.objects.fileProperty()
    private RegularFileProperty privateKeyFile = project.objects.fileProperty()
    private Property<String> password = project.objects.property(String)

    SignPluginTask() {
        signedPluginFile.convention(pluginFile.map({signedName(it) }))
    }

    /**
     * @return the plugin file that will be signed
     */
    @InputFile
    RegularFileProperty getPluginFile() {
        return pluginFile
    }

    /**
     * @return signed plugin file
     */
    @OutputFile
    RegularFileProperty getSignedPluginFile() {
        return signedPluginFile
    }

    @InputFile
    RegularFileProperty getCertificateFile() {
        return certificateFile
    }

    @InputFile
    RegularFileProperty getPrivateKeyFile() {
        return privateKeyFile
    }

    @Input
    @Optional
    Property<String> getPassword() {
        return password
    }

    @TaskAction
    protected void signPlugin() {
        def pluginFile = getPluginFile().get().asFile
        def signedPluginFile = getSignedPluginFile().get().asFile
        def certificateChain = loadCertificateChain(certificateFile.get().asFile)
        def privateKey = loadPrivateKey(privateKeyFile.get().asFile, password.orNull)

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

    RegularFile signedName(RegularFile file) {
        def path = FilenameUtils.removeExtension(file.asFile.path)
        def extension = FilenameUtils.getExtension(file.asFile.name)
        return project.layout.projectDirectory.file("${path}-signed.${extension}")
    }

    static List<X509Certificate> loadCertificateChain(File file) {
        return CertificateUtils.loadCertificatesFromFile(file)
    }

    static PrivateKey loadPrivateKey(File file, String password) {
        return PrivateKeyUtils.loadPrivateKey(file, password?.toCharArray())
    }
}
