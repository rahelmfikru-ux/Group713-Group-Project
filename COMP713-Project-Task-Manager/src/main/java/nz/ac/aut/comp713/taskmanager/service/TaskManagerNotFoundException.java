
package nz.ac.aut.comp713.taskmanager.service;

public class TaskManagerNotFoundException
        extends RuntimeException {

    public TaskManagerNotFoundException(String message) {
        super(message);
    }
}
