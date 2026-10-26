package nz.ac.aut.comp713.employeetaskmanager;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * Employee dashboard: choose who you are, then see the tasks a
 * manager has assigned to you. Tasks reload every time this screen
 * is shown again, so changes made on the details screen (or by the
 * manager on the website) appear when you come back.
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "employee_task_manager";
    private static final String PREF_EMPLOYEE_ID = "employee_id";

    private Spinner employeeSpinner;
    private TextView welcomeText;
    private TextView messageText;
    private Button retryButton;
    private LinearLayout taskContainer;

    private final List<Long> employeeIds = new ArrayList<>();
    private final List<String> employeeNames = new ArrayList<>();

    private long selectedEmployeeId = -1;
    private String selectedEmployeeName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        employeeSpinner = findViewById(R.id.employeeSpinner);
        welcomeText = findViewById(R.id.welcomeText);
        messageText = findViewById(R.id.messageText);
        retryButton = findViewById(R.id.retryButton);
        taskContainer = findViewById(R.id.taskContainer);

        retryButton.setOnClickListener(v -> {
            if (employeeIds.isEmpty()) {
                loadEmployees();
            } else {
                loadTasks();
            }
        });

        employeeSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view,
                            int position, long id) {

                        selectedEmployeeId = employeeIds.get(position);
                        selectedEmployeeName = employeeNames.get(position);

                        getSharedPreferences(PREFS, MODE_PRIVATE)
                                .edit()
                                .putLong(PREF_EMPLOYEE_ID, selectedEmployeeId)
                                .apply();

                        welcomeText.setText("Welcome, " + selectedEmployeeName);
                        loadTasks();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        loadEmployees();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Coming back from the details screen: show the latest data.
        if (selectedEmployeeId != -1) {
            loadTasks();
        }
    }

    // ---------- Employees ----------

    private void loadEmployees() {

        showMessage("Loading employees...", false);

        new Thread(() -> {
            try {
                ApiClient.Result result = ApiClient.get("users");

                if (!result.isSuccess()) {
                    runOnUiThread(() -> showMessage(result.errorMessage(), true));
                    return;
                }

                JSONArray users = new JSONArray(result.body);
                List<Long> ids = new ArrayList<>();
                List<String> names = new ArrayList<>();

                for (int i = 0; i < users.length(); i++) {
                    JSONObject user = users.getJSONObject(i);

                    if ("EMPLOYEE".equals(user.optString("role"))) {
                        ids.add(user.getLong("id"));
                        names.add(user.optString("displayName",
                                user.optString("username")));
                    }
                }

                runOnUiThread(() -> showEmployees(ids, names));

            } catch (IOException e) {
                runOnUiThread(() -> showMessage(ApiClient.networkError(e), true));

            } catch (JSONException e) {
                runOnUiThread(() -> showMessage(
                        "Unexpected response from the server.", true));
            }
        }).start();
    }

    private void showEmployees(List<Long> ids, List<String> names) {

        employeeIds.clear();
        employeeIds.addAll(ids);
        employeeNames.clear();
        employeeNames.addAll(names);

        if (ids.isEmpty()) {
            taskContainer.removeAllViews();
            showMessage("No employees yet. Ask your manager to create one "
                    + "on the website, then tap Retry.", true);
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        employeeSpinner.setAdapter(adapter);

        // Remember who used the app last time.
        long saved = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getLong(PREF_EMPLOYEE_ID, -1);
        int index = ids.indexOf(saved);

        hideMessage();

        // Triggers onItemSelected, which loads that employee's tasks.
        employeeSpinner.setSelection(index >= 0 ? index : 0);
    }

    // ---------- Tasks ----------

    private void loadTasks() {

        if (selectedEmployeeId == -1) {
            return;
        }

        final long employeeId = selectedEmployeeId;
        showMessage("Loading tasks...", false);

        new Thread(() -> {
            try {
                ApiClient.Result result =
                        ApiClient.get("users/" + employeeId + "/tasks");

                if (!result.isSuccess()) {
                    runOnUiThread(() -> {
                        taskContainer.removeAllViews();
                        showMessage(result.errorMessage(), true);
                    });
                    return;
                }

                JSONArray tasks = new JSONArray(result.body);

                runOnUiThread(() -> {
                    // Ignore a late answer for a previously selected employee.
                    if (employeeId == selectedEmployeeId) {
                        displayTasks(tasks);
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> showMessage(ApiClient.networkError(e), true));

            } catch (JSONException e) {
                runOnUiThread(() -> showMessage(
                        "Unexpected response from the server.", true));
            }
        }).start();
    }

    private void displayTasks(JSONArray tasks) {

        taskContainer.removeAllViews();

        if (tasks.length() == 0) {
            showMessage("No tasks assigned to " + selectedEmployeeName + " yet.", false);
            return;
        }

        hideMessage();

        for (int i = 0; i < tasks.length(); i++) {

            JSONObject task = tasks.optJSONObject(i);

            if (task == null) {
                continue;
            }

            // optString/optLong: a missing field (e.g. no description)
            // must not stop the rest of the list from showing.
            long taskId = task.optLong("id", -1);
            long projectId = task.optLong("projectId", -1);
            String title = task.optString("title", "Untitled task");
            String status = task.optString("status", "TODO");
            String description = task.optString("description", "");

            View taskView = LayoutInflater.from(this)
                    .inflate(R.layout.item_task, taskContainer, false);

            TextView taskTitle = taskView.findViewById(R.id.taskTitle);
            TextView taskStatus = taskView.findViewById(R.id.taskStatus);
            Button viewTaskButton = taskView.findViewById(R.id.viewTaskButton);

            taskTitle.setText(title);
            taskStatus.setText("Status: " + status.replace('_', ' '));

            viewTaskButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, TaskDetailsActivity.class);
                intent.putExtra("TASK_ID", taskId);
                intent.putExtra("PROJECT_ID", projectId);
                intent.putExtra("USER_ID", selectedEmployeeId);
                intent.putExtra("EMPLOYEE_NAME", selectedEmployeeName);
                intent.putExtra("TASK_NAME", title);
                intent.putExtra("PROJECT_NAME", "Project " + projectId);
                intent.putExtra("TASK_DESCRIPTION", description);
                intent.putExtra("TASK_STATUS", status);
                startActivity(intent);
            });

            taskContainer.addView(taskView);
        }
    }

    // ---------- Messages ----------

    private void showMessage(String text, boolean isError) {
        messageText.setText(text);
        messageText.setTextColor(isError
                ? Color.parseColor("#B00020")
                : Color.parseColor("#555555"));
        messageText.setVisibility(View.VISIBLE);
        retryButton.setVisibility(isError ? View.VISIBLE : View.GONE);
    }

    private void hideMessage() {
        messageText.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
    }
}