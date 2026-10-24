package nz.ac.aut.comp713.employeetaskmanager;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TaskDetailsActivity extends AppCompatActivity {

    private Spinner statusSpinner;
    private Button updateStatusButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_task_details);

        TextView taskNameText =
                findViewById(R.id.taskNameText);

        TextView projectText =
                findViewById(R.id.projectText);

        TextView descriptionText =
                findViewById(R.id.descriptionText);

        statusSpinner =
                findViewById(R.id.statusSpinner);

        updateStatusButton =
                findViewById(R.id.updateStatusButton);

        // Get task information passed from MainActivity

        int taskId =
                getIntent().getIntExtra(
                        "TASK_ID",
                        -1
                );

        int projectId =
                getIntent().getIntExtra(
                        "PROJECT_ID",
                        -1
                );

        String taskName =
                getIntent().getStringExtra(
                        "TASK_NAME"
                );

        String projectName =
                getIntent().getStringExtra(
                        "PROJECT_NAME"
                );

        String description =
                getIntent().getStringExtra(
                        "TASK_DESCRIPTION"
                );

        String currentStatus =
                getIntent().getStringExtra(
                        "TASK_STATUS"
                );

        // Display task information

        taskNameText.setText(taskName);

        projectText.setText(
                "Project: " + projectName
        );

        descriptionText.setText(description);

        // Status options

        String[] statuses = {
                "TODO",
                "IN_PROGRESS",
                "DONE"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        statuses
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        statusSpinner.setAdapter(adapter);

        // Select the task's current status

        if (currentStatus != null) {

            for (int i = 0; i < statuses.length; i++) {

                if (statuses[i].equals(currentStatus)) {

                    statusSpinner.setSelection(i);

                    break;
                }
            }
        }

        updateStatusButton.setOnClickListener(v -> {

            String selectedStatus =
                    statusSpinner
                            .getSelectedItem()
                            .toString();

            updateTaskStatus(
                    projectId,
                    taskId,
                    selectedStatus
            );
        });
    }

    private void updateTaskStatus(
            int projectId,
            int taskId,
            String status) {

        new Thread(() -> {

            try {

                URL url = new URL(
                        "http://10.0.2.2:8080/project-task-manager/api/projects/"
                                + projectId
                                + "/tasks/"
                                + taskId
                                + "/status"
                );

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("PUT");

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                connection.setDoOutput(true);

                String json =
                        "{\"status\":\"" + status + "\"}";

                OutputStream outputStream =
                        connection.getOutputStream();

                outputStream.write(
                        json.getBytes("UTF-8")
                );

                outputStream.flush();
                outputStream.close();

                int responseCode =
                        connection.getResponseCode();

                Log.d(
                        "TASK_UPDATE",
                        "Response code: " + responseCode
                );

                connection.disconnect();

                runOnUiThread(() -> {

                    if (responseCode >= 200 &&
                            responseCode < 300) {

                        Toast.makeText(
                                this,
                                "Task status updated",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        Toast.makeText(
                                this,
                                "Failed to update task",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

            } catch (Exception e) {

                Log.e(
                        "TASK_UPDATE",
                        "Status update failed",
                        e
                );

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "Error updating task",
                                Toast.LENGTH_SHORT
                        ).show()
                );
            }

        }).start();
    }
}