package nz.ac.aut.comp713.taskmanager.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import nz.ac.aut.comp713.taskmanager.model.CheckIn;
import nz.ac.aut.comp713.taskmanager.model.Task;
import nz.ac.aut.comp713.taskmanager.model.User;
import nz.ac.aut.comp713.taskmanager.repository.CheckInRepository;
import nz.ac.aut.comp713.taskmanager.repository.ProjectRepository;
import nz.ac.aut.comp713.taskmanager.repository.TaskRepository;
import nz.ac.aut.comp713.taskmanager.repository.UserRepository;

import java.util.List;
import java.util.Locale;

/*
 * Location check-ins (the Android app's mobile-specific feature).
 *
 * Business rules:
 *  - Only the employee the task is assigned to can check in.
 *  - GPS check-ins need a valid latitude and longitude.
 *  - MANUAL check-ins are the fallback when location is not
 *    available; they need a typed location note instead.
 */
@ApplicationScoped
public class CheckInService {

    private static final int MAX_NOTE_LENGTH = 200;

    private CheckInRepository checkIns;
    private TaskRepository tasks;
    private ProjectRepository projects;
    private UserRepository users;

    protected CheckInService() {
    }

    @Inject
    public CheckInService(
            CheckInRepository checkIns,
            TaskRepository tasks,
            ProjectRepository projects,
            UserRepository users) {

        this.checkIns = checkIns;
        this.tasks = tasks;
        this.projects = projects;
        this.users = users;
    }

    public List<CheckIn> getCheckInsForProject(Long projectId) {
        requireProject(projectId);
        return checkIns.findByProjectId(projectId);
    }

    public CheckIn checkIn(Long projectId, Long taskId, CheckIn request) {

        Task task = requireTask(projectId, taskId);

        if (request == null) {
            throw new IllegalArgumentException("Check-in details are required.");
        }

        User user = request.getUserId() == null || request.getUserId() <= 0
                ? null
                : users.findById(request.getUserId());

        if (user == null || !"EMPLOYEE".equals(user.getRole())) {
            throw new IllegalArgumentException("A valid employee is required.");
        }

        if (!user.getId().equals(task.getAssignedUserId())) {
            throw new IllegalArgumentException(
                    "This task is not assigned to " + user.getDisplayName() + ".");
        }

        String source = request.getSource() == null
                ? ""
                : request.getSource().trim().toUpperCase(Locale.ROOT);

        String note = request.getLocationNote() == null
                ? null
                : request.getLocationNote().trim();

        if (note != null && note.isEmpty()) {
            note = null;
        }

        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException(
                    "Location note must be at most " + MAX_NOTE_LENGTH + " characters.");
        }

        CheckIn checkIn = new CheckIn();
        checkIn.setTaskId(task.getId());
        checkIn.setUserId(user.getId());
        checkIn.setLocationNote(note);

        if ("GPS".equals(source)) {

            Double lat = request.getLatitude();
            Double lon = request.getLongitude();

            if (lat == null || lon == null
                    || lat < -90 || lat > 90
                    || lon < -180 || lon > 180) {
                throw new IllegalArgumentException(
                        "GPS check-in needs a latitude (-90 to 90) and longitude (-180 to 180).");
            }

            Double accuracy = request.getAccuracyMetres();

            if (accuracy != null && accuracy < 0) {
                throw new IllegalArgumentException("Accuracy cannot be negative.");
            }

            checkIn.setSource("GPS");
            checkIn.setLatitude(lat);
            checkIn.setLongitude(lon);
            checkIn.setAccuracyMetres(accuracy);

        } else if ("MANUAL".equals(source)) {

            if (note == null) {
                throw new IllegalArgumentException(
                        "Manual check-in needs a location description.");
            }

            checkIn.setSource("MANUAL");

        } else {
            throw new IllegalArgumentException("Source must be GPS or MANUAL.");
        }

        return checkIns.save(checkIn);
    }

    private void requireProject(Long projectId) {

        if (projectId == null || projectId <= 0) {
            throw new IllegalArgumentException("Valid project ID required.");
        }

        if (!projects.existsById(projectId)) {
            throw new TaskManagerNotFoundException("Project not found.");
        }
    }

    private Task requireTask(Long projectId, Long taskId) {

        requireProject(projectId);

        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("Valid task ID required.");
        }

        Task task = tasks.findById(taskId);

        if (task == null || !projectId.equals(task.getProjectId())) {
            throw new TaskManagerNotFoundException(
                    "Task not found in the selected project.");
        }

        return task;
    }
}