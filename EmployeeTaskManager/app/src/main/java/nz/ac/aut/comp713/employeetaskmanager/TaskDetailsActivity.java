package nz.ac.aut.comp713.employeetaskmanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/*
 * Task details: update the task's status, and check in at the task.
 *
 * Mobile-specific feature: the check-in uses the device location
 * (GPS or network). If permission is refused, location is turned
 * off, or no fix arrives within 10 seconds, the employee can type
 * where they are and check in manually instead.
 */
public class TaskDetailsActivity extends AppCompatActivity {

    private static final String[] STATUSES = {"TODO", "IN_PROGRESS", "DONE"};

    private static final int LOCATION_PERMISSION_REQUEST = 42;
    private static final long LOCATION_TIMEOUT_MS = 10_000;

    private Spinner statusSpinner;
    private Button updateStatusButton;
    private Button checkInGpsButton;
    private Button checkInManualButton;
    private EditText manualLocationInput;
    private TextView checkInResultText;

    private long taskId;
    private long projectId;
    private long userId;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private String locationProvider;
    private boolean waitingForLocation = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable locationTimeout = this::onLocationTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_task_details);

        TextView taskNameText = findViewById(R.id.taskNameText);
        TextView projectText = findViewById(R.id.projectText);
        TextView descriptionText = findViewById(R.id.descriptionText);
        statusSpinner = findViewById(R.id.statusSpinner);
        updateStatusButton = findViewById(R.id.updateStatusButton);
        checkInGpsButton = findViewById(R.id.checkInGpsButton);
        checkInManualButton = findViewById(R.id.checkInManualButton);
        manualLocationInput = findViewById(R.id.manualLocationInput);
        checkInResultText = findViewById(R.id.checkInResultText);

        // Task information passed from MainActivity
        taskId = getIntent().getLongExtra("TASK_ID", -1);
        projectId = getIntent().getLongExtra("PROJECT_ID", -1);
        userId = getIntent().getLongExtra("USER_ID", -1);

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

        checkInGpsButton.setOnClickListener(v -> startGpsCheckIn());
        checkInManualButton.setOnClickListener(v -> manualCheckIn());
    }

    @Override
    protected void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }

    // ---------- Status ----------

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

    // ---------- GPS check-in ----------

    private void startGpsCheckIn() {

        boolean fine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        boolean coarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        requestLocation();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != LOCATION_PERMISSION_REQUEST) {
            return;
        }

        boolean granted = false;

        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                granted = true;
            }
        }

        if (granted) {
            requestLocation();
        } else {
            offerManual("Location permission was refused. "
                    + "Type where you are below and check in manually.");
        }
    }

    @SuppressLint("MissingPermission") // checked in startGpsCheckIn()
    private void requestLocation() {

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager != null
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationProvider = LocationManager.GPS_PROVIDER;

        } else if (locationManager != null
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationProvider = LocationManager.NETWORK_PROVIDER;

        } else {
            offerManual("Location is turned off on this device. "
                    + "Type where you are below and check in manually.");
            return;
        }

        setCheckInBusy(true);
        checkInResultText.setText("Getting your location...");
        waitingForLocation = true;

        // Older Android versions need all four methods implemented.
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                onLocationReady(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        };

        locationManager.requestLocationUpdates(
                locationProvider, 0, 0, locationListener, Looper.getMainLooper());

        handler.postDelayed(locationTimeout, LOCATION_TIMEOUT_MS);
    }

    @SuppressLint("MissingPermission") // only reached after permission granted
    private void onLocationTimeout() {

        if (!waitingForLocation) {
            return;
        }

        // No fresh fix in time - use the last known one if the phone has it.
        Location last = locationManager == null
                ? null
                : locationManager.getLastKnownLocation(locationProvider);

        if (last != null) {
            onLocationReady(last);
        } else {
            stopLocationUpdates();
            offerManual("Couldn't get your location in time. "
                    + "Type where you are below and check in manually.");
        }
    }

    private void onLocationReady(Location location) {

        if (!waitingForLocation) {
            return;
        }

        stopLocationUpdates();

        try {
            JSONObject body = new JSONObject();
            body.put("userId", userId);
            body.put("source", "GPS");
            body.put("latitude", location.getLatitude());
            body.put("longitude", location.getLongitude());

            if (location.hasAccuracy()) {
                body.put("accuracyMetres", (double) location.getAccuracy());
            }

            String where = String.format(Locale.US, "GPS %.5f, %.5f",
                    location.getLatitude(), location.getLongitude());

            sendCheckIn(body, where);

        } catch (JSONException e) {
            setCheckInBusy(false);
            checkInResultText.setText("Could not build the request.");
        }
    }

    private void stopLocationUpdates() {

        waitingForLocation = false;
        handler.removeCallbacks(locationTimeout);

        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }

        locationListener = null;
    }

    // ---------- Manual check-in (fallback) ----------

    private void manualCheckIn() {

        String note = manualLocationInput.getText().toString().trim();

        if (note.isEmpty()) {
            manualLocationInput.setError("Type where you are first");
            manualLocationInput.requestFocus();
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("userId", userId);
            body.put("source", "MANUAL");
            body.put("locationNote", note);

            sendCheckIn(body, "\"" + note + "\"");

        } catch (JSONException e) {
            checkInResultText.setText("Could not build the request.");
        }
    }

    private void offerManual(String reason) {
        setCheckInBusy(false);
        checkInResultText.setText(reason);
        manualLocationInput.requestFocus();
    }

    // ---------- Send to API ----------

    private void sendCheckIn(JSONObject body, String where) {

        setCheckInBusy(true);
        checkInResultText.setText("Checking in...");

        new Thread(() -> {
            String message;

            try {
                ApiClient.Result result = ApiClient.send(
                        "POST",
                        "projects/" + projectId + "/tasks/" + taskId + "/check-ins",
                        body.toString());

                if (result.isSuccess()) {
                    String time = DateFormat.getTimeInstance(DateFormat.SHORT)
                            .format(new Date());
                    message = "Checked in at " + time + " (" + where + ").";
                } else {
                    message = "Check-in failed: " + result.errorMessage();
                }

            } catch (IOException e) {
                message = ApiClient.networkError(e);
            }

            final String shown = message;

            runOnUiThread(() -> {
                setCheckInBusy(false);
                checkInResultText.setText(shown);
            });

        }).start();
    }

    private void setCheckInBusy(boolean busy) {
        checkInGpsButton.setEnabled(!busy);
        checkInManualButton.setEnabled(!busy);
    }
}