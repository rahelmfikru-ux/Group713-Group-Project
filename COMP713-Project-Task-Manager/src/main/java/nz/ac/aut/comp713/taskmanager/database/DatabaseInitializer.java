
package nz.ac.aut.comp713.taskmanager.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    private static final String CREATE_PROJECTS_TABLE = """
            CREATE TABLE IF NOT EXISTS projects (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                description VARCHAR(500)
            )
            """;

    private static final String CREATE_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(60) NOT NULL UNIQUE,
                display_name VARCHAR(100) NOT NULL,
                role VARCHAR(20) NOT NULL,
                CONSTRAINT chk_user_role
                    CHECK (role IN ('MANAGER', 'EMPLOYEE'))
            )
            """;

    private static final String CREATE_TASKS_TABLE = """
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

    private static final String UPDATE_EXISTING_TASKS_TABLE = """
            ALTER TABLE tasks
            ADD COLUMN IF NOT EXISTS assigned_user_id BIGINT
            """;

    private DatabaseInitializer() {
    }

    /*
     * Creates any missing tables. Every statement uses
     * IF NOT EXISTS, so this is safe to run on every startup
     * and existing records are preserved.
     *
     * Called automatically when the app is deployed
     * (see SchemaStartup), and can still be run by hand
     * through main().
     */
    public static void initialise() throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(CREATE_PROJECTS_TABLE);
            System.out.println("Projects table ready.");

            statement.executeUpdate(CREATE_USERS_TABLE);
            System.out.println("Users table ready.");

            statement.executeUpdate(CREATE_TASKS_TABLE);
            System.out.println("Tasks table ready.");

            // Update older databases to support task assignments.
            statement.executeUpdate(UPDATE_EXISTING_TASKS_TABLE);
            System.out.println("Task assignment column ready.");
        }
    }

    public static void main(String[] args) {

        try {
            initialise();

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
