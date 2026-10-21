
package nz.ac.aut.comp713.taskmanager.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import nz.ac.aut.comp713.taskmanager.model.Task;
import nz.ac.aut.comp713.taskmanager.model.User;
import nz.ac.aut.comp713.taskmanager.repository.ProjectRepository;
import nz.ac.aut.comp713.taskmanager.repository.TaskRepository;
import nz.ac.aut.comp713.taskmanager.repository.UserRepository;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class TaskService {

    private static final Set<String> VALID_STATUSES =
            Set.of("TODO", "IN_PROGRESS", "DONE");

    private TaskRepository tasks;
    private ProjectRepository projects;
    private UserRepository users;

    protected TaskService() {
    }

    @Inject
    public TaskService(
            TaskRepository tasks,
            ProjectRepository projects,
            UserRepository users) {

        this.tasks = tasks;
        this.projects = projects;
        this.users = users;
    }

    public List<Task> getTasksForProject(Long projectId) {
        requireProject(projectId);
        return tasks.findByProjectId(projectId);
    }

    public Task createTask(Long projectId, Task task) {
        requireProject(projectId);

        if (task == null ||
                task.getTitle() == null ||
                task.getTitle().isBlank() ||
                task.getTitle().trim().length() > 150) {

            throw new IllegalArgumentException(
                "Task title must contain 1–150 characters."
            );
        }

        if (task.getDescription() != null &&
                task.getDescription().length() > 500) {

            throw new IllegalArgumentException(
                "Task description must be at most 500 characters."
            );
        }

        task.setTitle(task.getTitle().trim());

        String status = task.getStatus();

        if (status == null || status.isBlank()) {
            status = "TODO";
        }

        task.setStatus(normaliseStatus(status));

        validateEmployee(task.getAssignedUserId());

        task.setProjectId(projectId);
        task.setId(null);
        task.setAssigneeName(null);

        return tasks.save(task);
    }

    public Task updateTaskStatus(
            Long projectId,
            Long taskId,
            String status) {

        Task task = requireTask(projectId, taskId);

        String normalisedStatus = normaliseStatus(status);

        return tasks.updateStatus(
            task.getId(),
            normalisedStatus
        );
    }

    public Task assignTask(
            Long projectId,
            Long taskId,
            Long employeeId) {

        Task task = requireTask(projectId, taskId);

        validateEmployee(employeeId);

        return tasks.updateAssignee(
            task.getId(),
            employeeId
        );
    }

    private void validateEmployee(Long userId) {

        // Tasks may remain unassigned.
        if (userId == null) {
            return;
        }

        if (userId <= 0) {
            throw new IllegalArgumentException(
                "Valid employee ID required."
            );
        }

        User user = users.findById(userId);

        if (user == null) {
            throw new IllegalArgumentException(
                "Employee not found."
            );
        }

        if (!"EMPLOYEE".equals(user.getRole())) {
            throw new IllegalArgumentException(
                "Tasks can only be assigned to employees."
            );
        }
    }

    private String normaliseStatus(String status) {

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException(
                "Task status is required."
            );
        }

        String normalised =
                status.trim().toUpperCase(Locale.ROOT);

        if (!VALID_STATUSES.contains(normalised)) {
            throw new IllegalArgumentException(
                "Status must be TODO, IN_PROGRESS or DONE."
            );
        }

        return normalised;
    }

    private void requireProject(Long projectId) {

        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException(
                "Valid project ID required."
            );
        }

        if (!projects.existsById(projectId)) {
            throw new TaskManagerNotFoundException(
                "Project not found."
            );
        }
    }

    private Task requireTask(Long projectId, Long taskId) {

        requireProject(projectId);

        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException(
                "Valid task ID required."
            );
        }

        Task task = tasks.findById(taskId);

        if (task == null ||
                !projectId.equals(task.getProjectId())) {

            throw new TaskManagerNotFoundException(
                "Task not found in the selected project."
            );
        }

        return task;
    }
}
