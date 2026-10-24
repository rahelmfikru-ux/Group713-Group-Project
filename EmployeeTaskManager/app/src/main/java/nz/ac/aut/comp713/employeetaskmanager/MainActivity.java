package nz.ac.aut.comp713.employeetaskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String API_URL =
            "http://10.0.2.2:8080/project-task-manager/api/users/1/tasks";

    private LinearLayout taskContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        taskContainer = findViewById(R.id.taskContainer);

        loadTasksFromApi();

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    private void loadTasksFromApi() {

        new Thread(() -> {

            try {
                URL url = new URL(API_URL);

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream()
                        )
                );

                StringBuilder response = new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();

                Log.d(
                        "TASK_API",
                        "Response code: " + responseCode
                );

                Log.d(
                        "TASK_API",
                        "Response: " + response
                );

                JSONArray tasks =
                        new JSONArray(response.toString());

                runOnUiThread(() -> displayTasks(tasks));

            } catch (Exception e) {

                Log.e(
                        "TASK_API",
                        "API request failed",
                        e
                );
            }
        }).start();
    }

    private void displayTasks(JSONArray tasks) {

        taskContainer.removeAllViews();

        try {

            for (int i = 0; i < tasks.length(); i++) {

                JSONObject task =
                        tasks.getJSONObject(i);

                String title =
                        task.getString("title");

                String status =
                        task.getString("status");

                String description =
                        task.getString("description");

                String projectId =
                        task.getString("projectId");

                View taskView =
                        LayoutInflater
                                .from(this)
                                .inflate(
                                        R.layout.item_task,
                                        taskContainer,
                                        false
                                );

                TextView taskTitle =
                        taskView.findViewById(
                                R.id.taskTitle
                        );

                TextView taskStatus =
                        taskView.findViewById(
                                R.id.taskStatus
                        );

                Button viewTaskButton =
                        taskView.findViewById(
                                R.id.viewTaskButton
                        );

                taskTitle.setText(title);

                taskStatus.setText(
                        "Status: " + status
                );

                viewTaskButton.setOnClickListener(v -> {

                    Intent intent =
                            new Intent(
                                    MainActivity.this,
                                    TaskDetailsActivity.class
                            );

                    intent.putExtra(
                            "TASK_NAME",
                            title
                    );

                    intent.putExtra(
                            "PROJECT_NAME",
                            "Project " + projectId
                    );

                    intent.putExtra(
                            "TASK_DESCRIPTION",
                            description
                    );

                    startActivity(intent);
                });

                taskContainer.addView(taskView);
            }

        } catch (Exception e) {

            Log.e(
                    "TASK_API",
                    "Error displaying tasks",
                    e
            );
        }
    }
}