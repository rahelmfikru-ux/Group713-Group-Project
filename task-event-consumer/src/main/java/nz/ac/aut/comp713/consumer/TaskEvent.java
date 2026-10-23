package nz.ac.aut.comp713.consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Instant;

/*
 * One task event as published by the API's TaskEventPublisher.
 */
public record TaskEvent(
        String eventId,
        String type,
        long taskId,
        Long projectId,
        String title,
        String status,
        Long assignedUserId,
        Instant occurredAt) {

    /*
     * Throws IllegalArgumentException if the message is not a
     * valid task event. Such a message can never succeed, so the
     * consumer sends it straight to the failed queue.
     */
    public static TaskEvent parse(String body, String messageId) {

        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            String eventId = text(json, "eventId");
            if (eventId == null) {
                eventId = messageId;
            }

            String type = text(json, "type");
            Long taskId = number(json, "taskId");

            if (eventId == null || type == null || taskId == null) {
                throw new IllegalArgumentException(
                        "eventId, type and taskId are required");
            }

            String occurred = text(json, "occurredAt");

            return new TaskEvent(
                    eventId,
                    type,
                    taskId,
                    number(json, "projectId"),
                    text(json, "title"),
                    text(json, "status"),
                    number(json, "assignedUserId"),
                    occurred == null ? null : Instant.parse(occurred));

        } catch (IllegalArgumentException error) {
            throw error;

        } catch (RuntimeException error) {
            throw new IllegalArgumentException(
                    "Not a valid task event: " + error.getMessage(), error);
        }
    }

    private static String text(JsonObject json, String name) {
        JsonElement value = json.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static Long number(JsonObject json, String name) {
        JsonElement value = json.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsLong();
    }
}
