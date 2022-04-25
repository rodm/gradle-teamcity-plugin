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

import com.github.rodm.teamcity.internal.AbstractDeployment;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

/**
 * Agent-side plugin deployment configuration
 */
public class PluginDeployment extends AbstractDeployment {

    /**
     * Use a separate classloader to load the agent-side plugin's classes.
     */
    @Input
    @Optional
    private Boolean useSeparateClassloader;

    public Boolean getUseSeparateClassloader() {
        return useSeparateClassloader;
    }

    public void setUseSeparateClassloader(Boolean useSeparateClassloader) {
        this.useSeparateClassloader = useSeparateClassloader;
    }
}
