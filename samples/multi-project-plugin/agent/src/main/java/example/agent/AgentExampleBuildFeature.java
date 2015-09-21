
package example.agent;

import jetbrains.buildServer.agent.AgentBuildFeature;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.util.EventDispatcher;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static example.common.ExampleBuildFeature.BUILD_FEATURE_NAME;

public class AgentExampleBuildFeature extends AgentLifeCycleAdapter {

    private static Logger log = Logger.getLogger("jetbrains.buildServer.AGENT");

    public AgentExampleBuildFeature(EventDispatcher<AgentLifeCycleAdapter> eventDispatcher) {
        eventDispatcher.addListener(this);
    }

    @Override
    public void buildStarted(@NotNull AgentRunningBuild build) {
        Collection<AgentBuildFeature> features = build.getBuildFeaturesOfType(BUILD_FEATURE_NAME);
        if (!features.isEmpty()) {
            log.info("ExampleBuildFeature enabled for build");
        }
    }
}