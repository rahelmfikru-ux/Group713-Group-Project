package nz.ac.aut.comp713.consumer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

/*
 * Data access for the consumer. The consumer is the only writer
 * of the activity_log table; the API never inserts into it.
 *
 * It uses the same H2 file as the API. AUTO_SERVER=TRUE lets two
 * separate processes open that file at the same time.
 */
public class ActivityLogRepository {

    private static final String DEFAULT_URL =
            "jdbc:h2:file:../COMP713-Project-Task-Manager/data/taskmanager"
            + ";MODE=MySQL;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";

    /* H2's SQLState for a unique-constraint violation. */
    private static final String DUPLICATE_KEY = "23505";

    private final String url;

    public ActivityLogRepository() {
        String fromEnv = System.getenv("DB_URL");
        url = fromEnv == null || fromEnv.isBlank() ? DEFAULT_URL : fromEnv;
    }

    public String getUrl() {
        return url;
    }

    public void createTable() throws SQLException {

        // event_id is UNIQUE: a repeated event cannot be stored twice.
        String sql = """
                CREATE TABLE IF NOT EXISTS activity_log (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    event_id VARCHAR(36) NOT NULL UNIQUE,
                    event_type VARCHAR(40) NOT NULL,
                    task_id BIGINT NOT NULL,
                    project_id BIGINT,
                    task_title VARCHAR(150),
                    status VARCHAR(20),
                    assigned_user_id BIGINT,
                    occurred_at TIMESTAMP,
                    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    attempts INT NOT NULL DEFAULT 1
                )
                """;

        try (Connection connection = open();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);
        }
    }

    /*
     * Returns true if the event was saved, false if it was a
     * duplicate (already processed). Other database errors are
     * thrown so the consumer can retry.
     */
    public boolean save(TaskEvent event, int attempt) throws SQLException {

        String sql = """
                INSERT INTO activity_log (
                    event_id, event_type, task_id, project_id,
                    task_title, status, assigned_user_id,
                    occurred_at, attempts
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = open();
             PreparedStatement insert = connection.prepareStatement(sql)) {

            insert.setString(1, event.eventId());
            insert.setString(2, event.type());
            insert.setLong(3, event.taskId());
            setNullableLong(insert, 4, event.projectId());
            insert.setString(5, event.title());
            insert.setString(6, event.status());
            setNullableLong(insert, 7, event.assignedUserId());
            insert.setTimestamp(8, event.occurredAt() == null
                    ? null
                    : Timestamp.from(event.occurredAt()));
            insert.setInt(9, attempt);

            insert.executeUpdate();
            return true;

        } catch (SQLException error) {

            if (DUPLICATE_KEY.equals(error.getSQLState())) {
                return false;
            }

            throw error;
        }
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(url, "sa", "");
    }

    private static void setNullableLong(
            PreparedStatement statement, int index, Long value)
            throws SQLException {

        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}