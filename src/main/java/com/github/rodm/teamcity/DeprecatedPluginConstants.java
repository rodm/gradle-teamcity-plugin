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
package com.github.rodm.teamcity;

public class DeprecatedPluginConstants {

    public static final String DEPRECATED_SERVER_PLUGIN_ID = "com.github.rodm.teamcity-server";
    public static final String DEPRECATED_AGENT_PLUGIN_ID = "com.github.rodm.teamcity-agent";
    public static final String DEPRECATED_COMMON_PLUGIN_ID = "com.github.rodm.teamcity-common";
    public static final String DEPRECATED_ENVIRONMENTS_PLUGIN_ID = "com.github.rodm.teamcity-environments";

    public static final String DEPRECATED_PLUGIN_MESSAGE = "The {} plugin has been deprecated. Please use the {} plugin instead.";

    private DeprecatedPluginConstants() {
        throw new IllegalStateException("Utility class");
    }
}
