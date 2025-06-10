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
package com.github.rodm.teamcity.docker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContainerConfiguration implements Serializable {

    private String image;
    private String containerId;
    private boolean autoRemove;
    private final List<String> binds = new ArrayList<>();
    private final List<String> portBindings = new ArrayList<>();
    private final List<String> exposedPorts = new ArrayList<>();
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

    public ContainerConfiguration autoRemove() {
        this.autoRemove = true;
        return this;
    }

    public boolean getAutoRemove() {
        return autoRemove;
    }

    public ContainerConfiguration bind(String hostPath, String containerPath) {
        binds.add(hostPath + ":" + containerPath);
        return this;
    }

    public List<String> getBinds() {
        return binds;
    }

    public ContainerConfiguration environment(Map<String, String> variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            environment(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public ContainerConfiguration environment(String name, String value) {
        environment.add(name + "=" + value);
        return this;
    }

    public List<String> getEnvironment() {
        return environment;
    }

    public ContainerConfiguration bindPort(String hostPort, String containerPort) {
        portBindings.add(hostPort + ":" + containerPort);
        return this;
    }

    public List<String> getPortBindings() {
        return portBindings;
    }

    public ContainerConfiguration exposePort(String port) {
        exposedPorts.add(port);
        return this;
    }

    public List<String> getExposedPorts() {
        return exposedPorts;
    }
}
