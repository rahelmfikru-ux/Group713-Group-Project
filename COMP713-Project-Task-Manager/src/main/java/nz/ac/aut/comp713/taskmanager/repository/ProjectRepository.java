package nz.ac.aut.comp713.taskmanager.repository;

import jakarta.enterprise.context.ApplicationScoped;
import nz.ac.aut.comp713.taskmanager.database.DatabaseConnection;
import nz.ac.aut.comp713.taskmanager.model.Project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ProjectRepository {

    public List<Project> findAll() {

        List<Project> projects = new ArrayList<>();

        String sql = """
                SELECT id, name, description
                FROM projects
                ORDER BY id
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                Project project = new Project(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getString("description")
                );

                projects.add(project);
            }

            return projects;

        } catch (SQLException error) {
            throw new IllegalStateException(
                    "Unable to retrieve projects from the database.",
                    error
            );
        }
    }

    public Project save(Project project) {

        String sql = """
                INSERT INTO projects (name, description)
                VALUES (?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            statement.setString(1, project.getName());
            statement.setString(2, project.getDescription());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    project.setId(generatedKeys.getLong(1));
                } else {
                    throw new IllegalStateException(
                            "Project was created but no ID was returned."
                    );
                }
            }

            return project;

        } catch (SQLException error) {
            throw new IllegalStateException(
                    "Unable to save project to the database.",
                    error
            );
        }
    }

    public boolean existsById(Long id) {

        String sql = """
                SELECT 1
                FROM projects
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException error) {
            throw new IllegalStateException(
                    "Unable to check project in the database.",
                    error
            );
        }
    }
}