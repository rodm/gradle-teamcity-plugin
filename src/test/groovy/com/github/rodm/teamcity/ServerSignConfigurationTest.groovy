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

import com.github.rodm.teamcity.internal.SignAction
import com.github.rodm.teamcity.tasks.PublishTask
import com.github.rodm.teamcity.tasks.ServerPlugin
import com.github.rodm.teamcity.tasks.SignPluginTask
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.nullValue
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.greaterThan

class ServerSignConfigurationTest {

    @TempDir
    public Path projectDir

    Project project

    TeamCityPluginExtension extension

    @BeforeEach
    void setup() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build()
        project.apply plugin: 'com.github.rodm.teamcity-server'
        extension = project.getExtensions().getByType(TeamCityPluginExtension)
    }

    @Test
    void 'sign task is only created with sign configuration'() {
        project.teamcity {
            server {
            }
        }

        project.evaluate()

        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        assertThat(signPlugin, is(nullValue()))
    }

    @Test
    void 'sign task is configured with a certificate file'() {
        project.teamcity {
            server {
                sign {
                    certificateChain = 'certificate-chain'
                }
            }
        }

        project.evaluate()

        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        assertThat(signPlugin.certificateChain.get(), equalTo('certificate-chain'))
    }

    @Test
    void 'sign task is configured with a password file and no password'() {
        project.teamcity {
            server {
                sign {
                    privateKey = 'private-key'
                }
            }
        }

        project.evaluate()

        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        assertThat(signPlugin.privateKey.get(), equalTo('private-key'))
        assertThat(signPlugin.password.orNull, is(nullValue()))
    }

    @Test
    void 'sign task is configured with a password file and password'() {
        project.teamcity {
            server {
                sign {
                    privateKey = 'private-key'
                    password = 'password'
                }
            }
        }

        project.evaluate()

        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        assertThat(signPlugin.privateKey.get(), equalTo('private-key'))
        assertThat(signPlugin.password.orNull, equalTo('password'))
    }

    @Test
    void 'sign task is configured with output of package task'() {
        project.teamcity {
            server {
                sign {
                }
            }
        }

        project.evaluate()

        ServerPlugin serverPlugin = (ServerPlugin) project.tasks.findByPath(':serverPlugin')
        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        assertThat(signPlugin.pluginFile.get(), equalTo(serverPlugin.archiveFile.get()))
    }

    @Test
    void 'signed plugin file name defaults to server plugin task output with -signed appended'() {
        project.teamcity {
            server {
                descriptor {
                    archiveName = 'test-plugin-name.zip'
                }
                sign {
                }
            }
        }

        project.evaluate()

        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        assertThat(signPlugin.signedPluginFile.get().asFile.name, equalTo('test-plugin-name-signed.zip'))
    }

    @Test
    void 'support signing configuration being created from multiple configuration blocks'() {
        project.teamcity {
            server {
                sign {
                    certificateChain = 'certificate-chain'
                }
            }
        }
        project.teamcity {
            server {
                sign {
                    privateKey = 'private-key'
                }
            }
        }
        project.evaluate()

        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        assertThat(signPlugin.certificateChain.get(), equalTo('certificate-chain'))
    }

    @Test
    void 'publish task is configured with output of sign task'() {
        project.teamcity {
            server {
                sign {
                }
                publish {
                }
            }
        }

        project.evaluate()

        SignPluginTask signPlugin = (SignPluginTask) project.tasks.findByPath(':signPlugin')
        PublishTask publishPlugin = (PublishTask) project.tasks.findByPath(':publishPlugin')
        assertThat(publishPlugin.distributionFile.get(), equalTo(signPlugin.signedPluginFile.get()))
    }

    @Nested
    class SignExecutionTest {

        @BeforeEach
        void init() {
            Security.addProvider(new BouncyCastleProvider())
        }

        @Test
        void 'sign action signs a plugin'() {
            File plugin = projectDir.resolve('build/distributions/test.zip').toFile()
            File signed = projectDir.resolve('build/distributions/signed.zip').toFile()
            createFakePlugin(plugin)

            KeyPair keyPair = generateKeyPair()
            X509Certificate certificate = generateCertificate(keyPair)

            def certificateChain = convertCertificateToPEM(certificate)
            def privateKey = convertPrivateKeyToPEM(keyPair.private)

            def sign = new SignAction() {
                private SignAction.SignParameters parameters = new TestSignParameters(project)

                @Override
                SignAction.SignParameters getParameters() {
                    return this.parameters
                }
            }
            sign.parameters.pluginFile.set(plugin)
            sign.parameters.certificateChain.set(certificateChain)
            sign.parameters.privateKey.set(privateKey)
            sign.parameters.signedPluginFile.set(signed)

            sign.execute()

            assertThat('signed plugin file should be much larger than original', signed.size(), greaterThan(plugin.size() + 1000))
        }

        private void createFakePlugin(File plugin) {
            plugin.parentFile.mkdirs()
            FileOutputStream fos = new FileOutputStream(plugin)
            ZipOutputStream zos = new ZipOutputStream(fos)
            ZipEntry ze = new ZipEntry('teamcity-plugin.xml')
            zos.putNextEntry(ze)
            zos.write("test file".getBytes())
            zos.closeEntry()
            zos.close()
        }

        private KeyPair generateKeyPair() {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", PROVIDER_NAME)
            keyPairGenerator.initialize(4096, new SecureRandom())
            return keyPairGenerator.genKeyPair()
        }

        private X509Certificate generateCertificate(KeyPair keyPair) {
            def serial = BigInteger.valueOf(System.currentTimeMillis())
            Date notBefore = LocalDate.now().minusDays(1).toDate()
            Date notAfter = LocalDate.now().plusDays(1).toDate()

            def issuer = new X500Name("CN=Test")
            SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
            X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(issuer, serial, notBefore, notAfter, issuer, keyInfo)
            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA1WithRSA").setProvider(PROVIDER_NAME).build(keyPair.private)
            X509CertificateHolder holder = certificateBuilder.build(contentSigner)
            return new JcaX509CertificateConverter().setProvider(PROVIDER_NAME).getCertificate(holder)
        }

        private String convertCertificateToPEM(X509Certificate certificate) throws IOException {
            StringWriter writer = new StringWriter()
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer)
            pemWriter.writeObject(certificate)
            pemWriter.close()
            return writer.toString()
        }

        private String convertPrivateKeyToPEM(PrivateKey privateKey) {
            StringWriter writer = new StringWriter()
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer)
            pemWriter.writeObject(privateKey)
            pemWriter.close()
            return writer.toString()
        }

        static class TestSignParameters implements SignAction.SignParameters {
            RegularFileProperty pluginFile
            Property<String> certificateChain
            Property<String> privateKey
            Property<String> password
            RegularFileProperty signedPluginFile

            TestSignParameters(Project project) {
                pluginFile = project.objects.fileProperty()
                certificateChain = project.objects.property(String)
                privateKey = project.objects.property(String)
                password = project.objects.property(String)
                signedPluginFile = project.objects.fileProperty()
            }
        }
    }
}
