package cloudos.resources;

import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.model.app.AppConfiguration;
import cloudos.model.app.CloudOsApp;
import cloudos.model.support.AppDownloadRequest;
import cloudos.service.task.TaskId;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static cloudos.resources.ApiConstants.H_API_KEY;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.APPS_ENDPOINT)
@Service @Slf4j
public class AppsResource {

    @Autowired private SessionDAO sessionDAO;
    @Autowired private AppDAO appDAO;

    /**
     * List all apps that are installed and active
     * @param apiKey The session ID
     * @return a List of InstalledApps
     */
    @GET
    @ReturnType("java.util.List<cloudos.model.app.CloudOsApp>")
    public Response listActiveApps (@HeaderParam(H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        final List<CloudOsApp> apps;
        try {
            apps = appDAO.findActive();
        } catch (Exception e) {
            log.error("Error finding installed apps: "+e, e);
            return Response.serverError().build();
        }

        // if account is not admin, scrub out databags
        if (!admin.isAdmin()) {
            for (CloudOsApp app : apps) app.setDatabags(null);
        }

        return Response.ok(apps).build();
    }

    /**
     * Download a new app from a URL. Must be admin
     * @param apiKey The session ID
     * @param request The installation request
     * @statuscode 403 if caller is not an admin
     * @return a TaskId, can be used to check installation progress
     */
    @POST
    @Path("/download")
    @ReturnType("cloudos.service.task.TaskId")
    public Response downloadApp (@HeaderParam(H_API_KEY) String apiKey,
                                 AppDownloadRequest request) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // only admins can download apps
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        final TaskId taskId = appDAO.download(admin, request);

        return Response.ok(taskId).build();
    }

    /**
     * Retrieve configuration options for an app
     * @param apiKey The session ID
     * @param app The app name
     * @param version The app version
     * @return the AppConfiguration
     * @statuscode 403 if caller is not an admin
     */
    @GET
    @Path("/apps/{app}/versions/{version}/config")
    @ReturnType("cloudos.model.app.AppConfiguration")
    public Response getConfiguration (@HeaderParam(H_API_KEY) String apiKey,
                                      @PathParam("app") String app,
                                      @PathParam("version") String version) {
        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // only admins can view app configs
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        final AppConfiguration config = appDAO.getConfiguration(app, version);
        return Response.ok(config).build();
    }

    /**
     * Write configuration options for an app.
     * @param apiKey The session ID
     * @param app The app name
     * @param version The app version
     * @param config The updated app configuration. Anything not specified in this object will not be changed.
     * @return Just an HTTP status code
     * @statuscode 403 if caller is not an admin
     */
    @POST
    @Path("/apps/{app}/versions/{version}/config")
    @ReturnType("java.lang.Void")
    public Response setConfiguration (@HeaderParam(H_API_KEY) String apiKey,
                                      @PathParam("app") String app,
                                      @PathParam("version") String version,
                                      AppConfiguration config) {
        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // only admins can view app configs
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        appDAO.setConfiguration(app, version, config);
        return Response.ok().build();
    }

    /**
     * Install an application. Must already be downloaded and configured (if configuration is required).
     * @param apiKey The session ID
     * @param app The app name
     * @param version The app version
     * @param force If true, force installation even if the same version is already installed
     * @statuscode 403 if caller is not an admin
     * @return a TaskId, can be used to check installation progress
     */
    @POST
    @Path("/apps/{app}/versions/{version}/install")
    @ReturnType("cloudos.service.task.TaskId")
    public Response installApp (@HeaderParam(H_API_KEY) String apiKey,
                                @PathParam("app") String app,
                                @PathParam("version") String version,
                                @QueryParam("force") Boolean force) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // only admins can install apps
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        final TaskId taskId = appDAO.install(admin, app, version, force == null ? false : force);

        return Response.ok(taskId).build();
    }

}
