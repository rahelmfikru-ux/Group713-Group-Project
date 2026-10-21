
package nz.ac.aut.comp713.taskmanager.repository;

import jakarta.enterprise.context.ApplicationScoped;
import nz.ac.aut.comp713.taskmanager.database.DatabaseConnection;
import nz.ac.aut.comp713.taskmanager.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class UserRepository {

    public List<User> findAll() {
        String sql = "SELECT id, username, display_name, role FROM users ORDER BY id";
        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rows = ps.executeQuery()) {

            while (rows.next()) {
                users.add(read(rows));
            }

            return users;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load users.", e);
        }
    }

    public User findById(Long id) {
        String sql = "SELECT id, username, display_name, role FROM users WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rows = ps.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to find user.", e);
        }
    }

    public User save(User user) {
        String sql = "INSERT INTO users(username, display_name, role) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getDisplayName());
            ps.setString(3, user.getRole());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("No user ID was returned.");
                }
                user.setId(keys.getLong(1));
            }

            return user;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new IllegalArgumentException("This username is already taken.");
            }
            throw new IllegalStateException("Unable to create user.", e);
        }
    }

    private User read(ResultSet row) throws SQLException {
        return new User(
                row.getLong("id"),
                row.getString("username"),
                row.getString("display_name"),
                row.getString("role")
        );
    }
}
