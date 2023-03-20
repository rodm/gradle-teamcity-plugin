package example.teamcity;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jmock.Mock;
import org.testng.annotations.Test;

public class ExampleServerPluginTest extends BaseTestCase {

    @Test
    public void pluginRegistersWithServer() {
        Mock mockServer = mock(SBuildServer.class);
        mockServer.expects(once()).method("addListener");
        Mock mockPluginDescriptor = mock(PluginDescriptor.class);
        mockPluginDescriptor.stubs();
        SBuildServer server = (SBuildServer) mockServer.proxy();
        PluginDescriptor descriptor = (PluginDescriptor) mockPluginDescriptor.proxy();

        ExampleServerPlugin plugin = new ExampleServerPlugin(server, descriptor);
        plugin.register();

        verify();
    }
}
