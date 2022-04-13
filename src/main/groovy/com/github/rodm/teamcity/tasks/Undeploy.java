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
package com.github.rodm.teamcity.tasks;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.Internal;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Undeploy extends Delete {

    @Inject
    public Undeploy(ObjectFactory objects, ProviderFactory providers) {
        setDescription("Un-deploys plugins from the TeamCity Server");
        Provider<List<File>> files = providers.provider(() ->
            getPlugins().getFiles().stream()
                .map(file -> getPluginsDir().file(file.getName()).get().getAsFile())
                .collect(Collectors.toList()));
        delete(objects.fileCollection().from(files));
    }

    @Internal
    public abstract ConfigurableFileCollection getPlugins();

    @Internal
    public abstract DirectoryProperty getPluginsDir();
}
