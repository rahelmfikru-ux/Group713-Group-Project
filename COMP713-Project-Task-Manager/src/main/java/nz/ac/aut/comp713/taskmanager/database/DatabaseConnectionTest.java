package nz.ac.aut.comp713.taskmanager.database;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionTest {

    public static void main(String[] args) {

        try (Connection connection = DatabaseConnection.getConnection()) {

            System.out.println("DATABASE CONNECTION SUCCESSFUL");
            System.out.println("Connected database: " + connection.getCatalog());

        } catch (SQLException error) {

            System.err.println("DATABASE CONNECTION FAILED");
            System.err.println(error.getMessage());
        }
    }
}