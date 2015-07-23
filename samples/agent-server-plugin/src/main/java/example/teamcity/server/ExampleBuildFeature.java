package example.teamcity.server;

import jetbrains.buildServer.serverSide.BuildFeature;
import java.util.Map;

public class ExampleBuildFeature extends BuildFeature {

    @Override
    public String getType() {
        return "example-build-feature";
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
