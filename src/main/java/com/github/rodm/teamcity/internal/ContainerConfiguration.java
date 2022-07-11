/*
 * Copyright 2022 the original author or authors.
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

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;

import java.util.ArrayList;
import java.util.List;

public class ContainerConfiguration {

    private String image;
    private String containerId;
    private final List<Bind> binds = new ArrayList<>();
    private final List<PortBinding> portBindings = new ArrayList<>();
    private final List<ExposedPort> exposedPorts = new ArrayList<>();
    private final List<String> environment = new ArrayList<>();

    public static ContainerConfiguration builder() {
        return new ContainerConfiguration();
    }

    public ContainerConfiguration image(String image) {
        this.image = image;
        return this;
    }

    public String getImage() {
        return image;
    }

    public ContainerConfiguration name(String containerId) {
        this.containerId = containerId;
        return this;
    }

    public String getName() {
        return containerId;
    }

    public ContainerConfiguration bind(String hostPath, String containerPath) {
        binds.add(Bind.parse(hostPath + ":" + containerPath));
        return this;
    }

    public List<Bind> getBinds() {
        return binds;
    }

    public ContainerConfiguration environment(String name, String value) {
        environment.add(name + "=" + value);
        return this;
    }

    public List<String> getEnvironment() {
        return environment;
    }

    public ContainerConfiguration bindPort(String hostPort, String containerPort) {
        portBindings.add(PortBinding.parse(hostPort + ":" + containerPort));
        return this;
    }

    public List<PortBinding> getPortBindings() {
        return portBindings;
    }

    public ContainerConfiguration exposePort(String port) {
        exposedPorts.add(ExposedPort.parse(port));
        return this;
    }

    public List<ExposedPort> getExposedPorts() {
        return exposedPorts;
    }
}