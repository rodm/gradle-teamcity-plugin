/*
 * Copyright 2024 the original author or authors.
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

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class ShutdownWaitAction implements Action<Task> {

    private static final long DEFAULT_TIMEOUT = 10;

    private final Provider<String> timeoutProperty;

    public ShutdownWaitAction(Provider<String> timeoutProperty) {
        this.timeoutProperty = timeoutProperty;
    }

    @Override
    public void execute(@NotNull Task task) {
        if (!(task instanceof ServerConfiguration)) {
            throw new GradleException(getClass().getSimpleName() + " attached to an invalid task");
        }
        ServerConfiguration serverConfigurationTask = (ServerConfiguration) task;
        final String host = serverConfigurationTask.getServerHost().get();
        final int port = Integer.parseInt(serverConfigurationTask.getServerPort().get());
        sleep(250);
        if (!isServerAvailable(host, port)) {
            return;
        }

        final String path = task.getName();
        final Logger logger = task.getLogger();
        long timeout = getTimeout();
        long waitTime = TimeUnit.MILLISECONDS.convert(timeout, TimeUnit.SECONDS);
        logger.info("{}: TeamCity Server shutdown requested. Timeout is {} seconds.", path, timeout);
        while (isServerAvailable(host, port) && waitTime > 0) {
            logger.info("{}: TeamCity Server is still running", path);
            waitTime -= sleep(500);
        }
        if (isServerAvailable(host, port)) {
            throw new GradleException("Time out waiting for TeamCity Server to shutdown.");
        }
        logger.info("{}: TeamCity Server has stopped", path);
    }

    private long sleep(int timeout) {
        long start = System.currentTimeMillis();
        try {
            TimeUnit.MILLISECONDS.sleep(timeout);
        }
        catch (InterruptedException e) {
            // ignore
        }
        return System.currentTimeMillis() - start;
    }

    private long getTimeout() {
        if (timeoutProperty.isPresent()) {
            try {
                return Long.parseLong(timeoutProperty.get());
            }
            catch (NumberFormatException e) {
                // ignore
            }
        }
        return DEFAULT_TIMEOUT;
    }

    private boolean isServerAvailable(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return socket.isConnected();
        }
        catch (IOException ignored) {
            return false;
        }
    }
}
