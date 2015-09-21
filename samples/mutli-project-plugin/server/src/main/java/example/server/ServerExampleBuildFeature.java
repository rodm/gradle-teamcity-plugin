
package example.server;

import jetbrains.buildServer.serverSide.BuildFeature;

import java.util.Map;

import static example.common.ExampleBuildFeature.BUILD_FEATURE_NAME;

public class ServerExampleBuildFeature extends BuildFeature {

    @Override
    public String getType() {
        return BUILD_FEATURE_NAME;
    }

    @Override
    public String getDisplayName() {
        return "Example Build Feature";
    }

    @Override
    public String getEditParametersUrl() {
        return null;
    }

    @Override
    public boolean isMultipleFeaturesPerBuildTypeAllowed() {
        return false;
    }

    @Override
    public String describeParameters(Map<String, String> params) {
        return "Example Build Feature plugin";
    }
}
