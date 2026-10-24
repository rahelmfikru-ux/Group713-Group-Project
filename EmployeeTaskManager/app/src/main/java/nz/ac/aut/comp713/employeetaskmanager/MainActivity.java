package nz.ac.aut.comp713.employeetaskmanager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.util.Log;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final String API_URL =
            "http://10.0.2.2:8080/project-task-manager/api/users/1/tasks";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        loadTasksFromApi();

        findViewById(R.id.taskButton1).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TaskDetailsActivity.class);

            intent.putExtra("TASK_NAME", "Build Login Screen");
            intent.putExtra("PROJECT_NAME", "Mobile Application");
            intent.putExtra(
                    "TASK_DESCRIPTION",
                    "Implement the employee login functionality for the mobile application."
            );

            startActivity(intent);
        });

        findViewById(R.id.taskButton2).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TaskDetailsActivity.class);

            intent.putExtra("TASK_NAME", "Test REST API");
            intent.putExtra("PROJECT_NAME", "COMP713");
            intent.putExtra(
                    "TASK_DESCRIPTION",
                    "Test the REST API connection between the employee application and the task management system."
            );

            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
                        new InputStreamReader(connection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();

                Log.d("TASK_API", "Response code: " + responseCode);
                Log.d("TASK_API", "Response: " + response);

            } catch (Exception e) {
                Log.e("TASK_API", "API request failed", e);
            }

        }).start();
    }
}