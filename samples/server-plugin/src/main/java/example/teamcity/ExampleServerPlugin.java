package example.teamcity;

import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.apache.log4j.Logger;

public class ExampleServerPlugin extends BuildServerAdapter {

    private static final Logger logger = Logger.getLogger("jetbrains.buildServer.SERVER");

    private final SBuildServer server;

    private final PluginDescriptor descriptor;

    public ExampleServerPlugin(SBuildServer server, PluginDescriptor descriptor) {
        this.server = server;
        this.descriptor = descriptor;
    }

    public void register() {
        server.addListener(this);
        logger.info(String.format("ExampleServerPlugin: Plugin name: %s, version: %s", descriptor.getPluginName(), descriptor.getPluginVersion()));
        logger.info(String.format("ExampleServerPlugin: Plugin parameter: param=%s", descriptor.getParameterValue("param")));
    }

    @Override
    public void serverShutdown() {
        logger.info(String.format("ExampleServerPlugin: Server shutdown: Unregistering plugin %s", descriptor.getPluginName()));
    }
}
