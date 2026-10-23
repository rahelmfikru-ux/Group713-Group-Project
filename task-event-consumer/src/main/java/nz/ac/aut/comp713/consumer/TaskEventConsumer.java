package nz.ac.aut.comp713.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/*
 * Separately running consumer for the "task-events" queue
 * (advanced integration Path B).
 *
 * For every TASK_ASSIGNED / TASK_STATUS_CHANGED event it saves a
 * row in the activity_log table.
 *
 * Reliability behaviour:
 *  - Manual acknowledgement: a message is only acked after it has
 *    been saved (or deliberately given up on). If this process is
 *    stopped mid-way, RabbitMQ redelivers the unacked message.
 *  - Duplicate events: activity_log.event_id is UNIQUE, so an event
 *    delivered twice is detected, logged and acked without a
 *    second row being stored.
 *  - Retry: if saving fails, the event is republished with an
 *    x-attempt header, up to MAX_ATTEMPTS. After that it is moved
 *    to the "task-events.failed" queue so it does not block others.
 *  - Test hook: a task whose title contains "[fail]" simulates a
 *    processing failure, to demonstrate the retry path.
 *
 * Run from the task-event-consumer folder:  mvn compile exec:java
 */
public class TaskEventConsumer {

    static final String QUEUE = "task-events";
    static final String FAILED_QUEUE = "task-events.failed";

    private static final String ATTEMPT_HEADER = "x-attempt";
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2_000;

    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ActivityLogRepository repository;
    private Channel channel;

    public TaskEventConsumer(ActivityLogRepository repository) {
        this.repository = repository;
    }

    public static void main(String[] args) throws Exception {

        ActivityLogRepository repository = new ActivityLogRepository();
        repository.createTable();
        log("Activity log table ready (" + repository.getUrl() + ")");

        new TaskEventConsumer(repository).start();
    }

    private void start() throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(env("RABBITMQ_HOST", "localhost"));
        factory.setPort(Integer.parseInt(env("RABBITMQ_PORT", "5672")));
        factory.setUsername(env("RABBITMQ_USER", "guest"));
        factory.setPassword(env("RABBITMQ_PASSWORD", "guest"));

        // Reconnect by itself if the broker restarts.
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(5_000);

        Connection connection = factory.newConnection("task-event-consumer");
        channel = connection.createChannel();

        // Must match the API's declaration exactly.
        channel.queueDeclare(QUEUE, true, false, false, null);
        channel.queueDeclare(FAILED_QUEUE, true, false, false, null);

        // Take one message at a time.
        channel.basicQos(1);

        // autoAck = false: we ack ourselves after processing.
        channel.basicConsume(QUEUE, false,
                (tag, delivery) -> handle(delivery),
                tag -> log("Consumer cancelled by broker"));

        log("Connected to RabbitMQ at " + factory.getHost() + ":"
                + factory.getPort() + ", waiting for events on '"
                + QUEUE + "'. Press Ctrl+C to stop.");

        CountDownLatch stop = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                connection.close();
            } catch (Exception ignored) {
                // Shutting down anyway.
            }
            stop.countDown();
        }));

        stop.await();
    }

    private void handle(Delivery delivery) {

        long tag = delivery.getEnvelope().getDeliveryTag();
        AMQP.BasicProperties props = delivery.getProperties();
        String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
        int attempt = attemptOf(props);

        try {
            TaskEvent event;

            try {
                event = TaskEvent.parse(body, props.getMessageId());
            } catch (IllegalArgumentException invalid) {
                // Retrying can never fix a malformed message.
                moveToFailed(body, props, invalid.getMessage());
                channel.basicAck(tag, false);
                return;
            }

            try {
                if (event.title() != null && event.title().contains("[fail]")) {
                    throw new IllegalStateException(
                            "Simulated failure for task " + event.taskId());
                }

                boolean saved = repository.save(event, attempt);

                if (saved) {
                    log("Saved " + event.type() + " for task "
                            + event.taskId() + " (" + describe(event)
                            + ", event " + event.eventId()
                            + ", attempt " + attempt + ")");
                } else {
                    log("Duplicate event " + event.eventId()
                            + " ignored - already processed");
                }

                channel.basicAck(tag, false);

            } catch (Exception failure) {
                retryOrGiveUp(body, props, attempt, failure);
                channel.basicAck(tag, false);
            }

        } catch (Exception brokerError) {
            // Could not ack or republish (e.g. broker connection lost).
            // The message stays unacked and RabbitMQ will redeliver it.
            log("Broker error while handling message: "
                    + brokerError.getMessage());
        }
    }

    private void retryOrGiveUp(
            String body,
            AMQP.BasicProperties props,
            int attempt,
            Exception failure) throws Exception {

        if (attempt < MAX_ATTEMPTS) {

            log("Processing failed (attempt " + attempt + "/"
                    + MAX_ATTEMPTS + "): " + failure.getMessage()
                    + " - retrying in " + (RETRY_DELAY_MS / 1000) + "s");

            Thread.sleep(RETRY_DELAY_MS);

            Map<String, Object> headers = copyHeaders(props);
            headers.put(ATTEMPT_HEADER, attempt + 1);

            channel.basicPublish("", QUEUE,
                    props.builder().headers(headers).build(),
                    body.getBytes(StandardCharsets.UTF_8));

        } else {
            moveToFailed(body, props,
                    "Gave up after " + attempt + " attempts: "
                    + failure.getMessage());
        }
    }

    private void moveToFailed(
            String body,
            AMQP.BasicProperties props,
            String reason) throws Exception {

        Map<String, Object> headers = copyHeaders(props);
        headers.put("x-failure-reason", reason);

        channel.basicPublish("", FAILED_QUEUE,
                props.builder().headers(headers).build(),
                body.getBytes(StandardCharsets.UTF_8));

        log("Moved message to '" + FAILED_QUEUE + "': " + reason);
    }

    private static int attemptOf(AMQP.BasicProperties props) {
        Map<String, Object> headers = props.getHeaders();

        if (headers == null || headers.get(ATTEMPT_HEADER) == null) {
            return 1;
        }

        return ((Number) headers.get(ATTEMPT_HEADER)).intValue();
    }

    private static Map<String, Object> copyHeaders(AMQP.BasicProperties props) {
        return props.getHeaders() == null
                ? new HashMap<>()
                : new HashMap<>(props.getHeaders());
    }

    private static String describe(TaskEvent event) {
        if ("TASK_ASSIGNED".equals(event.type())) {
            return event.assignedUserId() == null
                    ? "unassigned"
                    : "assigned to user " + event.assignedUserId();
        }
        return "status " + event.status();
    }

    private static void log(String message) {
        System.out.println("[" + LocalTime.now().format(CLOCK) + "] " + message);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}