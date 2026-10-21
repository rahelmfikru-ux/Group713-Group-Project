
package nz.ac.aut.comp713.taskmanager.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import nz.ac.aut.comp713.taskmanager.model.Task;
import nz.ac.aut.comp713.taskmanager.service.TaskService;
import nz.ac.aut.comp713.taskmanager.service.TaskManagerNotFoundException;

import java.util.Map;

@Path("/projects/{projectId}/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {

    @Inject
    private TaskService service;

    @GET
    public Response getTasksForProject(
            @PathParam("projectId") Long projectId) {

        try {
            return Response.ok(
                service.getTasksForProject(projectId)
            ).build();

        } catch (TaskManagerNotFoundException error) {
            return notFound(error);

        } catch (IllegalArgumentException error) {
            return invalid(error);
        }
    }

    @POST
    public Response createTask(
            @PathParam("projectId") Long projectId,
            Task task) {

        try {
            Task created = service.createTask(projectId, task);

            return Response.status(Response.Status.CREATED)
                    .entity(created)
                    .build();

        } catch (TaskManagerNotFoundException error) {
            return notFound(error);

        } catch (IllegalArgumentException error) {
            return invalid(error);
        }
    }

    @PUT
    @Path("/{taskId}/status")
    public Response updateTaskStatus(
            @PathParam("projectId") Long projectId,
            @PathParam("taskId") Long taskId,
            Map<String, String> body) {

        try {
            String status =
                    body == null ? null : body.get("status");

            Task updated = service.updateTaskStatus(
                projectId,
                taskId,
                status
            );

            return Response.ok(updated).build();

        } catch (TaskManagerNotFoundException error) {
            return notFound(error);

        } catch (IllegalArgumentException error) {
            return invalid(error);
        }
    }

    @PUT
    @Path("/{taskId}/assignment")
    public Response assignTask(
            @PathParam("projectId") Long projectId,
            @PathParam("taskId") Long taskId,
            Map<String, Long> body) {

        try {
            if (body == null ||
                    !body.containsKey("assignedUserId")) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                        "error", "assignedUserId is required."
                    ))
                    .build();
            }

            // A null value clears the current assignment.
            Long employeeId = body.get("assignedUserId");

            Task updated = service.assignTask(
                projectId,
                taskId,
                employeeId
            );

            return Response.ok(updated).build();

        } catch (TaskManagerNotFoundException error) {
            return notFound(error);

        } catch (IllegalArgumentException error) {
            return invalid(error);
        }
    }

    private Response invalid(RuntimeException error) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("error", error.getMessage()))
            .build();
    }

    private Response notFound(RuntimeException error) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(Map.of("error", error.getMessage()))
            .build();
    }
}
