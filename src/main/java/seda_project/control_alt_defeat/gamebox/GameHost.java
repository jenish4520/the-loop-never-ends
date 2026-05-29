package seda_project.control_alt_defeat.gamebox;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

// Network host that listens for a single client connection.
public class GameHost implements AutoCloseable {

    public static final int DEFAULT_PORT = 5555;
    private static final long HEARTBEAT_INTERVAL_MS = 3000;
    private static final long CONNECTION_TIMEOUT_MS = 10000;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final Consumer<GameMessage> messageHandler;
    private final Runnable onConnectionLost;

    private volatile boolean running;
    private String clientPlayerName = "Player 2";
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private ScheduledExecutorService heartbeatScheduler;

    // Constructor.
    public GameHost(Consumer<GameMessage> messageHandler, Runnable onConnectionLost) {
        this.messageHandler = messageHandler;
        this.onConnectionLost = onConnectionLost;
    }

    // Starts listening on port.
    public InetAddress startAndWaitForClient(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        serverSocket.setSoTimeout(0);

        running = true;
        clientSocket = serverSocket.accept();
        clientSocket.setTcpNoDelay(true);
        clientSocket.setKeepAlive(true);

        out = new ObjectOutputStream(clientSocket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(clientSocket.getInputStream());

        // Wait for join request.
        try {
            GameMessage joinMsg = (GameMessage) in.readObject();
            if (joinMsg.getType() != MessageType.JOIN_REQUEST) {
                throw new IOException("Expected JOIN_REQUEST, got: " + joinMsg.getType());
            }
            String name = joinMsg.getPlayerName();
            clientPlayerName = (name != null && !name.isBlank()) ? name : "Player 2";
        } catch (ClassNotFoundException e) {
            throw new IOException("Protocol error: " + e.getMessage(), e);
        }

        // Accept join.
        sendMessage(GameMessage.joinAccepted(2));

        // Start background tasks.
        startReaderThread();
        startHeartbeat();

        return clientSocket.getInetAddress();
    }

    // Send message.
    public synchronized void sendMessage(GameMessage message) {
        if (!running || out == null)
            return;
        try {
            out.writeObject(message);
            out.flush();
            out.reset();
        } catch (IOException e) {
            handleConnectionLoss();
        }
    }

    private void startReaderThread() {
        executor.submit(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    GameMessage msg = (GameMessage) in.readObject();
                    if (msg.getType() != MessageType.HEARTBEAT) {
                        messageHandler.accept(msg);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    if (running) {
                        handleConnectionLoss();
                    }
                    break;
                }
            }
        });
    }

    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "host-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            sendMessage(GameMessage.heartbeat());
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void handleConnectionLoss() {
        if (!running)
            return;
        running = false;
        onConnectionLost.run();
    }

    public boolean isRunning() {
        return running;
    }

    // Get client name.
    public String getClientPlayerName() {
        return clientPlayerName;
    }

    // Get host address.
    public String getHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    @Override
    public void close() {
        running = false;
        if (heartbeatScheduler != null)
            heartbeatScheduler.shutdownNow();
        executor.shutdownNow();
        try {
            if (in != null)
                in.close();
        } catch (IOException ignored) {
        }
        try {
            if (out != null)
                out.close();
        } catch (IOException ignored) {
        }
        try {
            if (clientSocket != null)
                clientSocket.close();
        } catch (IOException ignored) {
        }
        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException ignored) {
        }
    }
}
