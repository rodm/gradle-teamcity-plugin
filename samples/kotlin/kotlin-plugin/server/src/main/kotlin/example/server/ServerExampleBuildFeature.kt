
package example.server

import example.common.ExampleBuildFeature.BUILD_FEATURE_NAME
import jetbrains.buildServer.serverSide.BuildFeature

class ServerExampleBuildFeature : BuildFeature() {

    override fun getType(): String {
        return BUILD_FEATURE_NAME
    }

    override fun getDisplayName(): String {
        return "Example Build Feature"
    }

    override fun getEditParametersUrl(): String? {
        return null
    }

    override fun isMultipleFeaturesPerBuildTypeAllowed(): Boolean {
        return false
    }

    override fun describeParameters(params: Map<String, String>): String {
        return "Example Build Feature plugin"
    }
}
