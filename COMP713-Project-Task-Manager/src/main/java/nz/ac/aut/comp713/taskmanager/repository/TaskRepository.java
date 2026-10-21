
package nz.ac.aut.comp713.taskmanager.repository;

import jakarta.enterprise.context.ApplicationScoped;
import nz.ac.aut.comp713.taskmanager.database.DatabaseConnection;
import nz.ac.aut.comp713.taskmanager.model.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TaskRepository {

    private static final String TASK_COLUMNS = """
        SELECT t.id, t.title, t.description, t.status,
               t.project_id, t.assigned_user_id,
               u.display_name AS assignee_name
        FROM tasks t
        LEFT JOIN users u ON u.id = t.assigned_user_id
        """;

    public List<Task> findByProjectId(Long projectId) {
        return queryList(
            TASK_COLUMNS + " WHERE t.project_id = ? ORDER BY t.id",
            projectId
        );
    }

    public List<Task> findByAssigneeId(Long userId) {
        return queryList(
            TASK_COLUMNS + " WHERE t.assigned_user_id = ? ORDER BY t.id",
            userId
        );
    }

    private List<Task> queryList(String sql, Long value) {
        List<Task> tasks = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, value);

            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    tasks.add(read(rows));
                }
            }

            return tasks;

        } catch (SQLException error) {
            throw new IllegalStateException(
                "Unable to retrieve tasks.", error
            );
        }
    }

    public Task findById(Long id) {
        String sql = TASK_COLUMNS + " WHERE t.id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }

        } catch (SQLException error) {
            throw new IllegalStateException(
                "Unable to find task.", error
            );
        }
    }

    public Task save(Task task) {
        String sql = """
            INSERT INTO tasks (
                title, description, status,
                project_id, assigned_user_id
            )
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                         sql, Statement.RETURN_GENERATED_KEYS
                     )) {

            statement.setString(1, task.getTitle());
            statement.setString(2, task.getDescription());
            statement.setString(3, task.getStatus());
            statement.setLong(4, task.getProjectId());

            if (task.getAssignedUserId() == null) {
                statement.setNull(5, Types.BIGINT);
            } else {
                statement.setLong(5, task.getAssignedUserId());
            }

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException(
                        "No task ID was returned."
                    );
                }

                task.setId(keys.getLong(1));
            }

            return findById(task.getId());

        } catch (SQLException error) {
            throw new IllegalStateException(
                "Unable to save task.", error
            );
        }
    }

    public Task updateStatus(Long taskId, String status) {
        String sql = """
            UPDATE tasks
            SET status = ?
            WHERE id = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setLong(2, taskId);

            int updated = statement.executeUpdate();

            return updated == 0 ? null : findById(taskId);

        } catch (SQLException error) {
            throw new IllegalStateException(
                "Unable to update task status.", error
            );
        }
    }

    public Task updateAssignee(Long taskId, Long userId) {
        String sql = """
            UPDATE tasks
            SET assigned_user_id = ?
            WHERE id = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            if (userId == null) {
                statement.setNull(1, Types.BIGINT);
            } else {
                statement.setLong(1, userId);
            }

            statement.setLong(2, taskId);

            int updated = statement.executeUpdate();

            return updated == 0 ? null : findById(taskId);

        } catch (SQLException error) {
            throw new IllegalStateException(
                "Unable to assign task.", error
            );
        }
    }

    private Task read(ResultSet rows) throws SQLException {
        long userId = rows.getLong("assigned_user_id");
        Long assignedUserId = rows.wasNull() ? null : userId;

        return new Task(
            rows.getLong("id"),
            rows.getString("title"),
            rows.getString("description"),
            rows.getString("status"),
            rows.getLong("project_id"),
            assignedUserId,
            rows.getString("assignee_name")
        );
    }
}
