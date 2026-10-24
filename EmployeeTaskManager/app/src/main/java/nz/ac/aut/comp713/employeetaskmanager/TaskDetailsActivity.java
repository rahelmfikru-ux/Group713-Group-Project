package nz.ac.aut.comp713.employeetaskmanager;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

            Toast.makeText(
                    this,
                    "Task " + taskId +
                            " status changed to " +
                            selectedStatus,
                    Toast.LENGTH_SHORT
            ).show();
        });
    }
}