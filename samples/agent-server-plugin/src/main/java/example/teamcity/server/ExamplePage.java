package example.teamcity.server;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ExamplePage extends BaseController {

    private PluginDescriptor descriptor;

    public ExamplePage(WebControllerManager manager, PluginDescriptor descriptor) {
        manager.registerController("/example.html", this);
        this.descriptor = descriptor;
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return new ModelAndView(descriptor.getPluginResourcesPath("example.jsp"));
    }
}
