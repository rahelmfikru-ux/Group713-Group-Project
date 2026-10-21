
package nz.ac.aut.comp713.taskmanager.model;

public class Task {

    private Long id;
    private String title;
    private String description;
    private String status;
    private Long projectId;
    private Long assignedUserId;
    private String assigneeName;

    public Task() {
    }

    // Original constructor kept for compatibility.
    public Task(
            Long id,
            String title,
            String description,
            String status,
            Long projectId) {

        this(id, title, description, status,
                projectId, null, null);
    }

    // New constructor supporting employee assignments.
    public Task(
            Long id,
            String title,
            String description,
            String status,
            Long projectId,
            Long assignedUserId,
            String assigneeName) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.projectId = projectId;
        this.assignedUserId = assignedUserId;
        this.assigneeName = assigneeName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Long assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }
}
