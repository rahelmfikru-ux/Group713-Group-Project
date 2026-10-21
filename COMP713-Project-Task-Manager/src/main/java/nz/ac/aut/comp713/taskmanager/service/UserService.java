
package nz.ac.aut.comp713.taskmanager.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import nz.ac.aut.comp713.taskmanager.model.Task;
import nz.ac.aut.comp713.taskmanager.model.User;
import nz.ac.aut.comp713.taskmanager.repository.TaskRepository;
import nz.ac.aut.comp713.taskmanager.repository.UserRepository;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class UserService {

    private static final Set<String> ROLES =
            Set.of("MANAGER", "EMPLOYEE");

    private UserRepository users;
    private TaskRepository tasks;

    protected UserService() {
    }

    @Inject
    public UserService(
            UserRepository users,
            TaskRepository tasks) {

        this.users = users;
        this.tasks = tasks;
    }

    public List<User> getUsers() {
        return users.findAll();
    }

    public User createUser(User user) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User details are required."
            );
        }

        String username = user.getUsername();

        if (username == null ||
                !username.trim().matches("[A-Za-z0-9_-]{3,60}")) {

            throw new IllegalArgumentException(
                    "Username must be 3-60 letters, numbers, "
                    + "underscores or hyphens."
            );
        }

        String name = user.getDisplayName();

        if (name == null ||
                name.isBlank() ||
                name.trim().length() > 100) {

            throw new IllegalArgumentException(
                    "Display name must contain 1-100 characters."
            );
        }

        String role = user.getRole() == null
                ? ""
                : user.getRole()
                    .trim()
                    .toUpperCase(Locale.ROOT);

        if (!ROLES.contains(role)) {

            throw new IllegalArgumentException(
                    "Role must be MANAGER or EMPLOYEE."
            );
        }

        user.setUsername(
                username.trim().toLowerCase(Locale.ROOT)
        );

        user.setDisplayName(name.trim());
        user.setRole(role);
        user.setId(null);

        return users.save(user);
    }

    public List<Task> getEmployeeTasks(Long userId) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "Valid user ID required."
            );
        }

        User user = users.findById(userId);

        if (user == null) {
            throw new TaskManagerNotFoundException(
                    "User not found."
            );
        }

        if (!"EMPLOYEE".equals(user.getRole())) {
            throw new IllegalArgumentException(
                    "Selected user is not an employee."
            );
        }

        return tasks.findByAssigneeId(userId);
    }
}
