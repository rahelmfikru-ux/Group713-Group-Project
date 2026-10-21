package nz.ac.aut.comp713.taskmanager.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import nz.ac.aut.comp713.taskmanager.model.Project;
import nz.ac.aut.comp713.taskmanager.service.ProjectService;

import java.util.Map;

@Path("/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectResource {

    @Inject
    private ProjectService service;

    @GET
    public Response getAllProjects() {
        return Response.ok(service.getAllProjects()).build();
    }

    @POST
    public Response createProject(Project project) {
        try {
            Project created = service.createProject(project);

            return Response.status(Response.Status.CREATED)
                    .entity(created)
                    .build();

        } catch (IllegalArgumentException error) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", error.getMessage()))
                    .build();
        }
    }
}