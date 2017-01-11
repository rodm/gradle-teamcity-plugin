
package example.agent

import jetbrains.buildServer.agent.AgentBuildFeature
import jetbrains.buildServer.agent.AgentLifeCycleAdapter
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.util.EventDispatcher
import org.apache.log4j.Logger

import example.common.ExampleBuildFeature.BUILD_FEATURE_NAME

class AgentExampleBuildFeature : AgentLifeCycleAdapter {

    val log: Logger = Logger.getLogger("jetbrains.buildServer.AGENT")

    constructor(eventDispatcher: EventDispatcher<AgentLifeCycleAdapter> ) {
        eventDispatcher.addListener(this)
    }

    override fun buildStarted(build: AgentRunningBuild) {
    val features: Collection<AgentBuildFeature> = build.getBuildFeaturesOfType(BUILD_FEATURE_NAME)
        if (!features.isEmpty()) {
            log.info("ExampleBuildFeature enabled for build")
        }
    }
}
