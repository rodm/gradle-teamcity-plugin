
package example.agent

import jetbrains.buildServer.agent.AgentBuildFeature
import jetbrains.buildServer.agent.AgentLifeCycleAdapter
import jetbrains.buildServer.agent.AgentLifeCycleListener
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.util.EventDispatcher
import org.apache.log4j.Logger

import example.common.ExampleBuildFeature.BUILD_FEATURE_NAME

class AgentExampleBuildFeature(eventDispatcher: EventDispatcher<AgentLifeCycleListener>) : AgentLifeCycleAdapter() {

    private val log: Logger = Logger.getLogger("jetbrains.buildServer.AGENT")

    init {
        eventDispatcher.addListener(this)
    }

    override fun buildStarted(build: AgentRunningBuild) {
        val features: Collection<AgentBuildFeature> = build.getBuildFeaturesOfType(BUILD_FEATURE_NAME)
        if (!features.isEmpty()) {
            log.info("ExampleBuildFeature enabled for build")
        }
    }
}
