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

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Set;

public abstract class PluginAction implements Action<Task> {

    private static final String SUPER_USER_TOKEN_PATH = "system/pluginData/superUser/token.txt";

    private final Logger logger;
    protected File dataDir;
    protected Set<File> plugins;
    protected List<String> unloadedPlugins;
    private final boolean enable;
    private String path;

    private static final String host = "localhost";
    private static final int port = 8111;

    protected PluginAction(Logger logger, File dataDir, Set<File> plugins, List<String> unloadedPlugins, boolean enable) {
        this.logger = logger;
        this.dataDir = dataDir;
        this.plugins = plugins;
        this.unloadedPlugins = unloadedPlugins;
        this.enable = enable;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getPath() {
        return path;
    }

    @Override
    public void execute(final Task task) {
        path = task.getPath();
        plugins.forEach(file -> {
            if (canExecuteAction(task, file.getName())) {
                executeAction(file.getName());
            } else {
                skipAction(file.getName());
            }
        });
    }

    public abstract boolean canExecuteAction(Task task, String pluginName);

    public abstract void sendRequest(HttpURLConnection request, String pluginName);

    public void executeAction(String pluginName) {
        if (!isServerAvailable()) {
            logger.info("{}: Cannot connect to the server on http://{}:{}.", getPath(), host, port);
            return;
        }

        String password;
        File tokenFile = new File(dataDir, SUPER_USER_TOKEN_PATH);
        if (tokenFile.isFile()) {
            try {
                String content = new String(Files.readAllBytes(tokenFile.toPath()));
                password = Long.valueOf(content).toString();
                logger.debug("{}: Using {} maintenance token to authenticate", getPath(), password);
            }
            catch (IOException ignored) {
                logger.warn("{}: Failure reading super user token file", getPath());
                return;
            }
            catch (NumberFormatException ignored) {
                logger.warn("{}: Malformed maintenance token", getPath());
                return;
            }
        } else {
            logger.warn("{}: Maintenance token file does not exist. Cannot reload plugin.", getPath());
            logger.warn("{}: Check the server was started with '-Dteamcity.superUser.token.saveToFile=true' property.", getPath());
            return;
        }

        String authToken = "Basic " + Base64.getEncoder().encodeToString((":" + password).getBytes(StandardCharsets.UTF_8));

        URL actionURL = getPluginActionURL(pluginName);
        logger.debug("{}: Sending {}", getPath(), actionURL);

        try {
            HttpURLConnection request = (HttpURLConnection) actionURL.openConnection();
            try {
                request.setRequestMethod("POST");
                request.setRequestProperty("Authorization", authToken);
                sendRequest(request, pluginName);
            }
            catch (IOException ex) {
                if (request.getResponseCode() == 401) {
                    logger.warn("{}: Cannot authenticate with server on http://{}:{} with maintenance token {}.", getPath(), host, port, password);
                    logger.warn("{}: Check the server was started with '-Dteamcity.superUser.token.saveToFile=true' property.", getPath());
                }
                logger.warn(getPath() + ": Cannot connect to the server on http://" + host + ":" + port + ": " + request.getResponseCode(), ex);
            }
        }
        catch (IOException e) {
            logger.warn("{}: Cannot connect to server.", getPath());
        }
    }

    @SuppressWarnings("UnusedMethodParameter")
    public void skipAction(String pluginName) {
    }

    public boolean isServerAvailable() {
        try (Socket socket = new Socket(host, port)) {
            return socket.isConnected();
        }
        catch (IOException ignored) {
            return false;
        }
    }

    private URL getPluginActionURL(final String pluginName) {
        try {
            final String pluginPath = URLEncoder.encode("<TeamCity Data Directory>/plugins/" + pluginName, "UTF-8");
            return new URL("http://" + host + ":" + port + "/httpAuth/admin/plugins.html?action=setEnabled&enabled=" + enable + "&pluginPath=" + pluginPath);
        }
        catch (MalformedURLException | UnsupportedEncodingException e) {
            throw new GradleException("Failure creating plugin action URL");
        }
    }
}
