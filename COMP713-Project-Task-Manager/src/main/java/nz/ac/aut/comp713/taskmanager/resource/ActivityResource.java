package nz.ac.aut.comp713.taskmanager.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import nz.ac.aut.comp713.taskmanager.service.ActivityService;

import java.util.Map;

/*
 * GET /api/activity?limit=20
 * Most recent task events processed by the message consumer.
 */
@Path("/activity")
@Produces(MediaType.APPLICATION_JSON)
public class ActivityResource {

    @Inject
    private ActivityService service;

    @GET
    public Response recentActivity(@QueryParam("limit") Integer limit) {

        try {
            return Response.ok(service.getRecentActivity(limit)).build();

        } catch (IllegalArgumentException error) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
        }
    }
}