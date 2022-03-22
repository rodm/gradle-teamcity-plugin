/*
 * Copyright 2015 the original author or authors.
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

import com.github.rodm.teamcity.TeamCityVersion;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public abstract class TeamCityTask extends DefaultTask {

    private static final String VERSION_MISMATCH_WARNING = "%s: Version %s does not match the TeamCity version %s installed at %s.";
    private static final String VERSION_INCOMPATIBLE = "Version %s is not compatible with the TeamCity version %s installed at %s.";
    private static final String INVALID_HOME_DIR = "Invalid TeamCity installation at %s.";
    private static final String MISSING_VERSION = "Unable to read version of TeamCity installation at %s";

    @Input
    private final Property<String> version = getProject().getObjects().property(String.class);

    @Input
    private final Property<String> homeDir = getProject().getObjects().property(String.class);

    @Input
    private final Property<String> javaHome = getProject().getObjects().property(String.class);

    public final Property<String> getVersion() {
        return version;
    }

    public final Property<String> getHomeDir() {
        return homeDir;
    }

    public final Property<String> getJavaHome() {
        return javaHome;
    }

    @TaskAction
    public void exec() {
        validate();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        getProject().exec(execSpec -> {
            configure(execSpec);
            execSpec.setStandardOutput(out);
            execSpec.setErrorOutput(out);
            execSpec.setIgnoreExitValue(true);
        });
        getLogger().info(out.toString());
    }

    public abstract void configure(ExecSpec execSpec);

    public void validate() {
        validTeamCityHomeDirectory(getVersion().get(), getHomeDir().get());
        validDirectory("javaHome", getJavaHome().get());
    }

    public void validTeamCityHomeDirectory(String version, String homeDir) {
        validDirectory("homeDir", homeDir);
        String installationVersion = getTeamCityVersion(homeDir);
        if (installationVersion.equals(version)) {
            return;
        }
        if (dataVersion(extractVersion(installationVersion)).equals(dataVersion(version))) {
            getLogger().warn(String.format(VERSION_MISMATCH_WARNING, getPath(), version, installationVersion, homeDir));
            return;
        }
        throw new InvalidUserDataException(String.format(VERSION_INCOMPATIBLE, version, installationVersion, homeDir));
    }

    public static void validDirectory(String propertyName, String value) {
        if (value == null) {
            throw new InvalidUserDataException(String.format("Property '%s' not set.", propertyName));
        }
        File path = new File(value);
        if (!path.exists()) {
            throw new InvalidUserDataException(String.format("Directory '%s' specified for property '%s' does not exist.", value, propertyName));
        } else if (!path.isDirectory()) {
            throw new InvalidUserDataException(String.format("Directory '%s' specified for property '%s' is not a directory.", value, propertyName));
        }
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("windows");
    }

    private static String dataVersion(String version) {
        return TeamCityVersion.version(version).getDataVersion();
    }

    private static String extractVersion(String version) {
        if (version.contains("EAP") || version.contains("RC")) {
            return version.split(" ")[0];
        } else {
            return version;
        }
    }

    private static String getTeamCityVersion(String homeDir) {
        Path jarPath = Paths.get(homeDir, "webapps/ROOT/WEB-INF/lib/common-api.jar");
        if (!Files.isRegularFile(jarPath)) {
            throw new InvalidUserDataException(String.format(INVALID_HOME_DIR, homeDir));
        }

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            ZipEntry entry = jarFile.getEntry("serverVersion.properties.xml");
            if (entry == null) {
                throw new InvalidUserDataException(String.format(MISSING_VERSION, homeDir));
            }
            try (InputStream is = jarFile.getInputStream(entry)) {
                Properties props = new Properties();
                props.loadFromXML(is);
                return ((String) (props.get("Display_Version")));
            }
        }
        catch (IOException e) {
            throw new GradleException("Failure loading server version", e);
        }
    }
}
