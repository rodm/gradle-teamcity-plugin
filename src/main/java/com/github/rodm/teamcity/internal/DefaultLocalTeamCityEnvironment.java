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

import com.github.rodm.teamcity.BaseTeamCityEnvironment;
import com.github.rodm.teamcity.LocalTeamCityEnvironment;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;

public class DefaultLocalTeamCityEnvironment extends BaseTeamCityEnvironment implements LocalTeamCityEnvironment {

    private final DefaultTeamCityEnvironments environments;

    private final Property<String> downloadUrl;
    private final Provider<String> installerFile;
    private final Property<String> homeDir;
    private final Property<String> javaHome;

    @Inject
    public DefaultLocalTeamCityEnvironment(String name, DefaultTeamCityEnvironments environments, ObjectFactory factory) {
        super(name, environments, factory);
        this.environments = environments;
        this.downloadUrl = factory.property(String.class).convention(defaultDownloadUrl());
        this.installerFile = factory.property(String.class).convention(defaultInstallerFile());
        this.homeDir = factory.property(String.class).convention(defaultHomeDir());
        this.javaHome = factory.property(String.class).convention(System.getProperty("java.home"));
    }

    /**
     * The download URL used to download the TeamCity distribution for this environment.
     */
    public String getDownloadUrl() {
        return gradleProperty(propertyName("downloadUrl")).orElse(downloadUrl).get();
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl.set(downloadUrl);
    }

    public Provider<String> getInstallerFile() {
        return installerFile;
    }

    /**
     * The home directory for this environment's TeamCity installation.
     */
    public String getHomeDir() {
        return getHomeDirProperty().get();
    }

    public void setHomeDir(String homeDir) {
        this.homeDir.set(homeDir);
    }

    public Provider<String> getHomeDirProperty() {
        return gradleProperty(propertyName("homeDir")).orElse(homeDir);
    }

    /**
     * The Java home directory used to start the server and agent for this environment.
     */
    public String getJavaHome() {
        return getJavaHomeProperty().get();
    }

    public void setJavaHome(String javaHome) {
        this.javaHome.set(javaHome);
    }

    public Provider<String> getJavaHomeProperty() {
        return gradleProperty(propertyName("javaHome")).orElse(javaHome);
    }

    private Provider<String> defaultDownloadUrl() {
        return environments.getBaseDownloadUrlProperty().map(baseUrl -> baseUrl + "/TeamCity-" + getVersion() + ".tar.gz");
    }

    private Provider<String> defaultInstallerFile() {
        return environments.getDownloadsDirProperty().map(dir -> dir + "/" + filename());
    }

    private String filename() {
        String url = downloadUrl.get();
        int index = url.lastIndexOf("/") + 1;
        return url.substring(index);
    }

    private Provider<String> defaultHomeDir() {
        return environments.getBaseHomeDirProperty().map(dir -> dir + "/TeamCity-" + getVersion());
    }
}
