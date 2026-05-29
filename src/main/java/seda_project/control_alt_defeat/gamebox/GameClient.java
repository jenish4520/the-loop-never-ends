package seda_project.control_alt_defeat.gamebox;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

// Network client.
public class GameClient implements AutoCloseable {

    private static final long HEARTBEAT_INTERVAL_MS = 3000;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final Consumer<GameMessage> messageHandler;
    private final Runnable onConnectionLost;

    private volatile boolean running;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService heartbeatScheduler;
    private int playerNumber;

    // Constructor.
    public GameClient(Consumer<GameMessage> messageHandler, Runnable onConnectionLost) {
        this.messageHandler = messageHandler;
        this.onConnectionLost = onConnectionLost;
    }

    // Connect to host.
    public int connect(String hostAddress, int port, String playerName) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(hostAddress, port), 5000);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);

        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        running = true;

        // Send join.
        sendMessage(GameMessage.joinRequest(playerName));

        // Wait for accept.
        try {
            GameMessage response = (GameMessage) in.readObject();
            if (response.getType() != MessageType.JOIN_ACCEPTED) {
                throw new IOException("Expected JOIN_ACCEPTED, got: " + response.getType());
            }
            this.playerNumber = response.getPlayerNumber();
        } catch (ClassNotFoundException e) {
            throw new IOException("Protocol error: " + e.getMessage(), e);
        }

        // Start tasks.
        startReaderThread();
        startHeartbeat();

        return playerNumber;
    }

    // Send message.
    public synchronized void sendMessage(GameMessage message) {
        if (!running || out == null) return;
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
            Thread t = new Thread(r, "client-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            sendMessage(GameMessage.heartbeat());
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void handleConnectionLoss() {
        if (!running) return;
        running = false;
        onConnectionLost.run();
    }

    public boolean isRunning() { return running; }
    public int getPlayerNumber() { return playerNumber; }

    @Override
    public void close() {
        running = false;
        if (heartbeatScheduler != null) heartbeatScheduler.shutdownNow();
        executor.shutdownNow();
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
