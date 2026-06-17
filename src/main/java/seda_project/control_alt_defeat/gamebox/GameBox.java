package seda_project.control_alt_defeat.gamebox;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

// this class handles the main game flow

public class GameBox {

    private static final Color BG_COLOR = Color.web("#1C1C1C");

    private final Stage stage;
    private MenuPanel menuPanel;
    private GamePanel gamePanel;

    // keep track of the game's current state
    private GameLogic gameLogic;
    private GameConfig currentConfig;

    // networking related variables
    private GameHost gameHost;
    private GameClient gameClient;
    private boolean isHost;
    private boolean isLocalMode;
    private int localPlayerNumber;
    private volatile boolean connected;
    private volatile GamePhase clientPhase = GamePhase.PLAYING;
    private volatile int clientActivePlayer = 1;
    private String player1Name = "Player 1";
    private String player2Name = "Player 2";

    private GameHub hub;
    private PauseTransition mismatchTimer;

    public GameBox(Stage stage, GameHub hub) {
        this.stage = stage;
        this.hub = hub;
        stage.setTitle("Memory Game - LAN Multiplayer");
        stage.setWidth(900);
        stage.setHeight(650);
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.centerOnScreen();

        stage.setOnCloseRequest(e -> cleanup());

        showMenu();
    }

    public void show() {
        stage.show();
    }

    // Show menu.
    private void showMenu() {
        menuPanel = new MenuPanel(
                this::startLocalGame,
                this::startHosting,
                this::startJoining,
                () -> hub.show());
        menuPanel.setBackground(new Background(new BackgroundFill(BG_COLOR, null, null)));
        Scene scene = new Scene(menuPanel, 900, 650);
        stage.setScene(scene);
    }

    private void showGame() {
        gamePanel = new GamePanel(
                localPlayerNumber,
                isLocalMode,
                player1Name,
                player2Name,
                this::onCardClicked,
                this::onRestartRequested,
                this::onBackToMenu);
        gamePanel.setBackground(new Background(new BackgroundFill(BG_COLOR, null, null)));
        Scene scene = new Scene(gamePanel, 900, 650);
        stage.setScene(scene);
    }

    private void startLocalGame(int matchSize, int deckSize) {
        isHost = true;
        isLocalMode = true;
        localPlayerNumber = 0;
        connected = true;
        clientPhase = GamePhase.PLAYING;
        clientActivePlayer = 1;
        player1Name = menuPanel.getPlayer1Name();
        player2Name = menuPanel.getPlayer2Name();
        currentConfig = new GameConfig(matchSize, deckSize);
        gameLogic = new GameLogic();
        gameLogic.initializeGame(currentConfig);
        GameState state = gameLogic.getState("Player 1's turn!");

        showGame();
        gamePanel.updateState(state);
    }

    private void startHosting(int matchSize, int deckSize) {
        isHost = true;
        isLocalMode = false;
        localPlayerNumber = 1;
        clientPhase = GamePhase.PLAYING;
        clientActivePlayer = 1;
        player1Name = menuPanel.getPlayer1Name();
        player2Name = "Player 2";
        currentConfig = new GameConfig(matchSize, deckSize);
        gameLogic = new GameLogic();

        int port = menuPanel.getPort();

        new Thread(() -> {
            try {
                gameHost = new GameHost(
                        this::onHostReceivedMessage,
                        this::onConnectionLost);

                Platform.runLater(() -> menuPanel.setStatus("Waiting for player 2 on port " + port +
                        " (IP: " + gameHost.getHostAddress() + ")...",
                        Color.web("#00d2ff")));

                gameHost.startAndWaitForClient(port);
                connected = true;

                player2Name = gameHost.getClientPlayerName();

                gameLogic.initializeGame(currentConfig);
                GameState state = gameLogic.getState("Game started! Player 1's turn.");

                gameHost.sendMessage(GameMessage.gameStart(state, currentConfig, player1Name, player2Name));

                Platform.runLater(() -> {
                    showGame();
                    gamePanel.updateState(state);
                });

            } catch (Exception e) {
                Platform.runLater(() -> menuPanel.setStatus("Error: " + e.getMessage(), Color.web("#e74c3c")));
            }
        }, "host-setup").start();
    }

