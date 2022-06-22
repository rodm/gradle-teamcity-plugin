package example.teamcity;

import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.apache.log4j.Logger;

public class Example2ServerPlugin extends BuildServerAdapter {

    private static final Logger logger = Logger.getLogger("jetbrains.buildServer.SERVER");

    private final SBuildServer server;

    private final PluginDescriptor descriptor;

    public Example2ServerPlugin(SBuildServer server, PluginDescriptor descriptor) {
        this.server = server;
        this.descriptor = descriptor;
    }

    public void register() {
        server.addListener(this);
        String name = getClass().getSimpleName();
        logger.info(String.format("%s: Plugin name: %s, version: %s", name, descriptor.getPluginName(), descriptor.getPluginVersion()));
        logger.info(String.format("%s: Plugin parameter: param=%s", name, descriptor.getParameterValue("param")));
    }

    @Override
    public void serverShutdown() {
        String name = getClass().getSimpleName();
        logger.info(String.format("%s: Server shutdown: Unregistering plugin %s", name, descriptor.getPluginName()));
    }
}
