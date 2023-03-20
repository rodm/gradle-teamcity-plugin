package example.teamcity.agent;

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.util.EventDispatcher;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ExampleAgentBuildListener extends AgentLifeCycleAdapter {

    private static final Logger log = Logger.getLogger("jetbrains.buildServer.AGENT");

    private static final String PARAMETER_NAME = "docker.demo.parameter";

    public ExampleAgentBuildListener(EventDispatcher<AgentLifeCycleListener> eventDispatcher) {
        eventDispatcher.addListener(this);
    }

    @Override
    public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
        String value = runner.getRunnerParameters().getOrDefault(PARAMETER_NAME, "not set");
        log.info("Docker demo parameter: " + value);
    }
}