    // process messages coming from the host
    private void onHostReceivedMessage(GameMessage msg) {
        switch (msg.getType()) {
            case CARD_CLICK -> handleRemoteCardClick(msg.getCardIndex());
            case RESTART_REQUEST -> handleRestartRequest();
            default -> {
            }
        }
    }

    // when a remote player clicks a card
    private void handleRemoteCardClick(int cardIndex) {
        if (!isHost)
            return;

        boolean valid = gameLogic.handleCardClick(cardIndex, 2);
        if (!valid)
            return;

        GameState state = gameLogic.getState();
        broadcastState(state);

        if (state.getPhase() == GamePhase.RESOLVING) {
            scheduleMismatchResolution();
        }
    }

    private void startJoining(String joinPlayerName, String hostAddress, int port) {
        isHost = false;
        isLocalMode = false;
        clientPhase = GamePhase.PLAYING;
        clientActivePlayer = 1;
        player2Name = (joinPlayerName == null || joinPlayerName.isBlank()) ? "Player 2" : joinPlayerName;

        new Thread(() -> {
            try {
                gameClient = new GameClient(
                        this::onClientReceivedMessage,
                        this::onConnectionLost);

                localPlayerNumber = gameClient.connect(hostAddress, port, player2Name);
                connected = true;

                Platform.runLater(() -> menuPanel.setStatus("Connected as Player " + localPlayerNumber +
                        ". Waiting for game to start...", Color.web("#ff6b9d")));

            } catch (Exception e) {
                Platform.runLater(
                        () -> menuPanel.setStatus("Connection failed: " + e.getMessage(), Color.web("#e74c3c")));
            }
        }, "client-connect").start();
    }

    private void onClientReceivedMessage(GameMessage msg) {
        switch (msg.getType()) {
            case GAME_START -> {
                currentConfig = msg.getConfig();
                clientPhase = GamePhase.PLAYING;
                clientActivePlayer = 1;
                String p1 = msg.getP1Name();
                String p2 = msg.getP2Name();
                if (p1 != null && !p1.isBlank())
                    player1Name = p1;
                if (p2 != null && !p2.isBlank())
                    player2Name = p2;
                Platform.runLater(() -> {
                    showGame();
                    GameState gs = msg.getGameState();
                    if (gs != null) {
                        clientPhase = gs.getPhase();
                        clientActivePlayer = gs.getActivePlayer();
                        gamePanel.updateState(gs);
                    }
                });
            }
            case STATE_UPDATE, GAME_END -> {
                GameState gs = msg.getGameState();
                if (gs != null) {
                    clientPhase = gs.getPhase();
                    clientActivePlayer = gs.getActivePlayer();
                }
                Platform.runLater(() -> {
                    if (gamePanel != null && gs != null) {
                        gamePanel.updateState(gs);
                    }
                });
            }
            case RESTART_CONFIRMED -> {
                currentConfig = msg.getConfig();
                clientPhase = GamePhase.PLAYING;
                clientActivePlayer = 1;
                String p1 = msg.getP1Name();
                String p2 = msg.getP2Name();
                if (p1 != null && !p1.isBlank())
                    player1Name = p1;
                if (p2 != null && !p2.isBlank())
                    player2Name = p2;
                GameState gs = msg.getGameState();
                Platform.runLater(() -> {
                    if (gamePanel != null && gs != null) {
                        gamePanel.updateState(gs);
                    }
                });
            }
            case ERROR -> {
                Platform.runLater(() -> {
                    if (gamePanel != null) {
                        gamePanel.showError(msg.getMessage());
                    }
                });
            }
            default -> {
            }
        }
    }

