/*
 * Copyright 2022 Rod MacKenzie
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

import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EnablePluginAction extends PluginAction {

    private static final String PLUGIN_SUCCESSFULLY_LOADED = "{}: Plugin '{}' successfully loaded";
    private static final String ENABLING_PLUGIN_FAILED = "{}: Enabling plugin '{}' failed: {}";

    public EnablePluginAction(Logger logger, File dataDir, Set<File> plugins, List<String> disabledPlugins) {
        super(logger, dataDir, plugins, disabledPlugins, true);
    }

    @Override
    public boolean canExecuteAction(Task task, String pluginName) {
        return unloadedPlugins.contains(pluginName);
    }

    public void sendRequest(HttpURLConnection request, final String pluginName) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
            String result = br.lines().collect(Collectors.joining());
            if (result.contains("Plugin loaded successfully")) {
                getLogger().info(PLUGIN_SUCCESSFULLY_LOADED, getPath(), pluginName);
            } else {
                final String message = result.replace("<response>", "").replace("</response>", "");
                getLogger().warn(ENABLING_PLUGIN_FAILED, getPath(), pluginName, message);
            }
        }
        catch (IOException e) {
            throw new GradleException("Failure reading response from server");
        }
    }
}
