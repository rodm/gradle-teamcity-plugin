package com.github.rodm.teamcity.tasks

import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.zip.signer.signer.PublicKeyUtils
import org.jetbrains.zip.signer.signing.DefaultSignatureProvider
import org.jetbrains.zip.signer.signing.ZipSigner

import java.security.PrivateKey
import java.security.cert.X509Certificate

class SignPluginTask extends DefaultTask {
    private RegularFileProperty pluginFile = project.objects.fileProperty()
    private RegularFileProperty signedPluginFile = project.objects.fileProperty()
    private ListProperty<X509Certificate> certificateChain = project.objects.listProperty(X509Certificate)
    private Property<PrivateKey> privateKey = project.objects.property(PrivateKey)

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


    @Input
    ListProperty<X509Certificate> getCertificateChain() {
        return certificateChain
    }

    @Input
    Property<PrivateKey> getPrivateKey() {
        return privateKey
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    @TaskAction
    protected void signPlugin() {
        def pluginFile = getPluginFile().get().asFile
        def defaultSignedPluginName = FilenameUtils.removeExtension(pluginFile.path)
        def signedPluginFile = getSignedPluginFile().getOrNull()?.asFile ?:
            new File(defaultSignedPluginName + "-signed" + FilenameUtils.getExtension(pluginFile.name))

        def certificateChain = getCertificateChain().get()
        def privateKey = getPrivateKey().get()

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
