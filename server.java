import java.util.UUID;
import org.json.JSONObject;
import io.javalin.Javalin;
import io.javalin.websocket.WsHandler;
import io.javalin.websocket.WsMessageContext;
import io.javalin.websocket.WsConnectContext;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketServer {

    private static final int MAX_REQUESTS = 100;
    private static final int PERIOD_SECONDS = 900;

    private static List<WsHandler> waitingClients = new ArrayList<>();
    private static Map<String, WsHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(8080);

        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> handleConnect(ctx));
            ws.onMessage(ctx -> handleMessage(ctx));
            ws.onClose(ctx -> handleClose(ctx));
        });

        // Read the JSON file
        String jsonContent = readJsonFile("config.json");
        if (jsonContent != null) {
            try {
                // Parse the JSON content
                JSONObject jsonObject = new JSONObject(jsonContent);

                // Access the values from the JSON object
                int port = jsonObject.getInt("port");
                boolean debugMode = jsonObject.getBoolean("debugMode");

                // Use the values as needed
                app.port(port);
                app.enableDebugLogging(debugMode);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleConnect(WsConnectContext ctx) {
        String clientId = UUID.randomUUID().toString();

        if (clients.containsKey(clientId)) {
            System.out.println("client_id already in use, closing connection");
            ctx.getSession().close();
            return;
        }

        String origin = ctx.header("Origin");
        if (origin == null || !origin.equals("https://your-trusted-origin.com")) {
            ctx.getSession().close();
            return;
        }

        WsHandler wsHandler = ctx.getSession();
        clients.put(clientId, wsHandler);
        System.out.println("WebSocket connection opened from " + origin);

        if (waitingClients.isEmpty()) {
            waitingClients.add(wsHandler);
        } else {
            WsHandler partner = waitingClients.remove(0);
            partner.send(new JSONObject()
                    .put("type", "start chat")
                    .put("partner", clientId)
                    .toString());
            wsHandler.send(new JSONObject()
                    .put("type", "start chat")
                    .put("partner", partner.toString())
                    .toString());
        }
    }

    private static void handleMessage(WsMessageContext ctx) {
        String message = ctx.message();

        try {
            JSONObject json = new JSONObject(message);

            if (json.has("type")) {
                String type = json.getString("type");

                switch (type) {
                    case "chat":
                        String from = json.getString("from");
                        String to = json.getString("to");
                        String chatMessage = json.getString("message");

                        for (WsHandler client : clients.values()) {
                            if (client.equals(clients.get(to))) {
                                client.send(new JSONObject()
                                        .put("type", "chat")
                                        .put("from", from)
                                        .put("message", chatMessage)
                                        .toString());
                            }
                        }
                        break;

                    case "join":
                        String client = json.getString("client");
                        ctx.send(new JSONObject()
                                .put("type", "clients")
                                .put("clients", new ArrayList<>(clients.keySet()))
                                .toString());
                        for (WsHandler handler : clients.values()) {
                            handler.send(new JSONObject()
                                    .put("type", "join")
                                    .put("client", client)
                                    .toString());
                        }
                        break;

                    case "leave":
                        String leavingClient = json.getString("client");
                        clients.remove(leavingClient);
                        for (WsHandler handler : clients.values()) {
                            handler.send(new JSONObject()
                                    .put("type", "leave")
                                    .put("client", leavingClient)
                                    .toString());
                        }
                        break;

                    default:
                        System.out.println("Unknown message type: " + type);
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Invalid JSON: " + message);
        }
    }

    private static void handleClose(WsConnectContext ctx) {
        String clientId = getClientId(ctx);

        if (clientId != null) {
            clients.remove(clientId);
            waitingClients.removeIf(client -> getClientId(client) != null && getClientId(client).equals(clientId));
            System.out.println("WebSocket connection closed for " + clientId);
        }
    }

    private static String getClientId(WsHandler handler) {
        for (Map.Entry<String, WsHandler> entry : clients.entrySet()) {
            if (entry.getValue().equals(handler)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static String readJsonFile(String filePath) {
        try {
            // Read the JSON file
            byte[] jsonData = Files.readAllBytes(Paths.get(filePath));
            return new String(jsonData);
        } catch (Exception e) {
            System.out.println("Error reading JSON file: " + e.getMessage());
            return null;
        }
    }
}
