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

public class DisablePluginAction extends PluginAction {

    private static final String PLUGIN_SUCCESSFULLY_UNLOADED = "{}: Plugin '{}' successfully unloaded";
    private static final String PLUGIN_PARTIALLY_UNLOADED = "{}: Plugin '{}' partially unloaded - some parts could still be running. Server restart could be needed.";
    private static final String DISABLING_PLUGIN_FAILED = "{}: Disabling plugin '{}' failed: {}";

    public DisablePluginAction(Logger logger, File dataDir, Set<File> plugins, List<String> disabledPlugins) {
        super(logger, dataDir, plugins, disabledPlugins, false);
    }

    @Override
    public boolean canExecuteAction(Task task, String pluginName) {
        File pluginDir = new File(dataDir, "plugins");
        return new File(pluginDir, pluginName).exists();
    }

    @Override
    public void skipAction(String pluginName) {
        unloadedPlugins.add(pluginName);
    }

    public void sendRequest(HttpURLConnection request, final String pluginName) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
            String result = br.lines().collect(Collectors.joining());
            if (result.contains("Plugin unloaded successfully")) {
                getLogger().info(PLUGIN_SUCCESSFULLY_UNLOADED, getPath(), pluginName);
                unloadedPlugins.add(pluginName);
            } else {
                if (result.contains("Plugin unloaded partially")) {
                    getLogger().warn(PLUGIN_PARTIALLY_UNLOADED, getPath(), pluginName);
                    unloadedPlugins.add(pluginName);
                } else {
                    final String message = result.replace("<response>", "").replace("</response>", "");
                    getLogger().warn(DISABLING_PLUGIN_FAILED, getPath(), pluginName, message);
                }
            }
        }
        catch (IOException e) {
            throw new GradleException("Failure reading response from server");
        }
    }
}
