/*
 * Copyright 2021 the original author or authors.
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
package com.github.rodm.teamcity.tasks

import com.github.rodm.teamcity.internal.SignAction
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

class SignPluginTask extends DefaultTask {

    private RegularFileProperty pluginFile = project.objects.fileProperty()
    private RegularFileProperty signedPluginFile = project.objects.fileProperty()
    private Property<String> certificateChain = project.objects.property(String)
    private Property<String> privateKey = project.objects.property(String)
    private Property<String> password = project.objects.property(String)
    private FileCollection classpath
    private final WorkerExecutor executor

    @Inject
    SignPluginTask(WorkerExecutor executor) {
        description = "Signs the plugin"
        signedPluginFile.convention(pluginFile.map({signedName(it) }))
        this.executor = executor
    }

    @Classpath
    FileCollection getClasspath() {
        return classpath
    }

    void setClasspath(FileCollection classpath) {
        this.classpath = classpath
    }

    /**
     * @return the plugin file that will be signed
     */
    @InputFile
    RegularFileProperty getPluginFile() {
        return pluginFile
    }

    /**
     * @return signed plugin file
     */
    @OutputFile
    RegularFileProperty getSignedPluginFile() {
        return signedPluginFile
    }

    @Input
    Property<String> getCertificateChain() {
        return certificateChain
    }

    @Input
    Property<String> getPrivateKey() {
        return privateKey
    }

    @Input
    @Optional
    Property<String> getPassword() {
        return password
    }

    @TaskAction
    protected void signPlugin() {
        WorkQueue queue = executor.classLoaderIsolation { spec ->
            spec.classpath.from(this.classpath)
        }
        queue.submit(SignAction) { params ->
            params.pluginFile = this.pluginFile
            params.certificateChain = this.certificateChain
            params.privateKey = this.privateKey
            params.password = this.password
            params.signedPluginFile = this.signedPluginFile
        }
        queue.await()
    }

    RegularFile signedName(RegularFile file) {
        def path = FilenameUtils.removeExtension(file.asFile.path)
        def extension = FilenameUtils.getExtension(file.asFile.name)
        return project.layout.projectDirectory.file("${path}-signed.${extension}")
    }
}
