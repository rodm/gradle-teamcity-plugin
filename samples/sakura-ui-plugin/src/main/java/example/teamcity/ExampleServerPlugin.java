package example.teamcity;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ExampleServerPlugin extends BaseController {

    private static final String PLUGIN_NAME = "SakuraUI-Plugin";
    private static final String PLUGIN_URL = "/reactPlugin.html";

    private final PluginDescriptor descriptor;

    public ExampleServerPlugin(@NotNull PluginDescriptor descriptor,
                               @NotNull PagePlaces places,
                               @NotNull WebControllerManager controller)
    {
        this.descriptor = descriptor;
        final SimplePageExtension extension = new SimplePageExtension(places);
        extension.setPluginName(PLUGIN_NAME);
        extension.setPlaceId(PlaceId.ALL_PAGES_FOOTER_PLUGIN_CONTAINER);
        extension.setIncludeUrl(PLUGIN_URL);
        extension.register();

        controller.registerController(PLUGIN_URL, this);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
        final ModelAndView mv = new ModelAndView(descriptor.getPluginResourcesPath("react-plugin.jsp"));
        mv.getModel().put("PLUGIN_NAME", PLUGIN_NAME);
        return mv;
    }
}
