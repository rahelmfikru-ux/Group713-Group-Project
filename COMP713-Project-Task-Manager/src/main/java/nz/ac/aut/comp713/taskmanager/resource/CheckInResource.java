package nz.ac.aut.comp713.taskmanager.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import nz.ac.aut.comp713.taskmanager.model.CheckIn;
import nz.ac.aut.comp713.taskmanager.service.CheckInService;
import nz.ac.aut.comp713.taskmanager.service.TaskManagerNotFoundException;

import java.util.Map;

/*
 * POST /api/projects/{projectId}/tasks/{taskId}/check-ins
 * Android employee checks in at a task (GPS or manual location).
 *
 * The full path is on the class on purpose: JAX-RS picks ONE root
 * resource class by its longest literal path, so a class mapped to
 * "/projects/{projectId}" would lose every .../tasks/... request to
 * TaskResource ("/projects/{projectId}/tasks") and return 404.
 */
@Path("/projects/{projectId}/tasks/{taskId}/check-ins")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CheckInResource {

    @Inject
    private CheckInService service;

    @POST
    public Response checkIn(
            @PathParam("projectId") Long projectId,
            @PathParam("taskId") Long taskId,
            CheckIn request) {

        try {
            return Response.status(Response.Status.CREATED)
                    .entity(service.checkIn(projectId, taskId, request))
                    .build();

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