package nz.ac.aut.comp713.employeetaskmanager;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class TaskDetailsActivity extends AppCompatActivity {

    private static final String[] STATUSES = {"TODO", "IN_PROGRESS", "DONE"};

    private Spinner statusSpinner;
    private Button updateStatusButton;

    private long taskId;
    private long projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_task_details);

        TextView taskNameText = findViewById(R.id.taskNameText);
        TextView projectText = findViewById(R.id.projectText);
        TextView descriptionText = findViewById(R.id.descriptionText);
        statusSpinner = findViewById(R.id.statusSpinner);
        updateStatusButton = findViewById(R.id.updateStatusButton);

        // Task information passed from MainActivity
        taskId = getIntent().getLongExtra("TASK_ID", -1);
        projectId = getIntent().getLongExtra("PROJECT_ID", -1);

        String description = getIntent().getStringExtra("TASK_DESCRIPTION");
        String currentStatus = getIntent().getStringExtra("TASK_STATUS");

        taskNameText.setText(getIntent().getStringExtra("TASK_NAME"));
        projectText.setText("Project: " + getIntent().getStringExtra("PROJECT_NAME"));
        descriptionText.setText(description == null || description.isEmpty()
                ? "No description"
                : description);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, STATUSES);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        for (int i = 0; i < STATUSES.length; i++) {
            if (STATUSES[i].equals(currentStatus)) {
                statusSpinner.setSelection(i);
                break;
            }
        }

        updateStatusButton.setOnClickListener(v ->
                updateTaskStatus(statusSpinner.getSelectedItem().toString()));
    }

    private void updateTaskStatus(String status) {

        updateStatusButton.setEnabled(false);

        new Thread(() -> {
            String message;

            try {
                JSONObject body = new JSONObject();
                body.put("status", status);

                ApiClient.Result result = ApiClient.send(
                        "PUT",
                        "projects/" + projectId + "/tasks/" + taskId + "/status",
                        body.toString());

                // Show the server's own error text, e.g. "Task not found".
                message = result.isSuccess()
                        ? "Task status updated"
                        : result.errorMessage();

            } catch (IOException e) {
                message = ApiClient.networkError(e);

            } catch (JSONException e) {
                message = "Could not build the request.";
            }

            final String shown = message;

            runOnUiThread(() -> {
                updateStatusButton.setEnabled(true);
                Toast.makeText(this, shown, Toast.LENGTH_LONG).show();
            });

        }).start();
    }
}