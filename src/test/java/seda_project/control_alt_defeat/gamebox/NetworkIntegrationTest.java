package seda_project.control_alt_defeat.gamebox;






import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(15)
class NetworkIntegrationTest {

    private GameHost host;
    private GameClient client;

    @AfterEach
    void tearDown() {
        if (client != null) client.close();
        if (host != null) host.close();
    }

    @Test
    void testHostClientConnection() throws Exception {
        BlockingQueue<GameMessage> clientMessages = new LinkedBlockingQueue<>();
        CountDownLatch connected = new CountDownLatch(1);

        // Start host in background
        host = new GameHost(msg -> {}, () -> {});
        Thread hostThread = new Thread(() -> {
            try {
                host.startAndWaitForClient(0); // 0 = auto port
            } catch (Exception e) {
                // May get closed during test teardown
            }
        });

        // We need to know the port, so let's use a fixed port
        int port = findFreePort();
        host = new GameHost(msg -> {}, () -> {});
        hostThread = new Thread(() -> {
            try {
                host.startAndWaitForClient(port);
                connected.countDown();
            } catch (Exception e) {
                // ignored
            }
        });
        hostThread.start();
        Thread.sleep(300); // Wait for server to start

        // Connect client
        client = new GameClient(clientMessages::add, () -> {});
        int playerNumber = client.connect("127.0.0.1", port, "TestClient");

        assertTrue(connected.await(5, TimeUnit.SECONDS), "Host should accept connection");
        assertEquals(2, playerNumber, "Client should be player 2");
        assertTrue(host.isRunning());
        assertTrue(client.isRunning());
    }

    @Test
    void testGameStartMessage() throws Exception {
        int port = findFreePort();
        BlockingQueue<GameMessage> clientMessages = new LinkedBlockingQueue<>();
        CountDownLatch connected = new CountDownLatch(1);

        host = new GameHost(msg -> {}, () -> {});
        new Thread(() -> {
            try {
                host.startAndWaitForClient(port);
                connected.countDown();
            } catch (Exception ignored) {}
        }).start();
        Thread.sleep(300);

        client = new GameClient(clientMessages::add, () -> {});
        client.connect("127.0.0.1", port, "TestClient");
        connected.await(5, TimeUnit.SECONDS);

        // Host sends game start
        GameLogic logic = new GameLogic();
        GameConfig config = new GameConfig(2, 16);
        logic.initializeGame(config);
        GameState state = logic.getState();

        host.sendMessage(GameMessage.gameStart(state, config, "Alice", "Bob"));

        // Client should receive it
        GameMessage received = clientMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(received, "Client should receive game start message");
        assertEquals(MessageType.GAME_START, received.getType());
        assertEquals(16, received.getGameState().getDeckSize());
        assertEquals(2, received.getConfig().getMatchSize());
    }

    @Test
    void testCardClickMessage() throws Exception {
        int port = findFreePort();
        BlockingQueue<GameMessage> hostMessages = new LinkedBlockingQueue<>();
        CountDownLatch connected = new CountDownLatch(1);

        host = new GameHost(hostMessages::add, () -> {});
        new Thread(() -> {
            try {
                host.startAndWaitForClient(port);
                connected.countDown();
            } catch (Exception ignored) {}
        }).start();
        Thread.sleep(300);

        client = new GameClient(msg -> {}, () -> {});
        client.connect("127.0.0.1", port, "TestClient");
        connected.await(5, TimeUnit.SECONDS);

        // Client sends card click
        client.sendMessage(GameMessage.cardClick(7));

        // Host should receive it
        GameMessage received = hostMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(received, "Host should receive card click");
        assertEquals(MessageType.CARD_CLICK, received.getType());
        assertEquals(7, received.getCardIndex());
    }

    @Test
    void testStateUpdateRoundTrip() throws Exception {
        int port = findFreePort();
        BlockingQueue<GameMessage> clientMessages = new LinkedBlockingQueue<>();
        CountDownLatch connected = new CountDownLatch(1);

        host = new GameHost(msg -> {}, () -> {});
        new Thread(() -> {
            try {
                host.startAndWaitForClient(port);
                connected.countDown();
            } catch (Exception ignored) {}
        }).start();
        Thread.sleep(300);

        client = new GameClient(clientMessages::add, () -> {});
        client.connect("127.0.0.1", port, "TestClient");
        connected.await(5, TimeUnit.SECONDS);

        // Create game state and send update
        GameLogic logic = new GameLogic();
        logic.initializeGame(new GameConfig(2, 16));
        logic.handleCardClick(0, 1);
        GameState state = logic.getState("Card opened");

        host.sendMessage(GameMessage.stateUpdate(state));

        GameMessage received = clientMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(MessageType.STATE_UPDATE, received.getType());

        GameState receivedState = received.getGameState();
        assertTrue(receivedState.getCards().get(0).isFaceUp(),
            "First card should be face up on client side");
        assertEquals("Card opened", receivedState.getStatusMessage());
    }

    @Test
    void testConnectionLossDetection() throws Exception {
        int port = findFreePort();
        AtomicReference<Boolean> hostLostConnection = new AtomicReference<>(false);
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch lostLatch = new CountDownLatch(1);

        host = new GameHost(msg -> {}, () -> {
            hostLostConnection.set(true);
            lostLatch.countDown();
        });
        new Thread(() -> {
            try {
                host.startAndWaitForClient(port);
                connected.countDown();
            } catch (Exception ignored) {}
        }).start();
        Thread.sleep(300);

        client = new GameClient(msg -> {}, () -> {});
        client.connect("127.0.0.1", port, "TestClient");
        connected.await(5, TimeUnit.SECONDS);

        // Close client abruptly
        client.close();

        // Host should detect connection loss (via heartbeat failure or read failure)
        boolean detected = lostLatch.await(10, TimeUnit.SECONDS);
        assertTrue(detected, "Host should detect client disconnection");
        assertTrue(hostLostConnection.get());
    }

    private int findFreePort() {
        try (var ss = new java.net.ServerSocket(0)) {
            return ss.getLocalPort();
        } catch (Exception e) {
            return 15555 + ThreadLocalRandom.current().nextInt(1000);
        }
    }
}
