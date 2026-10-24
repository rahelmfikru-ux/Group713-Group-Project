package nz.ac.aut.comp713.employeetaskmanager;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
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
}