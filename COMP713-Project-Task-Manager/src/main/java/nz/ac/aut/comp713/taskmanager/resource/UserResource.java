
package nz.ac.aut.comp713.taskmanager.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import nz.ac.aut.comp713.taskmanager.model.User;
import nz.ac.aut.comp713.taskmanager.service.UserService;
import nz.ac.aut.comp713.taskmanager.service.TaskManagerNotFoundException;

import java.util.Map;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    private UserService service;

    @GET
    public Response listUsers() {
        return Response.ok(service.getUsers()).build();
    }

    @POST
    public Response addUser(User user) {
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(service.createUser(user))
                    .build();

        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{userId}/tasks")
    public Response employeeTasks(
            @PathParam("userId") Long userId) {

        try {
            return Response.ok(service.getEmployeeTasks(userId))
                    .build();

        } catch (TaskManagerNotFoundException ex) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();

        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }
}
