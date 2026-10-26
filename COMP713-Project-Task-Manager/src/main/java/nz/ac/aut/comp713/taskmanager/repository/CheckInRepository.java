package nz.ac.aut.comp713.taskmanager.repository;

import jakarta.enterprise.context.ApplicationScoped;
import nz.ac.aut.comp713.taskmanager.database.DatabaseConnection;
import nz.ac.aut.comp713.taskmanager.model.CheckIn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CheckInRepository {

    private static final String SELECT = """
            SELECT c.id, c.task_id, c.user_id, u.display_name,
                   c.source, c.latitude, c.longitude,
                   c.accuracy_metres, c.location_note, c.checked_in_at
            FROM check_ins c
            LEFT JOIN users u ON u.id = c.user_id
            """;

    public CheckIn save(CheckIn checkIn) {

        String sql = """
                INSERT INTO check_ins (
                    task_id, user_id, source, latitude, longitude,
                    accuracy_metres, location_note
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, checkIn.getTaskId());
            ps.setLong(2, checkIn.getUserId());
            ps.setString(3, checkIn.getSource());
            setNullableDouble(ps, 4, checkIn.getLatitude());
            setNullableDouble(ps, 5, checkIn.getLongitude());
            setNullableDouble(ps, 6, checkIn.getAccuracyMetres());
            ps.setString(7, checkIn.getLocationNote());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("No check-in ID returned.");
                }
                return findById(keys.getLong(1));
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save check-in.", e);
        }
    }

    public CheckIn findById(Long id) {

        String sql = SELECT + " WHERE c.id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rows = ps.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to find check-in.", e);
        }
    }

    /* Newest first, for every task in the project. */
    public List<CheckIn> findByProjectId(Long projectId) {

        String sql = SELECT + """
                JOIN tasks t ON t.id = c.task_id
                WHERE t.project_id = ?
                ORDER BY c.checked_in_at DESC, c.id DESC
                """;

        List<CheckIn> checkIns = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, projectId);

            try (ResultSet rows = ps.executeQuery()) {
                while (rows.next()) {
                    checkIns.add(read(rows));
                }
            }

            return checkIns;

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load check-ins.", e);
        }
    }

    private CheckIn read(ResultSet rows) throws SQLException {

        CheckIn checkIn = new CheckIn();

        checkIn.setId(rows.getLong("id"));
        checkIn.setTaskId(rows.getLong("task_id"));
        checkIn.setUserId(rows.getLong("user_id"));
        checkIn.setEmployeeName(rows.getString("display_name"));
        checkIn.setSource(rows.getString("source"));
        checkIn.setLatitude(nullableDouble(rows, "latitude"));
        checkIn.setLongitude(nullableDouble(rows, "longitude"));
        checkIn.setAccuracyMetres(nullableDouble(rows, "accuracy_metres"));
        checkIn.setLocationNote(rows.getString("location_note"));

        Timestamp time = rows.getTimestamp("checked_in_at");
        checkIn.setCheckedInAt(time == null ? null : time.toInstant().toString());

        return checkIn;
    }

    private static Double nullableDouble(ResultSet rows, String column)
            throws SQLException {

        double value = rows.getDouble(column);
        return rows.wasNull() ? null : value;
    }

    private static void setNullableDouble(
            PreparedStatement ps, int index, Double value) throws SQLException {

        if (value == null) {
            ps.setNull(index, Types.DOUBLE);
        } else {
            ps.setDouble(index, value);
        }
    }
}