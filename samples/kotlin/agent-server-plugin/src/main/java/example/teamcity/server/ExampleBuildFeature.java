package example.teamcity.server;

import jetbrains.buildServer.serverSide.BuildFeature;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ExampleBuildFeature extends BuildFeature {

    @NotNull
    @Override
    public String getType() {
        return "example-build-feature";
    }

    @NotNull
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

    @NotNull
    @Override
    public String describeParameters(@NotNull Map<String, String> params) {
        return "Example Build Feature plugin";
    }
}