    private void onCardClicked(int cardIndex) {
        if (!connected)
            return;

        if (isLocalMode) {
            int activePlayer = gameLogic.getActivePlayer();
            boolean valid = gameLogic.handleCardClick(cardIndex, activePlayer);
            if (!valid)
                return;

            GameState state = gameLogic.getState();
            broadcastState(state);

            if (state.getPhase() == GamePhase.RESOLVING) {
                scheduleMismatchResolution();
            }
        } else if (isHost) {
            if (clientPhase == GamePhase.RESOLVING)
                return;
            if (clientActivePlayer != localPlayerNumber)
                return;
            boolean valid = gameLogic.handleCardClick(cardIndex, localPlayerNumber);
            if (!valid)
                return;

            GameState state = gameLogic.getState();
            clientPhase = state.getPhase();
            clientActivePlayer = state.getActivePlayer();
            broadcastState(state);

            if (state.getPhase() == GamePhase.RESOLVING) {
                scheduleMismatchResolution();
            }
        } else {
            if (clientPhase == GamePhase.RESOLVING)
                return;
            if (clientActivePlayer != localPlayerNumber)
                return;
            gameClient.sendMessage(GameMessage.cardClick(cardIndex));
        }
    }

    // sync the state with other players
    private void broadcastState(GameState state) {
        Platform.runLater(() -> {
            if (gamePanel != null) {
                gamePanel.updateState(state);
            }
        });

        if (isHost && !isLocalMode && gameHost != null) {
            if (state.getPhase() == GamePhase.GAME_OVER) {
                gameHost.sendMessage(GameMessage.gameEnd(state));
            } else {
                gameHost.sendMessage(GameMessage.stateUpdate(state));
            }
        }
    }

    private void scheduleMismatchResolution() {
        if (mismatchTimer != null) {
            mismatchTimer.stop();
        }

        mismatchTimer = new PauseTransition(Duration.seconds(1));
        mismatchTimer.setOnFinished(e -> {
            gameLogic.resolveMismatch();
            GameState newState = gameLogic.getState();
            clientPhase = newState.getPhase();
            clientActivePlayer = newState.getActivePlayer();
            broadcastState(newState);
        });
        mismatchTimer.play();
    }

    private void onRestartRequested() {
        if (isHost) {
            performRestart();
        } else {
            gameClient.sendMessage(GameMessage.restartRequest());
        }
    }

    private void handleRestartRequest() {
        performRestart();
    }

    private void performRestart() {
        if (!isHost || gameLogic == null)
            return;

        gameLogic.initializeGame(currentConfig);
        GameState state = gameLogic.getState("New round! Player 1's turn.");
        clientPhase = state.getPhase();
        clientActivePlayer = state.getActivePlayer();

        if (!isLocalMode && gameHost != null) {
            gameHost.sendMessage(GameMessage.restartConfirmed(state, currentConfig, player1Name, player2Name));
        }

        Platform.runLater(() -> {
            if (gamePanel != null) {
                gamePanel.updateState(state);
            }
        });
    }

    private void onConnectionLost() {
        connected = false;
        Platform.runLater(() -> {
            if (gamePanel != null) {
                gamePanel.disableInput();
                gamePanel.showError("Connection lost! Please restart the application.");
            } else if (menuPanel != null) {
                menuPanel.setStatus("Connection lost.", Color.web("#e74c3c"));
            }
        });
    }

    private void onBackToMenu() {
        cleanup();
        showMenu();
    }

    private void cleanup() {
        connected = false;
        if (mismatchTimer != null)
            mismatchTimer.stop();
        if (gameHost != null)
            gameHost.close();
        if (gameClient != null)
            gameClient.close();
        gameHost = null;
        gameClient = null;
        gameLogic = null;
    }
}
