package nz.ac.aut.comp713.employeetaskmanager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/*
 * Small helper for calling the Project & Task Manager REST API.
 * Call these methods from a background thread, never the UI thread.
 */
public final class ApiClient {

    /*
     * 10.0.2.2 is how the Android emulator reaches the computer it
     * runs on. On a real phone, change this to the computer's IP
     * address on the same Wi-Fi network (e.g. 192.168.1.20).
     */
    public static final String BASE_URL =
            "http://10.0.2.2:8080/project-task-manager/api/";

    private static final int TIMEOUT_MS = 5000;

    private ApiClient() {
    }

    public static final class Result {

        public final int code;
        public final String body;

        Result(int code, String body) {
            this.code = code;
            this.body = body;
        }

        public boolean isSuccess() {
            return code >= 200 && code < 300;
        }

        /* The API's {"error": "..."} message, or a generic one. */
        public String errorMessage() {
            try {
                String message = new JSONObject(body).optString("error", "");
                if (!message.isEmpty()) {
                    return message;
                }
            } catch (JSONException ignored) {
                // Not JSON - fall through to the generic message.
            }
            return "Request failed (HTTP " + code + ").";
        }
    }

    public static Result get(String path) throws IOException {
        return send("GET", path, null);
    }

    public static Result send(String method, String path, String jsonBody)
            throws IOException {

        HttpURLConnection connection =
                (HttpURLConnection) new URL(BASE_URL + path).openConnection();

        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");

            if (jsonBody != null) {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                OutputStream out = connection.getOutputStream();
                try {
                    out.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                } finally {
                    out.close();
                }
            }

            int code = connection.getResponseCode();

            // Error responses (4xx/5xx) are read from the error stream.
            InputStream stream = code >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();

            return new Result(code, readAll(stream));

        } finally {
            connection.disconnect();
        }
    }

    /* Friendly message when the server can't be reached at all. */
    public static String networkError(Exception error) {
        return "Can't reach the server. Check the API is running and try again.\n("
                + error.getClass().getSimpleName() + ")";
    }

    private static String readAll(InputStream stream) throws IOException {

        if (stream == null) {
            return "";
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8));

        try {
            StringBuilder text = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                text.append(line);
            }

            return text.toString();

        } finally {
            reader.close();
        }
    }
}