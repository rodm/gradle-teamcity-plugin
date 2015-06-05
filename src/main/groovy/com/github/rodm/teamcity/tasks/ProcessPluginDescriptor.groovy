/*
 * Copyright 2015 Rod MacKenzie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rodm.teamcity.tasks

import com.github.rodm.teamcity.TeamCityPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.InputFile

class ProcessPluginDescriptor extends Copy {

    private File descriptor

    ProcessPluginDescriptor() {
        setDestinationDir(new File(project.getBuildDir(), TeamCityPlugin.PLUGIN_DESCRIPTOR_DIR))
        from {
            getDescriptor()
        }
        rename {
            TeamCityPlugin.PLUGIN_DESCRIPTOR_FILENAME
        }
    }

    @InputFile
    File getDescriptor() {
        return descriptor
    }
}
