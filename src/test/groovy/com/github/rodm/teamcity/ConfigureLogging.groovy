/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rodm.teamcity

import org.gradle.api.logging.LogLevel
import org.gradle.internal.logging.events.OutputEventListener
import org.gradle.internal.logging.slf4j.OutputEventListenerBackedLogger
import org.gradle.internal.logging.slf4j.OutputEventListenerBackedLoggerContext
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.rules.ExternalResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.logging.LogManager

class ConfigureLogging extends ExternalResource implements BeforeEachCallback, AfterEachCallback {
    private final OutputEventListener listener
    private final OutputEventListenerBackedLoggerContext context
    private final OutputEventListenerBackedLogger logger
    private OutputEventListener originalListener

    ConfigureLogging(OutputEventListener listener) {
        this.listener = listener
        context = LoggerFactory.ILoggerFactory as OutputEventListenerBackedLoggerContext
        logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as OutputEventListenerBackedLogger
    }

    @Override
    void beforeEach(ExtensionContext context) throws Exception {
        attachListener()
    }

    @Override
    void afterEach(ExtensionContext context) throws Exception {
        resetLogging()
    }

    @Override
    protected void before() {
        attachListener()
    }

    void attachListener() {
        // Retain the previously configured listener
        originalListener = context.outputEventListener

        context.outputEventListener = listener
        context.level = LogLevel.DEBUG
    }

    @Override
    protected void after() {
        resetLogging()
    }

    void resetLogging() {
        context.reset()
        LogManager.getLogManager().reset()

        // Reinstate the previously configured listener, if any
        if (originalListener != null) {
            context.outputEventListener = originalListener
        }
    }

    void setLevel(LogLevel level) {
        context.level = level
    }
}
