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

import com.github.rodm.teamcity.SignConfiguration;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public class DefaultSignConfiguration implements SignConfiguration {

    private final Property<String> certificateChain;
    private final Property<String> privateKey;
    private final Property<String> password;

    public DefaultSignConfiguration(Project project) {
        this.certificateChain = project.getObjects().property(String.class);
        this.privateKey = project.getObjects().property(String.class);
        this.password = project.getObjects().property(String.class);
    }

    public String getCertificateChain() {
        return certificateChain.get();
    }

    @Override
    public void setCertificateChain(String certificateChain) {
        this.certificateChain.set(certificateChain);
    }

    public Provider<String> getCertificateChainProperty() {
        return certificateChain;
    }

    public String getPrivateKey() {
        return privateKey.get();
    }

    @Override
    public void setPrivateKey(String privateKey) {
        this.privateKey.set(privateKey);
    }

    public Provider<String> getPrivateKeyProperty() {
        return privateKey;
    }

    public String getPassword() {
        return password.get();
    }

    @Override
    public void setPassword(String password) {
        this.password.set(password);
    }

    public Provider<String> getPasswordProperty() {
        return password;
    }
}
