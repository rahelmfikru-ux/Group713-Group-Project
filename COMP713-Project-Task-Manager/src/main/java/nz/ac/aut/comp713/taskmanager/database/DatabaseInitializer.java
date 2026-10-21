
package nz.ac.aut.comp713.taskmanager.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void main(String[] args) {

        String createProjectsTable = """
                CREATE TABLE IF NOT EXISTS projects (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    description VARCHAR(500)
                )
                """;

        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(60) NOT NULL UNIQUE,
                    display_name VARCHAR(100) NOT NULL,
                    role VARCHAR(20) NOT NULL,
                    CONSTRAINT chk_user_role
                        CHECK (role IN ('MANAGER', 'EMPLOYEE'))
                )
                """;

        String createTasksTable = """
                CREATE TABLE IF NOT EXISTS tasks (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(150) NOT NULL,
                    description VARCHAR(500),
                    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
                    project_id BIGINT NOT NULL,
                    assigned_user_id BIGINT,
                    CONSTRAINT fk_tasks_project
                        FOREIGN KEY (project_id)
                        REFERENCES projects(id)
                        ON DELETE CASCADE
                )
                """;

        String updateExistingTasksTable = """
                ALTER TABLE tasks
                ADD COLUMN IF NOT EXISTS assigned_user_id BIGINT
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            // Create the projects table if it doesn't exist.
            statement.executeUpdate(createProjectsTable);
            System.out.println("Projects table ready.");

            // Create the users table if it doesn't exist.
            statement.executeUpdate(createUsersTable);
            System.out.println("Users table ready.");

            // Create the tasks table if it doesn't exist.
            statement.executeUpdate(createTasksTable);
            System.out.println("Tasks table ready.");

            // Update older databases to support task assignments.
            // Existing task records are preserved.
            statement.executeUpdate(updateExistingTasksTable);
            System.out.println("Task assignment column ready.");

            System.out.println(
                    "DATABASE INITIALISATION SUCCESSFUL"
            );

        } catch (SQLException error) {

            System.err.println(
                    "DATABASE INITIALISATION FAILED"
            );

            error.printStackTrace();
        }
    }
}
