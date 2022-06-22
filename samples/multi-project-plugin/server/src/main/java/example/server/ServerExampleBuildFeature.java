
package example.server;

import jetbrains.buildServer.serverSide.BuildFeature;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static example.common.ExampleBuildFeature.BUILD_FEATURE_NAME;

public class ServerExampleBuildFeature extends BuildFeature {

    @NotNull
    @Override
    public String getType() {
        return BUILD_FEATURE_NAME;
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
