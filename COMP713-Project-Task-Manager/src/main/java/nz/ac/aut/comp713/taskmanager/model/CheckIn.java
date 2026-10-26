package nz.ac.aut.comp713.taskmanager.model;

/*
 * An employee checking in at a task's location from the Android app.
 *
 * source is "GPS" (latitude/longitude from the device) or "MANUAL"
 * (the employee typed where they are because location was not
 * available or permission was refused).
 */
public class CheckIn {

    private Long id;
    private Long taskId;
    private Long userId;
    private String employeeName;
    private String source;
    private Double latitude;
    private Double longitude;
    private Double accuracyMetres;
    private String locationNote;
    private String checkedInAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getAccuracyMetres() { return accuracyMetres; }
    public void setAccuracyMetres(Double accuracyMetres) { this.accuracyMetres = accuracyMetres; }

    public String getLocationNote() { return locationNote; }
    public void setLocationNote(String locationNote) { this.locationNote = locationNote; }

    public String getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(String checkedInAt) { this.checkedInAt = checkedInAt; }
}