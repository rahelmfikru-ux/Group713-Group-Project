package nz.ac.aut.comp713.taskmanager.repository;

import jakarta.enterprise.context.ApplicationScoped;
import nz.ac.aut.comp713.taskmanager.database.DatabaseConnection;
import nz.ac.aut.comp713.taskmanager.model.ActivityEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/*
 * Read-only access to activity_log. The table is created and
 * written only by the task-event-consumer process.
 */
@ApplicationScoped
public class ActivityRepository {

    /* H2's SQLState for "table not found". */
    private static final String TABLE_NOT_FOUND = "42S02";

    public List<ActivityEntry> findRecent(int limit) {

        String sql = """
                SELECT a.id, a.event_id, a.event_type, a.task_id,
                       a.project_id, a.task_title, a.status,
                       a.assigned_user_id, u.display_name,
                       a.occurred_at, a.processed_at, a.attempts
                FROM activity_log a
                LEFT JOIN users u ON u.id = a.assigned_user_id
                ORDER BY a.id DESC
                LIMIT ?
                """;

        List<ActivityEntry> entries = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rows = ps.executeQuery()) {
                while (rows.next()) {
                    entries.add(read(rows));
                }
            }

            return entries;

        } catch (SQLException e) {

            // The consumer has never run, so its table doesn't exist yet.
            if (TABLE_NOT_FOUND.equals(e.getSQLState())) {
                return entries;
            }

            throw new IllegalStateException("Unable to load activity log.", e);
        }
    }

    private ActivityEntry read(ResultSet rows) throws SQLException {

        ActivityEntry entry = new ActivityEntry();

        entry.setId(rows.getLong("id"));
        entry.setEventId(rows.getString("event_id"));
        entry.setEventType(rows.getString("event_type"));
        entry.setTaskId(rows.getLong("task_id"));
        entry.setProjectId(nullableLong(rows, "project_id"));
        entry.setTaskTitle(rows.getString("task_title"));
        entry.setStatus(rows.getString("status"));
        entry.setAssignedUserId(nullableLong(rows, "assigned_user_id"));
        entry.setAssigneeName(rows.getString("display_name"));
        entry.setOccurredAt(isoTime(rows.getTimestamp("occurred_at")));
        entry.setProcessedAt(isoTime(rows.getTimestamp("processed_at")));
        entry.setAttempts(rows.getInt("attempts"));

        return entry;
    }

    private static Long nullableLong(ResultSet rows, String column)
            throws SQLException {

        long value = rows.getLong(column);
        return rows.wasNull() ? null : value;
    }

    private static String isoTime(Timestamp time) {
        return time == null ? null : time.toInstant().toString();
    }
}