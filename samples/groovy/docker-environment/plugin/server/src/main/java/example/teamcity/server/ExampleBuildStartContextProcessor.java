package example.teamcity.server;

import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.SRunnerContext;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;

public class ExampleBuildStartContextProcessor implements BuildStartContextProcessor {

    private static final String PARAMETER_NAME = "docker.demo.parameter";

    @Override
    public void updateParameters(@NotNull BuildStartContext context) {
        String value = "Docker demo value (" + LocalDate.now() + ")";
        for (SRunnerContext runnerContext : context.getRunnerContexts()) {
            runnerContext.addRunnerParameter(PARAMETER_NAME, value);
        }
    }
}
