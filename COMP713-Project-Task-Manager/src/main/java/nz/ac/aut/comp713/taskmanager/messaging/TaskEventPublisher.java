package nz.ac.aut.comp713.taskmanager.messaging;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import nz.ac.aut.comp713.taskmanager.model.Task;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Publishes task domain events to RabbitMQ (advanced integration
 * Path B). The separate consumer process reads them from the
 * "task-events" queue.
 *
 * Design decision: the database is the source of truth. A task
 * update is already saved before we publish, so if the broker is
 * down the API request still succeeds - the failure is logged and
 * the event is skipped. To avoid slowing every request while the
 * broker is down, we wait RETRY_AFTER_MS before trying to
 * reconnect.
 */
@ApplicationScoped
public class TaskEventPublisher {

    public static final String QUEUE = "task-events";

    public static final String TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String TASK_STATUS_CHANGED = "TASK_STATUS_CHANGED";

    private static final Logger LOG =
            Logger.getLogger(TaskEventPublisher.class.getName());

    private static final long RETRY_AFTER_MS = 10_000;
    private static final long CONFIRM_TIMEOUT_MS = 2_000;

    private final ConnectionFactory factory;

    private Connection connection;
    private Channel channel;
    private long lastFailureAt = 0;

    public TaskEventPublisher() {
        factory = new ConnectionFactory();
        factory.setHost(env("RABBITMQ_HOST", "localhost"));
        factory.setPort(Integer.parseInt(env("RABBITMQ_PORT", "5672")));
        factory.setUsername(env("RABBITMQ_USER", "guest"));
        factory.setPassword(env("RABBITMQ_PASSWORD", "guest"));
        factory.setConnectionTimeout(1_000);

        // We reconnect ourselves on the next publish instead.
        factory.setAutomaticRecoveryEnabled(false);
    }

    /*
     * Returns true if the broker confirmed it received the event.
     * Never throws - a broker problem must not fail the API call.
     */
    public synchronized boolean publish(String type, Task task) {

        String eventId = UUID.randomUUID().toString();

        try {
            Channel ch = openChannel();

            if (ch == null) {
                LOG.warning("Broker unavailable, event skipped: "
                        + type + " for task " + task.getId());
                return false;
            }

            byte[] body = toJson(eventId, type, task)
                    .getBytes(StandardCharsets.UTF_8);

            AMQP.BasicProperties properties =
                    new AMQP.BasicProperties.Builder()
                            .contentType("application/json")
                            .deliveryMode(2)          // persistent
                            .messageId(eventId)
                            .type(type)
                            .timestamp(new Date())
                            .build();

            ch.basicPublish("", QUEUE, properties, body);

            // Publisher confirm: wait for the broker's acknowledgement.
            ch.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);

            LOG.info("Published " + type + " for task "
                    + task.getId() + " (event " + eventId + ")");
            return true;

        } catch (Exception error) {

            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            LOG.log(Level.WARNING,
                    "Failed to publish " + type + " for task "
                    + task.getId() + ": " + error.getMessage());

            lastFailureAt = System.currentTimeMillis();
            close();
            return false;
        }
    }

    private Channel openChannel() throws Exception {

        if (channel != null && channel.isOpen()) {
            return channel;
        }

        // Broker failed recently - don't make this request wait.
        if (System.currentTimeMillis() - lastFailureAt < RETRY_AFTER_MS) {
            return null;
        }

        connection = factory.newConnection("task-manager-api");
        channel = connection.createChannel();

        // Durable queue; the consumer declares it the same way.
        channel.queueDeclare(QUEUE, true, false, false, null);
        channel.confirmSelect();

        LOG.info("Connected to RabbitMQ at "
                + factory.getHost() + ":" + factory.getPort());
        return channel;
    }

    private String toJson(String eventId, String type, Task task) {

        JsonObjectBuilder json = Json.createObjectBuilder()
                .add("eventId", eventId)
                .add("type", type)
                .add("occurredAt", Instant.now().toString())
                .add("taskId", task.getId())
                .add("projectId", task.getProjectId())
                .add("title", task.getTitle())
                .add("status", task.getStatus());

        if (task.getAssignedUserId() == null) {
            json.addNull("assignedUserId");
        } else {
            json.add("assignedUserId", task.getAssignedUserId());
        }

        return json.build().toString();
    }

    @PreDestroy
    public synchronized void close() {
        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (Exception ignored) {
            // Already closing - nothing useful to do.
        }

        connection = null;
        channel = null;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
