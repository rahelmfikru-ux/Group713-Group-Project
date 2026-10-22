package nz.ac.aut.comp713.taskmanager.database;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;

import java.sql.SQLException;

/*
 * Runs once when Payara deploys the application, so a fresh
 * clone works without anyone running DatabaseInitializer by
 * hand first. This is what lets the local H2 file
 * (data/taskmanager.mv.db) stay out of the repository.
 */
@ApplicationScoped
public class SchemaStartup {

    public void onStartup(
            @Observes @Initialized(ApplicationScoped.class)
            Object event) {

        try {
            DatabaseInitializer.initialise();
            System.out.println("Database schema checked on startup.");

        } catch (SQLException error) {

            // Fail the deployment loudly rather than starting
            // an API that cannot reach its tables.
            throw new IllegalStateException(
                    "Database schema could not be created.",
                    error
            );
        }
    }
}