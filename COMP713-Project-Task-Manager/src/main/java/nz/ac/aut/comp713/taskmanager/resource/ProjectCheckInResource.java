package nz.ac.aut.comp713.taskmanager.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import nz.ac.aut.comp713.taskmanager.service.CheckInService;
import nz.ac.aut.comp713.taskmanager.service.TaskManagerNotFoundException;

import java.util.Map;

/*
 * GET /api/projects/{projectId}/check-ins
 * All check-ins for a project, newest first (manager website).
 */
@Path("/projects/{projectId}/check-ins")
@Produces(MediaType.APPLICATION_JSON)
public class ProjectCheckInResource {

    @Inject
    private CheckInService service;

    @GET
    public Response projectCheckIns(@PathParam("projectId") Long projectId) {

        try {
            return Response.ok(service.getCheckInsForProject(projectId)).build();

        } catch (TaskManagerNotFoundException error) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", error.getMessage()))
                    .build();

        } catch (IllegalArgumentException error) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
        }
    }
}