
package example.common

import jetbrains.buildServer.util.StringUtil

object ExampleBuildFeature {

    const val BUILD_FEATURE_NAME = "example"

    init {
        // Just simple usage of TeamCity Common API (shared between server and agent)
        assert(StringUtil.isTrue("true"))
    }
}
