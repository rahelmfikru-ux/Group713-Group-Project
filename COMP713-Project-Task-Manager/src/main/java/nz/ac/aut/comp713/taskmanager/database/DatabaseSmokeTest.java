package nz.ac.aut.comp713.taskmanager.database;

import nz.ac.aut.comp713.taskmanager.model.Project;
import nz.ac.aut.comp713.taskmanager.model.Task;
import nz.ac.aut.comp713.taskmanager.repository.ProjectRepository;
import nz.ac.aut.comp713.taskmanager.repository.TaskRepository;

public class DatabaseSmokeTest {

    public static void main(String[] args) {

        ProjectRepository projectRepository = new ProjectRepository();
        TaskRepository taskRepository = new TaskRepository();

        try {

            // Create a project
            Project project = new Project(
                    null,
                    "COMP713 Test Project",
                    "Testing MySQL persistence"
            );

            projectRepository.save(project);

            System.out.println(
                    "Project created with ID: " + project.getId()
            );

            // Create a task linked to that project
            Task task = new Task(
                    null,
                    "Test database task",
                    "Check that task persistence works",
                    "TODO",
                    project.getId()
            );

            taskRepository.save(task);

            System.out.println(
                    "Task created with ID: " + task.getId()
            );

            // Read projects
            System.out.println(
                    "Projects in database: "
                            + projectRepository.findAll().size()
            );

            // Read tasks for the project
            System.out.println(
                    "Tasks for test project: "
                            + taskRepository
                                    .findByProjectId(project.getId())
                                    .size()
            );

            // Update task status
            Task updatedTask =
                    taskRepository.updateStatus(
                            task.getId(),
                            "DONE"
                    );

            System.out.println(
                    "Updated task status: "
                            + updatedTask.getStatus()
            );

            System.out.println(
                    "DATABASE CRUD TEST SUCCESSFUL"
            );

        } catch (Exception error) {

            System.err.println(
                    "DATABASE CRUD TEST FAILED"
            );

            error.printStackTrace();
        }
    }
}