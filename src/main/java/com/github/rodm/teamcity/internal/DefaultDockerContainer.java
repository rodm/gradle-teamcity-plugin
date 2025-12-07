/*
 * Copyright 2025 the original author or authors.
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

import com.github.rodm.teamcity.DockerContainer;
import com.github.rodm.teamcity.Ports;
import com.github.rodm.teamcity.Variables;
import com.github.rodm.teamcity.Volumes;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;

public class DefaultDockerContainer implements DockerContainer {

    /**
     * The name of the container
     */
    private final String name;

    private final Property<String> image;
    private final MapProperty<String, String> ports;
    private final MapProperty<String, String> variables;
    private final MapProperty<String, String> volumes;

    @Inject
    public DefaultDockerContainer(String name, ObjectFactory factory) {
        this.name = name;
        this.image = factory.property(String.class);
        this.ports = factory.mapProperty(String.class, String.class);
        this.variables = factory.mapProperty(String.class, String.class);
        this.volumes = factory.mapProperty(String.class, String.class);
    }

    @Override
    public String getName() {
        return name;
    }

    public final String getCapitalizedName() {
        return capitalize(name);
    }

    @Override
    public String getImage() {
        return getImageProperty().get();
    }

    @Override
    public void setImage(String image) {
        this.image.set(image);
    }

    public Provider<String> getImageProperty() {
        return image;
    }

    public void ports(Action<? super Ports> action) {
        action.execute(this);
    }

    @Override
    public void port(String host, String container) {
        ports.put(host, container);
    }

    public MapProperty<String, String> getPorts() {
        return ports;
    }

    public void variables(Action<? super Variables> action) {
        action.execute(this);
    }

    @Override
    public void variable(String key, String value) {
        variables.put(key, value);
    }

    public MapProperty<String, String> getVariables() {
        return variables;
    }

    public void volumes(Action<? super Volumes> action) {
        action.execute(this);
    }

    @Override
    public void volume(String path, String containerPath) {
        validatePath(path);
        volumes.put(path, containerPath);
    }

    public MapProperty<String, String> getVolumes() {
        return volumes;
    }

    private String capitalize(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private void validatePath(String path) {
        if (path.isEmpty() || path.contains("/") || path.contains("\\")) {
            throw new InvalidUserDataException("Invalid host path: " + path) ;
        }
    }
}
