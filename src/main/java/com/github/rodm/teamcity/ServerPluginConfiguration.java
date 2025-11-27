/*
 * Copyright 2015 Rod MacKenzie
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

import com.github.rodm.teamcity.internal.DefaultPublishConfiguration;
import com.github.rodm.teamcity.internal.DefaultSignConfiguration;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;

/**
 * Server-side plugin configuration
 */
public class ServerPluginConfiguration extends PluginConfiguration {

    private final ConfigurableFileCollection web;
    private PublishConfiguration publish;
    private SignConfiguration sign;

    private final ObjectFactory objects;

    public ServerPluginConfiguration(Project project) {
        super(project);
        this.objects = project.getObjects();
        this.web = objects.fileCollection();
    }

    /**
     * Configures the server-side plugin descriptor for the TeamCity plugin.
     *
     * <p>The given action is executed to configure the server-side plugin descriptor.</p>
     *
     * @param configuration The action.
     */
    public void descriptor(Action<ServerPluginDescriptor> configuration) {
        if (getDescriptor() == null) {
            ServerPluginDescriptor descriptor = objects.newInstance(ServerPluginDescriptor.class);
            setDescriptor(descriptor);
        }
        configuration.execute((ServerPluginDescriptor) getDescriptor());
    }

    public ConfigurableFileCollection getWeb() {
        return web;
    }

    public void setWeb(Object web) {
        this.web.setFrom(web);
    }

    public void web(Object web) {
        this.web.from(web);
    }

    /**
     * Configures the credentials to publish the plugin to the JetBrains Plugin Repository.
     *
     * <p>The given action is executed to configure the publishing credentials.</p>
     *
     * @param configuration The action.
     */
    public void publish(Action<PublishConfiguration> configuration) {
        if (publish == null) {
            publish = objects.newInstance(DefaultPublishConfiguration.class);
        }
        configuration.execute(publish);
    }

    public PublishConfiguration getPublish() {
        return publish;
    }

    public void sign(Action<SignConfiguration> configuration) {
        if (sign == null) {
            sign = objects.newInstance(DefaultSignConfiguration.class);
        }
        configuration.execute(sign);
    }

    public SignConfiguration getSign() {
        return sign;
    }
}
