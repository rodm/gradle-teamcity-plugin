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
package com.github.rodm.teamcity

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.bundling.Zip

class PackagePlugin extends Zip {

    private def descriptor

    private FileCollection serverComponents

    PackagePlugin() {
        description = 'Package TeamCity plugin'
        group = 'TeamCity'

        into('server') {
            from {
                def serverComponents = getServerComponents()
                serverComponents ? serverComponents.filter { File file -> file.isFile() } : []
            }
        }
        into('') {
            from {
                getDescriptor()
            }
            rename {
                'teamcity-plugin.xml'
            }
        }
    }

    @InputFiles
    FileCollection getServerComponents() {
        return serverComponents
    }

    void setServerComponents(Object components) {
        this.serverComponents = project.files(components)
    }

    @InputFile
    public File getDescriptor() {
        return descriptor
    }

    public void setDescriptor(File descriptor) {
        this.descriptor = descriptor
    }
}
