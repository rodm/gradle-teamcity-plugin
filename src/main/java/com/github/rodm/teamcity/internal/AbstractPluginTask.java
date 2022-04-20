/*
 * Copyright 2020 Rod MacKenzie
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

import org.gradle.api.Transformer;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.bundling.Zip;

import static com.github.rodm.teamcity.TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME;

public abstract class AbstractPluginTask extends Zip {

    public static final Transformer<String, String> PLUGIN_DESCRIPTOR_RENAMER = new Transformer<String, String>() {
        @Override
        public String transform(String ignore) {
            return PLUGIN_DESCRIPTOR_FILENAME;
        }
    };

    @InputFile
    public abstract RegularFileProperty getDescriptor();
}
