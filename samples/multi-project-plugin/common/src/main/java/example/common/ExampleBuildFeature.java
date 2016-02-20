
package example.common;

import jetbrains.buildServer.util.StringUtil;

public class ExampleBuildFeature {

    public static final String BUILD_FEATURE_NAME = "example";

    {
        // Just simple usage of TeamCity Common API (shared between server and agent)
        assert StringUtil.isTrue("true");
    }
}
