package example.teamcity.agent;

import jetbrains.buildServer.agent.AgentBuildFeature;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.util.EventDispatcher;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ExampleBuildFeature extends AgentLifeCycleAdapter {

    private static final Logger log = Logger.getLogger("jetbrains.buildServer.AGENT");

    public ExampleBuildFeature(EventDispatcher<AgentLifeCycleListener> eventDispatcher) {
        eventDispatcher.addListener(this);
    }

    @Override
    public void buildStarted(@NotNull AgentRunningBuild build) {
        Collection<AgentBuildFeature> features = build.getBuildFeaturesOfType("example-build-feature");
        if (!features.isEmpty()) {
            log.info("ExampleBuildFeature enabled for build");
        }
    }
}
