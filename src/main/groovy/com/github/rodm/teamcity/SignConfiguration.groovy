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
