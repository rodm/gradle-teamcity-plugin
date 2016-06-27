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

import com.github.rodm.teamcity.ProgressLoggerWrapper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class Download extends DefaultTask {

    private static final int BUFFER_SIZE = 10000;

    @Input
    String source

    @OutputFile
    File target

    @TaskAction
    void download() {
        ProgressLoggerWrapper progressLogger = new ProgressLoggerWrapper(project, getSource())
        OutputStream os = null
        InputStream is = null
        try {
            progressLogger.started()

            os = new BufferedOutputStream(new FileOutputStream(getTarget()))
            URLConnection conn = new URL(getSource()).openConnection()
            is = conn.getInputStream()
            int bytesRead
            long totalBytesRead = 0
            long loggedMb = 0
            byte[] buffer = new byte[BUFFER_SIZE]
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);

                totalBytesRead += bytesRead;
                long processedMb = totalBytesRead / (1024 * 1024)
                if (processedMb > loggedMb) {
                    progressLogger.progress(String.format("%dMB downloaded", processedMb))
                    loggedMb = processedMb
                }
            }
        }
        finally {
            progressLogger.completed()

            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }
}
