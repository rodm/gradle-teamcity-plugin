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
package com.github.rodm.teamcity

import com.github.rodm.teamcity.docker.ContainerConfiguration
import org.junit.jupiter.api.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.empty
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.nullValue

class ContainerConfigurationTest {

    private ContainerConfiguration config = ContainerConfiguration.builder()

    @Test
    void 'configuration default settings'() {
        assertThat(this.config.getImage(), nullValue())
        assertThat(this.config.getName(), nullValue())
        assertThat(this.config.getAutoRemove(), is(false))
        assertThat(this.config.getBinds(), is(empty()))
        assertThat(this.config.getEnvironment(), is(empty()))
        assertThat(this.config.getPortBindings(), is(empty()))
        assertThat(this.config.getExposedPorts(), is(empty()))
    }

    @Test
    void 'set container image name'() {
        config.image('jetbrains/teamcity-server:2022.04.2')

        assertThat(config.getImage(), equalTo('jetbrains/teamcity-server:2022.04.2'))
    }

    @Test
    void 'set container name'() {
        config.name('test-container')

        assertThat(config.getName(), equalTo('test-container'))
    }

    @Test
    void 'auto remove setting enabled'() {
        config.autoRemove()

        assertThat(config.getAutoRemove(), is(true))
    }

    @Test
    void 'add a bind volume'() {
        config.bind('/host/path', '/container/path')

        assertThat(config.getBinds(), hasSize(1))
        assertThat(config.getBinds().get(0), equalTo('/host/path:/container/path'))
    }

    @Test
    void 'add multiple volumes'() {
        config.bind(['/host/path': '/container/path', '/host/data': '/container/data'])

        def binds = config.getBinds()
        assertThat(binds, hasSize(2))
        assertThat(binds.get(0), equalTo('/host/path:/container/path'))
        assertThat(binds.get(1), equalTo('/host/data:/container/data'))
    }

    @Test
    void 'add an environment variable'() {
        config.environment("ENV_VAR", "VALUE")

        assertThat(config.getEnvironment(), hasSize(1))
        assertThat(config.getEnvironment().get(0), equalTo("ENV_VAR=VALUE"))
    }

    @Test
    void 'add environment variables using a map'() {
        config.environment(["ENV_VAR_1": "VALUE_1", "ENV_VAR_2": "VALUE_2"])

        assertThat(config.getEnvironment(), hasSize(2))
        assertThat(config.getEnvironment().get(0), equalTo("ENV_VAR_1=VALUE_1"))
        assertThat(config.getEnvironment().get(1), equalTo("ENV_VAR_2=VALUE_2"))
    }

    @Test
    void 'add a port binding'() {
        config.bindPort("7111", "8111")

        assertThat(config.getPortBindings(), hasSize(1))
        assertThat(config.getPortBindings().get(0), equalTo("7111:8111"))
    }

    @Test
    void 'add multiple port bindings'() {
        config.bindPorts(["7111": '8111', '1234': '2345'])

        def ports = config.getPortBindings()
        assertThat(ports, hasSize(2))
        assertThat(ports.get(0), equalTo("7111:8111"))
        assertThat(ports.get(1), equalTo("1234:2345"))
    }

    @Test
    void 'add an exposed port'() {
        config.exposePort("7111")

        assertThat(config.getExposedPorts(), hasSize(1))
        assertThat(config.getExposedPorts().get(0), equalTo("7111"))
    }

    @Test
    void 'add multiple exposed ports'() {
        config.exposePorts(["7111", "1234"])

        def ports = config.getExposedPorts()
        assertThat(ports, hasSize(2))
        assertThat(ports.get(0), equalTo("7111"))
        assertThat(ports.get(1), equalTo("1234"))
    }
}
