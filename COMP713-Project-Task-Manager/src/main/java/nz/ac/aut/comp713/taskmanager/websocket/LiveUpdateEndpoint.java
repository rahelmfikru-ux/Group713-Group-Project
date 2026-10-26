package nz.ac.aut.comp713.taskmanager.websocket;

import jakarta.json.Json;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/*
 * WebSocket endpoint: ws://localhost:8080/project-task-manager/ws/updates
 *
 * Browsers (the manager website) connect here and receive a JSON
 * message whenever a task changes or an employee checks in, so the
 * page updates without refreshing or polling.
 *
 * The server only pushes; it ignores anything clients send.
 */
@ServerEndpoint("/ws/updates")
public class LiveUpdateEndpoint {

    private static final Logger LOG =
            Logger.getLogger(LiveUpdateEndpoint.class.getName());

    private static final Set<Session> SESSIONS = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session session) {
        SESSIONS.add(session);
        LOG.info("Live update client connected (" + SESSIONS.size() + " open)");

        send(session, Json.createObjectBuilder()
                .add("type", "CONNECTED")
                .add("sentAtMillis", System.currentTimeMillis())
                .build()
                .toString());
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        SESSIONS.remove(session);
        LOG.info("Live update client disconnected (" + SESSIONS.size() + " open)");
    }

    @OnError
    public void onError(Session session, Throwable error) {
        SESSIONS.remove(session);
        LOG.warning("Live update connection error: " + error.getMessage());
    }

    /* Sends the message to every open browser. */
    static void broadcast(String message) {
        for (Session session : SESSIONS) {
            send(session, message);
        }
    }

    static int openSessions() {
        return SESSIONS.size();
    }

    private static void send(Session session, String message) {

        if (!session.isOpen()) {
            SESSIONS.remove(session);
            return;
        }

        try {
            // One message at a time per connection.
            synchronized (session) {
                session.getBasicRemote().sendText(message);
            }
        } catch (IOException | IllegalStateException error) {
            // A broken connection must not affect the API request.
            SESSIONS.remove(session);
            LOG.warning("Dropped live update client: " + error.getMessage());
        }
    }
}