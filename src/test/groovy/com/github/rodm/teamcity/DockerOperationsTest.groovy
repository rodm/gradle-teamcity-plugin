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

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.InspectContainerCmd
import com.github.dockerjava.api.command.InspectImageCmd
import com.github.dockerjava.api.exception.ConflictException
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.core.command.CreateContainerCmdImpl
import com.github.dockerjava.core.exec.CreateContainerCmdExec
import com.github.rodm.teamcity.docker.ContainerConfiguration
import com.github.rodm.teamcity.docker.DockerOperations
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.mockito.Mockito.any
import static org.mockito.Mockito.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.spy
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class DockerOperationsTest {

    private DockerClient client
    private DockerOperations dockerOperations

    @BeforeEach
    void init() {
        client = mock(DockerClient)
        dockerOperations = new DockerOperations(client)
    }

    @Test
    void 'image is available'() {
        def command = mock(InspectImageCmd)
        when(client.inspectImageCmd(eq('image'))).thenReturn(command)

        assertThat(dockerOperations.isImageAvailable('image'), equalTo(true))
        verify(client).inspectImageCmd('image')
    }

    @Test
    void 'image is not available'() {
        def command = mock(InspectImageCmd)
        when(command.exec()).thenThrow(NotFoundException)
        when(client.inspectImageCmd(eq('image'))).thenReturn(command)

        assertThat(dockerOperations.isImageAvailable('image'), equalTo(false))
    }

    @Test
    void 'container is available'() {
        def command = mock(InspectContainerCmd)
        when(client.inspectContainerCmd(eq('containerId'))).thenReturn(command)

        assertThat(dockerOperations.isContainerAvailable('containerId'), equalTo(true))
        verify(client).inspectContainerCmd('containerId')
    }

    @Test
    void 'container is not available'() {
        def command = mock(InspectContainerCmd)
        when(command.exec()).thenThrow(NotFoundException)
        when(client.inspectContainerCmd(eq('containerId'))).thenReturn(command)

        assertThat(dockerOperations.isContainerAvailable('containerId'), equalTo(false))
    }

    @Nested
    class CreateContainerTests {

        private CreateContainerCmd createContainer

        @BeforeEach
        void setup() {
            def createResponse = new CreateContainerResponse()
            createResponse.id = 'containerId'

            def exec = mock(CreateContainerCmdExec)
            when(client.createContainerCmd(any())).thenAnswer(new Answer<CreateContainerCmd>() {
                @Override
                CreateContainerCmd answer(InvocationOnMock invocation) throws Throwable {
                    String image = invocation.getArgument(0)
                    createContainer = spy(new CreateContainerCmdImpl(exec, null, image) {
                        @Override
                        CreateContainerResponse exec() throws NotFoundException, ConflictException {
                            return createResponse
                        }
                    })
                    return createContainer
                }
            })
        }

        private static ContainerConfiguration configuration() {
            return ContainerConfiguration.builder()
                .image('image')
                .name('name')
        }

        @Test
        void 'create container uses configured image and container name'() {
            ContainerConfiguration config = configuration()
                .image('test-image')
                .name('test-name')

            dockerOperations.createContainer(config)

            assertThat(createContainer.getImage(), equalTo('test-image'))
            assertThat(createContainer.getName(), equalTo('test-name'))
            verify(client).createContainerCmd('test-image')
        }

        @Test
        void 'create container returns container id'() {
            ContainerConfiguration config = configuration()

            def id = dockerOperations.createContainer(config)

            assertThat(id, equalTo('containerId'))
        }

        @Test
        void 'create container has auto remove set in host config'() {
            ContainerConfiguration config = configuration()
                .autoRemove()

            dockerOperations.createContainer(config)

            def hostConfig = createContainer.getHostConfig()
            assertThat(hostConfig.autoRemove, equalTo(Boolean.TRUE))
        }

        @Test
        void 'create container has volume bind set in host config'() {
            ContainerConfiguration config = configuration()
                .bind('/host', '/container')

            dockerOperations.createContainer(config)

            def hostConfig = createContainer.getHostConfig()
            assertThat(hostConfig.binds[0], equalTo(Bind.parse('/host:/container')))
        }

        @Test
        void 'create container command is closed after use'() {
            ContainerConfiguration config = configuration()

            dockerOperations.createContainer(config)

            verify(createContainer).close()
        }
    }
}
