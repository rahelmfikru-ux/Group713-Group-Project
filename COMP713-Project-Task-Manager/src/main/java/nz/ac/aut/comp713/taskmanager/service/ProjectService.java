package nz.ac.aut.comp713.taskmanager.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nz.ac.aut.comp713.taskmanager.model.Project;
import nz.ac.aut.comp713.taskmanager.repository.ProjectRepository;

import java.util.List;

@ApplicationScoped
public class ProjectService {

    private ProjectRepository repository;

    /*
     * Required by the CDI container so this @ApplicationScoped
     * service can be proxied by Payara/Weld.
     */
    protected ProjectService() {
    }

    /*
     * Constructor injection follows the same pattern used in
     * the COMP713 Lab 5-6 PetService.
     */
    @Inject
    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }

    public List<Project> getAllProjects() {
        return repository.findAll();
    }

    public Project createProject(Project project) {

        if (project == null
                || project.getName() == null
                || project.getName().isBlank()) {

            throw new IllegalArgumentException(
                    "Project name is required."
            );
        }

        project.setName(
                project.getName().trim()
        );

        return repository.save(project);
    }
}