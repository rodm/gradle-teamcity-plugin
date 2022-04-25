package com.github.rodm.teamcity.internal;

import com.github.rodm.teamcity.Deployment;
import com.github.rodm.teamcity.ExecutableFiles;
import org.gradle.api.Action;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.Nested;

public class AbstractDeployment implements Deployment {

    @Nested
    protected final ExecutableFiles executableFiles;

    public AbstractDeployment() {
        executableFiles = ((ExtensionAware) this).getExtensions().create("executableFiles", ExecutableFiles.class);
    }

    @Override
    public void executableFiles(Action<ExecutableFiles> configuration) {
        configuration.execute(executableFiles);
    }

    @Override
    public ExecutableFiles getExecutableFiles() {
        return executableFiles;
    }
}
