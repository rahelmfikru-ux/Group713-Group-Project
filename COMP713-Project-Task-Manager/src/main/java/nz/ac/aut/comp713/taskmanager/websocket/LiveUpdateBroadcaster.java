package nz.ac.aut.comp713.taskmanager.websocket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import nz.ac.aut.comp713.taskmanager.model.CheckIn;
import nz.ac.aut.comp713.taskmanager.model.Task;

/*
 * Builds live-update messages and pushes them to every connected
 * browser through LiveUpdateEndpoint.
 *
 * Event types:
 *   TASK_UPDATED      change = CREATED, STATUS or ASSIGNMENT
 *   CHECK_IN_CREATED  an employee checked in at a task
 *
 * sentAtMillis lets the browser measure how long delivery took.
 */
@ApplicationScoped
public class LiveUpdateBroadcaster {

    public static final String CREATED = "CREATED";
    public static final String STATUS = "STATUS";
    public static final String ASSIGNMENT = "ASSIGNMENT";

    public void taskUpdated(String change, Task task) {

        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("type", "TASK_UPDATED")
                .add("change", change)
                .add("projectId", task.getProjectId())
                .add("taskId", task.getId())
                .add("title", task.getTitle())
                .add("status", task.getStatus());

        if (task.getAssignedUserId() == null) {
            json.addNull("assignedUserId");
        } else {
            json.add("assignedUserId", task.getAssignedUserId());
        }

        addText(json, "assigneeName", task.getAssigneeName());
        send(json);
    }

    public void checkInCreated(Task task, CheckIn checkIn) {

        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("type", "CHECK_IN_CREATED")
                .add("projectId", task.getProjectId())
                .add("taskId", task.getId())
                .add("title", task.getTitle())
                .add("source", checkIn.getSource());

        addText(json, "employeeName", checkIn.getEmployeeName());
        send(json);
    }

    private void send(JsonObjectBuilder json) {
        json.add("sentAtMillis", System.currentTimeMillis());
        LiveUpdateEndpoint.broadcast(json.build().toString());
    }

    private static void addText(JsonObjectBuilder json, String name, String value) {
        if (value == null) {
            json.addNull(name);
        } else {
            json.add(name, value);
        }
    }
}