package seda_project.control_alt_defeat.chess;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import seda_project.control_alt_defeat.gamebox.GameClient;
import seda_project.control_alt_defeat.gamebox.GameHost;
import seda_project.control_alt_defeat.gamebox.GameHub;
import seda_project.control_alt_defeat.gamebox.GameMessage;
import seda_project.control_alt_defeat.gamebox.MessageType;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChessApp {
    private final Stage stage;
    private final GameHub hub;
    private ChessBoard chessBoard;

    // ── LAN state ──────────────────────────────────
    private GameHost gameHost;
    private GameClient gameClient;
    /** Which color the local player controls in LAN mode. */
    private ChessBoard.PieceColor myColor;
    private boolean lanMode = false;

    private final ExecutorService bgExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "chess-bg");
        t.setDaemon(true);
        return t;
    });

    // ── Style constants ────────────────────────────
    private static final Color BG_COLOR = Color.web("#0f0f1e");
    private static final Color CARD_BG = Color.web("#1e2341");
    private static final Color CARD_HOVER = Color.web("#282d50");
    private static final Color ACCENT_CYAN = Color.web("#00d2ff");
    private static final Color ACCENT_PINK = Color.web("#ff6b9d");
    private static final Color ACCENT_PURPLE = Color.web("#a882ff");
    private static final Color TEXT_DIM = Color.web("#8c8caa");
    private static final Color TEXT_WHITE = Color.web("#f0f0fa");
    private static final Color SUCCESS_GREEN = Color.web("#2ecc71");
    private static final Color ERROR_RED = Color.web("#e74c3c");

    private final StackPane[][] squarePanes = new StackPane[11][11];
    private int selectedQ = -999;
    private int selectedR = -999;

    private Label turnLabel;
    private Label statusLabel;
    private final double radius = 33.0;

    public ChessApp(Stage stage, GameHub hub) {
        this.stage = stage;
        this.hub = hub;
    }

    /** Entry point — always shows the mode selection screen first. */
    public void show() {
        showModeScreen();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODE SELECTION SCREEN
    // ─────────────────────────────────────────────────────────────────────────

    private void showModeScreen() {
        closeLanConnection();

        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setBackground(new Background(new BackgroundFill(BG_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));

        Label title = new Label("CHESS");
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 52));
        title.setTextFill(ACCENT_PURPLE);

        Label subtitle = new Label("Glinski's Hexagonal Chess — Choose how you want to play");
        subtitle.setFont(Font.font("SansSerif", 18));
        subtitle.setTextFill(TEXT_DIM);

        HBox cardsRow = new HBox(24);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.setPadding(new Insets(40, 0, 0, 0));

        cardsRow.getChildren().addAll(
                buildModeCard("💻", "LOCAL GAME", "Two Players, One Screen", ACCENT_PURPLE, this::startLocalGame),
                buildModeCard("⌂", "HOST A GAME", "Create a LAN Session", ACCENT_CYAN, this::showHostScreen),
                buildModeCard("→", "JOIN A GAME", "Connect to a Host", ACCENT_PINK, this::showJoinScreen),
                buildModeCard("", "VS COMPUTER", "Play Against the Chess Bot", SUCCESS_GREEN, this::startBotGame));

        Button backBtn = new Button("← Back to Main Menu");
        backBtn.setStyle("-fx-background-color: " + toHex(CARD_BG) + "; -fx-text-fill: " + toHex(TEXT_DIM)
                + "; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 20;");
        backBtn.setCursor(Cursor.HAND);
        backBtn.setOnAction(e -> hub.show());

        HBox backRow = new HBox(backBtn);
        backRow.setAlignment(Pos.CENTER);
        backRow.setPadding(new Insets(30, 0, 0, 0));

        root.getChildren().addAll(title, subtitle, cardsRow, backRow);

        Scene scene = new Scene(root, 1050, 650);
        stage.setTitle("Chess — Select Mode");
        stage.setScene(scene);
    }

    private VBox buildModeCard(String icon, String title, String desc, Color accent, Runnable onClick) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(230, 230);
        card.setCursor(Cursor.HAND);

        Background normalBg = new Background(new BackgroundFill(CARD_BG, new CornerRadii(20), Insets.EMPTY));
        Background hoverBg = new Background(new BackgroundFill(CARD_HOVER, new CornerRadii(20), Insets.EMPTY));
        card.setBackground(normalBg);
        card.setStyle("-fx-border-color: " + toHex(accent.darker().darker())
                + "; -fx-border-width: 1.5; -fx-border-radius: 20;");

        Label iconLabel = lbl(icon, 48, FontWeight.NORMAL, accent);
        Label titleLabel = lbl(title, 17, FontWeight.BOLD, accent);
        Label descLabel = lbl(desc, 13, FontWeight.NORMAL, TEXT_DIM);
        card.getChildren().addAll(iconLabel, titleLabel, descLabel);

        card.setOnMouseEntered(e -> {
            card.setBackground(hoverBg);
            card.setStyle("-fx-border-color: " + toHex(accent) + "; -fx-border-width: 2.5; -fx-border-radius: 20;");
        });
        card.setOnMouseExited(e -> {
            card.setBackground(normalBg);
            card.setStyle("-fx-border-color: " + toHex(accent.darker().darker())
                    + "; -fx-border-width: 1.5; -fx-border-radius: 20;");
        });
        card.setOnMouseClicked(e -> onClick.run());
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOCAL GAME
    // ─────────────────────────────────────────────────────────────────────────

    private void startLocalGame() {
        lanMode = false;
        myColor = null;
        botMode = false;
        chessBoard = new ChessBoard();
        selectedQ = -999;
        selectedR = -999;
        showLocalGameScreen();
    }

    // ── Bot mode ───────────────────────────────────────────────────────────────
    private boolean botMode = false;

    private void startBotGame() {
        lanMode = false;
        myColor = null;
        botMode = true;
        chessBoard = new ChessBoard();
        selectedQ = -999;
        selectedR = -999;
        showLocalGameScreen();
    }

    /** Called after every move in local/bot mode to let the bot respond. */
    private void maybeTriggerBot() {
        if (!botMode)
            return;
        if (chessBoard.getCurrentTurn() != ChessBoard.PieceColor.BLACK)
            return;
        if (!chessBoard.hasValidMoves(ChessBoard.PieceColor.BLACK))
            return;

        bgExecutor.submit(() -> {
            // small delay so the player can see their own move first
            try {
                Thread.sleep(350);
            } catch (InterruptedException ignored) {
            }
            Move botMove = chessBoard.getBotMove(ChessBoard.PieceColor.BLACK);
            if (botMove != null) {
                Platform.runLater(() -> {
                    chessBoard.makeMove(botMove.fromQ, botMove.fromR, botMove.toQ, botMove.toR);
                    // Bot always promotes to Queen
                    if (chessBoard.hasPendingPromotion()) {
                        chessBoard.completePromotion(ChessBoard.PieceType.QUEEN);
                    }
                    selectedQ = -999;
                    selectedR = -999;
                    updateBoardDisplay();
                });
            }
        });
    }

    // ────────────────────────────────────────────────────────────────────────────────
    // PROMOTION DIALOG
    // ────────────────────────────────────────────────────────────────────────────────

    /**
     * Shows a promotion-choice overlay over the current scene.
     * 
     * @param promotingColor the color doing the promotion
     * @param isLan          true if we should NOT call maybeTriggerBot afterwards
     */
    private void showPromotionDialog(ChessBoard.PieceColor promotingColor, boolean isLan) {
        // Build an overlay pane
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.72);");

        VBox box = new VBox(18);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(36));
        box.setMaxWidth(420);
        box.setBackground(new Background(new BackgroundFill(CARD_BG, new CornerRadii(18), Insets.EMPTY)));
        box.setStyle("-fx-border-color: " + toHex(ACCENT_PURPLE) + "; -fx-border-radius: 18; -fx-border-width: 2;");

        Label title = lbl("PAWN PROMOTION", 22, FontWeight.BOLD, ACCENT_PURPLE);
        Label sub = lbl("Choose a piece to promote to:", 14, FontWeight.NORMAL, TEXT_DIM);

        HBox btnRow = new HBox(14);
        btnRow.setAlignment(Pos.CENTER);

        // The 4 legal promotion choices
        record PromOpt(String icon, String name, ChessBoard.PieceType type) {
        }
        java.util.List<PromOpt> opts = java.util.List.of(
                new PromOpt("♛", "Queen", ChessBoard.PieceType.QUEEN),
                new PromOpt("♜", "Rook", ChessBoard.PieceType.ROOK),
                new PromOpt("♝", "Bishop", ChessBoard.PieceType.BISHOP),
                new PromOpt("♞", "Knight", ChessBoard.PieceType.KNIGHT));

        // Grab a reference to the current root so we can remove the overlay
        javafx.scene.Parent sceneRoot = stage.getScene().getRoot();
        StackPane fullOverlay; // wrapped in scene's root
        if (sceneRoot instanceof StackPane sp) {
            fullOverlay = sp;
        } else {
            // Wrap existing root inside a StackPane
            fullOverlay = new StackPane(sceneRoot);
            stage.getScene().setRoot(fullOverlay);
        }
        fullOverlay.getChildren().add(overlay);
        overlay.getChildren().add(box);

        for (PromOpt opt : opts) {
            VBox card = new VBox(4);
            card.setAlignment(Pos.CENTER);
            card.setPrefSize(80, 90);
            card.setCursor(Cursor.HAND);
            card.setBackground(new Background(new BackgroundFill(
                    CARD_BG.brighter(), new CornerRadii(10), Insets.EMPTY)));
            card.setStyle("-fx-border-color: " + toHex(ACCENT_PURPLE.darker())
                    + "; -fx-border-radius: 10; -fx-border-width: 1.5;");

            javafx.scene.Node iconNode;
            Piece dummyPiece = new Piece(opt.type(), promotingColor);
            String imagePath = "/images/chess/" + dummyPiece.getImageName();
            java.net.URL imgUrl = getClass().getResource(imagePath);
            if (imgUrl != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(imgUrl.toExternalForm()));
                iv.setFitWidth(40);
                iv.setFitHeight(40);
                iv.setPreserveRatio(true);
                iconNode = iv;
            } else {
                Label iconLbl = new Label(opt.icon());
                iconLbl.setFont(Font.font("SansSerif", 34));
                iconLbl.setTextFill(promotingColor == ChessBoard.PieceColor.WHITE ? Color.WHITE : Color.DARKGRAY);
                iconNode = iconLbl;
            }

            Label nameLbl = lbl(opt.name(), 12, FontWeight.BOLD, TEXT_DIM);
            card.getChildren().addAll(iconNode, nameLbl);

            card.setOnMouseEntered(e -> card.setStyle("-fx-border-color: " + toHex(ACCENT_PURPLE)
                    + "; -fx-border-radius: 10; -fx-border-width: 2;"));
            card.setOnMouseExited(e -> card.setStyle("-fx-border-color: " + toHex(ACCENT_PURPLE.darker())
                    + "; -fx-border-radius: 10; -fx-border-width: 1.5;"));

            card.setOnMouseClicked(e -> {
                fullOverlay.getChildren().remove(overlay);
                chessBoard.completePromotion(opt.type());
                selectedQ = -999;
                selectedR = -999;
                updateBoardDisplay();
                if (!isLan)
                    maybeTriggerBot();
            });
            btnRow.getChildren().add(card);
        }

        box.getChildren().addAll(title, sub, btnRow);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HOST SCREEN
    // ─────────────────────────────────────────────────────────────────────────

    private void showHostScreen() {
        closeLanConnection();
        chessBoard = new ChessBoard();
        gameHost = new GameHost(this::handleNetworkMessage, this::handleConnectionLost);

        String hostIp = gameHost.getHostAddress();

        // ── UI ──────────────────────────────────────
        VBox root = new VBox(22);
        root.setAlignment(Pos.CENTER);
        root.setBackground(new Background(new BackgroundFill(BG_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));

        Label title = lbl("HOST A GAME", 38, FontWeight.BOLD, ACCENT_CYAN);

        // IP info card
        VBox ipCard = new VBox(8);
        ipCard.setAlignment(Pos.CENTER);
        ipCard.setPadding(new Insets(22));
        ipCard.setMaxWidth(500);
        ipCard.setBackground(new Background(new BackgroundFill(CARD_BG, new CornerRadii(14), Insets.EMPTY)));
        ipCard.setStyle("-fx-border-color: #00d2ff55; -fx-border-radius: 14; -fx-border-width: 1.5;");

        Label ipTitle = lbl("YOUR LAN IP ADDRESS", 12, FontWeight.BOLD, TEXT_DIM);
        Label ipValue = lbl(hostIp, 32, FontWeight.BOLD, ACCENT_CYAN);
        
        Label errorLabel = new Label("");
        errorLabel.setFont(Font.font("SansSerif", 14));
        errorLabel.setTextFill(ERROR_RED);

        // ── Background: wait for client ──────────────
        bgExecutor.execute(() -> {
            try {
                gameHost.startAndWaitForClient(5555);

                // Client connected — send initial board state
                String boardState = chessBoard.serializeBoard();
                gameHost.sendMessage(GameMessage.chessAction("START:" + boardState));

                Platform.runLater(() -> {
                    myColor = ChessBoard.PieceColor.WHITE;
                    lanMode = true;
                    selectedQ = -999;
                    selectedR = -999;
                    showLanGameScreen("Host — Playing as White");
                });
            } catch (IOException e) {
                Platform.runLater(() -> errorLabel.setText("Error: " + e.getMessage()));
            }
        });
        
        Label portInfo = lbl("Port  5555", 14, FontWeight.NORMAL, TEXT_DIM);
        Label shareHint = lbl("Share this IP with your opponent", 14, FontWeight.NORMAL, TEXT_DIM);
        ipCard.getChildren().addAll(ipTitle, ipValue, portInfo, shareHint);

        Label waitLabel = lbl("⏳  Waiting for opponent to connect…", 18, FontWeight.BOLD, ACCENT_PURPLE);

        Button cancelBtn = new Button("← Cancel");
        cancelBtn.setStyle("-fx-background-color: " + toHex(CARD_BG) + "; -fx-text-fill: " + toHex(TEXT_DIM)
                + "; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 24;");
        cancelBtn.setCursor(Cursor.HAND);
        cancelBtn.setOnAction(e -> showModeScreen());

        root.getChildren().addAll(title, ipCard, waitLabel, errorLabel, cancelBtn);
        Scene scene = new Scene(root, 1050, 650);
        stage.setTitle("Chess — Host a Game");
        stage.setScene(scene);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JOIN SCREEN
    // ─────────────────────────────────────────────────────────────────────────

    private void showJoinScreen() {
        closeLanConnection();

        VBox root = new VBox(22);
        root.setAlignment(Pos.CENTER);
        root.setBackground(new Background(new BackgroundFill(BG_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));

        Label title = lbl("JOIN A GAME", 38, FontWeight.BOLD, ACCENT_PINK);

        // Input card
        VBox inputCard = new VBox(14);
        inputCard.setAlignment(Pos.CENTER);
        inputCard.setPadding(new Insets(26));
        inputCard.setMaxWidth(480);
        inputCard.setBackground(new Background(new BackgroundFill(CARD_BG, new CornerRadii(14), Insets.EMPTY)));
        inputCard.setStyle("-fx-border-color: #ff6b9d55; -fx-border-radius: 14; -fx-border-width: 1.5;");

        Label inputTitle = lbl("ENTER HOST IP ADDRESS", 12, FontWeight.BOLD, TEXT_DIM);

        TextField ipField = new TextField();
        ipField.setPromptText("e.g.  192.168.1.42");
        ipField.setFont(Font.font("SansSerif", 20));
        ipField.setMaxWidth(320);
        ipField.setAlignment(Pos.CENTER);
        ipField.setStyle(
                "-fx-background-color: #0f0f1e;" +
                        "-fx-text-fill: #f0f0fa;" +
                        "-fx-border-color: #ff6b9d66;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 10;");

        Label portNote = lbl("Port: 5555 (fixed)", 13, FontWeight.NORMAL, TEXT_DIM);
        inputCard.getChildren().addAll(inputTitle, ipField, portNote);

        Label statusMsg = new Label("");
        statusMsg.setFont(Font.font("SansSerif", 14));
        statusMsg.setTextFill(ERROR_RED);
        statusMsg.setWrapText(true);
        statusMsg.setAlignment(Pos.CENTER);

        Button connectBtn = new Button("Connect  →");
        connectBtn.setStyle(
                "-fx-background-color: " + toHex(ACCENT_PINK) + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 17px;" +
                        "-fx-padding: 12 36;" +
                        "-fx-background-radius: 10;");
        connectBtn.setCursor(Cursor.HAND);

        Button cancelBtn = new Button("← Cancel");
        cancelBtn.setStyle("-fx-background-color: " + toHex(CARD_BG) + "; -fx-text-fill: " + toHex(TEXT_DIM)
                + "; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 24;");
        cancelBtn.setCursor(Cursor.HAND);
        cancelBtn.setOnAction(e -> showModeScreen());

        connectBtn.setOnAction(e -> {
            statusMsg.setText("Connecting...");
            bgExecutor.execute(() -> {
                try {
                    gameClient = new GameClient(this::handleNetworkMessage, this::handleConnectionLost);
                    gameClient.connect(ipField.getText(), 5555, "Player 2");
                    Platform.runLater(() -> {
                        statusMsg.setTextFill(SUCCESS_GREEN);
                        statusMsg.setText("Connected!  Waiting for host to start the game…");
                    });
                    // HOST will send CHESS_ACTION("START:…") — handled by handleNetworkMessage
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        statusMsg.setTextFill(ERROR_RED);
                        statusMsg.setText("Failed to connect: " + ex.getMessage());
                        connectBtn.setDisable(false);
                    });
                }
            });
        });

        // Allow pressing Enter to trigger the connect action
        ipField.setOnAction(e -> connectBtn.fire());

        root.getChildren().addAll(title, inputCard, statusMsg, connectBtn, cancelBtn);
        Scene scene = new Scene(root, 1050, 650);
        stage.setTitle("Chess — Join a Game");
        stage.setScene(scene);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NETWORK MESSAGE HANDLER (runs on background thread → dispatches to UI)
    // ─────────────────────────────────────────────────────────────────────────

    private void handleNetworkMessage(GameMessage msg) {
        Platform.runLater(() -> {
            switch (msg.getType()) {
                case CHESS_MOVE -> {
                    // Opponent made a move — apply it and redraw
                    chessBoard.makeMove(
                            msg.getChessFromQ(), msg.getChessFromR(),
                            msg.getChessToQ(), msg.getChessToR());
                    // If the opponent's move caused a pawn promotion, auto-promote to Queen
                    // (the opponent already showed a choice dialog on their side)
                    if (chessBoard.hasPendingPromotion()) {
                        chessBoard.completePromotion(ChessBoard.PieceType.QUEEN);
                    }
                    selectedQ = -999;
                    selectedR = -999;
                    updateBoardDisplay();
                }
                case CHESS_ACTION -> {
                    String action = msg.getChessAction();
                    if (action == null)
                        break;

                    if (action.startsWith("START:")) {
                        // Client receives this: deserialise board, then show the game
                        String boardState = action.substring(6);
                        chessBoard = new ChessBoard();
                        chessBoard.deserializeBoard(boardState);
                        myColor = ChessBoard.PieceColor.BLACK;
                        lanMode = true;
                        selectedQ = -999;
                        selectedR = -999;
                        showLanGameScreen("Client — Playing as Black");

                    } else if (action.equals("RESIGN")) {
                        // Opponent resigned
                        String winner = (myColor == ChessBoard.PieceColor.WHITE) ? "White" : "Black";
                        if (statusLabel != null) {
                            statusLabel.setText("Opponent resigned!\n" + winner + " wins! 🎉");
                            statusLabel.setTextFill(SUCCESS_GREEN);
                        }

                    } else if (action.equals("RESTART")) {
                        chessBoard.resetBoard();
                        selectedQ = -999;
                        selectedR = -999;
                        updateBoardDisplay();
                    }
                }
                default -> {
                    /* heartbeat and others — ignore */ }
            }
        });
    }

    private void handleConnectionLost() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Connection Lost");
            alert.setHeaderText("Opponent Disconnected");
            alert.setContentText("The connection to your opponent was lost.");
            alert.showAndWait();
            showModeScreen();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LAN GAME SCREEN
    // ─────────────────────────────────────────────────────────────────────────

    private void showLanGameScreen(String roleText) {
        Color roleAccent = (myColor == ChessBoard.PieceColor.WHITE) ? ACCENT_CYAN : ACCENT_PINK;

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setBackground(new Background(new BackgroundFill(BG_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));

        // ── Header ────────────────────────────────
        VBox topBox = new VBox(5);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(0, 0, 10, 0));
        Label titleLbl = new Label("CHESS");
        titleLbl.setFont(Font.font("SansSerif", FontWeight.BOLD, 36));
        titleLbl.setTextFill(ACCENT_PURPLE);
        Label subtitleLbl = new Label("Hexagonal Chess — " + roleText);
        subtitleLbl.setFont(Font.font("SansSerif", 14));
        subtitleLbl.setTextFill(roleAccent);
        topBox.getChildren().addAll(titleLbl, subtitleLbl);
        root.setTop(topBox);

        // ── Board (centre) ────────────────────────
        Pane boardPane = buildBoardPane(true);
        HBox boardWrapper = new HBox(boardPane);
        boardWrapper.setAlignment(Pos.CENTER);
        root.setCenter(boardWrapper);

        // ── Left spacer ───────────────────────────
        Region leftSpacer = new Region();
        leftSpacer.setPrefWidth(220);
        root.setLeft(leftSpacer);

        // ── Right sidebar ─────────────────────────
        VBox sidebar = new VBox(18);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPadding(new Insets(0, 0, 0, 20));
        sidebar.setPrefWidth(220);

        // Role card
        VBox roleCard = new VBox(7);
        roleCard.setAlignment(Pos.CENTER);
        roleCard.setPadding(new Insets(14));
        roleCard.setBackground(new Background(new BackgroundFill(CARD_BG, new CornerRadii(10), Insets.EMPTY)));
        roleCard.setStyle(
                "-fx-border-color: " + toHex(roleAccent.darker()) + "; -fx-border-radius: 10; -fx-border-width: 1.5;");
        Label roleTitleLbl = lbl("YOUR COLOR", 11, FontWeight.BOLD, TEXT_DIM);
        Label roleValueLbl = lbl(
                (myColor == ChessBoard.PieceColor.WHITE) ? "⬜  White" : "⬛  Black",
                18, FontWeight.BOLD, roleAccent);
        roleCard.getChildren().addAll(roleTitleLbl, roleValueLbl);

        // Turn indicator card
        VBox turnCard = new VBox(10);
        turnCard.setAlignment(Pos.CENTER);
        turnCard.setPadding(new Insets(15));
        turnCard.setBackground(new Background(new BackgroundFill(CARD_BG, new CornerRadii(10), Insets.EMPTY)));
        turnCard.setStyle("-fx-border-color: #2b3154; -fx-border-radius: 10; -fx-border-width: 1.5;");
        Label tTitleLbl = new Label("CURRENT TURN");
        tTitleLbl.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
        tTitleLbl.setTextFill(TEXT_DIM);
        turnLabel = new Label("White's Turn");
        turnLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 20));
        turnLabel.setTextFill(ACCENT_CYAN);
        turnCard.getChildren().addAll(tTitleLbl, turnLabel);

        // Status label
        statusLabel = new Label("Game started!");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        statusLabel.setTextFill(SUCCESS_GREEN);
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);

        // Resign button
        Button resignBtn = new Button("🏳  Resign");
        styleButton(resignBtn, ERROR_RED);
        resignBtn.setOnAction(e -> {
            sendNetworkMessage(GameMessage.chessAction("RESIGN"));
            statusLabel.setText("You resigned.\nOpponent wins.");
            statusLabel.setTextFill(ERROR_RED);
        });

        Button modeBtn = new Button("Change Mode");
        styleButton(modeBtn, ACCENT_PURPLE);
        modeBtn.setOnAction(e -> showModeScreen());

        Button menuBtn = new Button("Main Menu");
        styleButton(menuBtn, CARD_BG);
        menuBtn.setOnAction(e -> {
            closeLanConnection();
            hub.show();
        });

        sidebar.getChildren().addAll(roleCard, turnCard, statusLabel, resignBtn, modeBtn, menuBtn);
        root.setRight(sidebar);

        updateBoardDisplay();

        Scene scene = new Scene(root, 1200, 850);
        stage.setTitle("Hexagonal Chess — LAN (" + roleText + ")");
        stage.setScene(scene);
    }

    /** Handles square clicks during a LAN game (enforces turn-locking). */
    private void handleLanSquareClick(int q, int r) {
        // Only allow interaction on the local player's turn
        if (chessBoard.getCurrentTurn() != myColor)
            return;
        if (!chessBoard.hasValidMoves(chessBoard.getCurrentTurn()))
            return;
        if (chessBoard.checkDrawCriteria() != null)
            return;

        Piece clickedPiece = chessBoard.getPiece(q, r);

        if (selectedQ == -999 && selectedR == -999) {
            if (clickedPiece != null && clickedPiece.getColor() == chessBoard.getCurrentTurn()) {
                selectedQ = q;
                selectedR = r;
            }
        } else {
            if (chessBoard.isValidMove(selectedQ, selectedR, q, r)) {
                int fromQ = selectedQ, fromR = selectedR;
                chessBoard.makeMove(fromQ, fromR, q, r);
                selectedQ = -999;
                selectedR = -999;
                // Broadcast move first (promotion is resolved locally and auto-queened
                // remotely)
                sendNetworkMessage(GameMessage.chessMove(fromQ, fromR, q, r));
                // Handle promotion choice if needed (turn hasn't switched yet in pending state)
                if (chessBoard.hasPendingPromotion()) {
                    showPromotionDialog(chessBoard.getCurrentTurn(), true);
                    return;
                }
            } else if (clickedPiece != null && clickedPiece.getColor() == chessBoard.getCurrentTurn()) {
                selectedQ = q;
                selectedR = r;
            } else {
                selectedQ = -999;
                selectedR = -999;
            }
        }
        updateBoardDisplay();
    }

    /** Send a message through whichever connection is active (host or client). */
    private void sendNetworkMessage(GameMessage msg) {
        if (gameHost != null)
            gameHost.sendMessage(msg);
        else if (gameClient != null)
            gameClient.sendMessage(msg);
    }

    /** Close and nullify any open host/client connections. */
    private void closeLanConnection() {
        if (gameHost != null) {
            gameHost.close();
            gameHost = null;
        }
        if (gameClient != null) {
            gameClient.close();
            gameClient = null;
        }
        lanMode = false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOCAL GAME SCREEN
    // ─────────────────────────────────────────────────────────────────────────

    private void showLocalGameScreen() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setBackground(new Background(new BackgroundFill(BG_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));

        // Top Header
        VBox topBox = new VBox(5);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(0, 0, 10, 0));
        Label title = new Label("CHESS");
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 36));
        title.setTextFill(ACCENT_PURPLE);
        Label subtitle = new Label("Hexagonal Chess — Local Game");
        subtitle.setFont(Font.font("SansSerif", 14));
        subtitle.setTextFill(TEXT_DIM);
        topBox.getChildren().addAll(title, subtitle);
        root.setTop(topBox);

        // Center board
        Pane boardPane = buildBoardPane(false);
        HBox boardWrapper = new HBox(boardPane);
        boardWrapper.setAlignment(Pos.CENTER);
        root.setCenter(boardWrapper);

        // Left spacer
        Region leftSpacer = new Region();
        leftSpacer.setPrefWidth(220);
        root.setLeft(leftSpacer);

        // Right sidebar
        VBox sidebar = new VBox(20);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPadding(new Insets(0, 0, 0, 20));
        sidebar.setPrefWidth(220);

        // Turn indicator card
        VBox turnCard = new VBox(10);
        turnCard.setAlignment(Pos.CENTER);
        turnCard.setPadding(new Insets(15));
        turnCard.setBackground(new Background(new BackgroundFill(CARD_BG, new CornerRadii(10), Insets.EMPTY)));
        turnCard.setStyle("-fx-border-color: #2b3154; -fx-border-radius: 10; -fx-border-width: 1.5;");
        Label tTitle = new Label("CURRENT TURN");
        tTitle.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
        tTitle.setTextFill(TEXT_DIM);
        turnLabel = new Label("White's Turn");
        turnLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 20));
        turnLabel.setTextFill(ACCENT_CYAN);
        turnCard.getChildren().addAll(tTitle, turnLabel);

        // Status label
        statusLabel = new Label("");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        statusLabel.setTextFill(ERROR_RED);
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);

        Button restartBtn = new Button("Restart Game");
        styleButton(restartBtn, SUCCESS_GREEN);
        restartBtn.setOnAction(e -> restartLocalGame());

        Button modeBtn = new Button("Change Mode");
        styleButton(modeBtn, ACCENT_PURPLE);
        modeBtn.setOnAction(e -> showModeScreen());

        Button menuBtn = new Button("Main Menu");
        styleButton(menuBtn, CARD_BG);
        menuBtn.setOnAction(e -> hub.show());

        sidebar.getChildren().addAll(turnCard, statusLabel, restartBtn, modeBtn, menuBtn);
        root.setRight(sidebar);

        updateBoardDisplay();

        Scene scene = new Scene(root, 1200, 850);
        stage.setTitle("Glinski's Hexagonal Chess — Local Game");
        stage.setScene(scene);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHARED BOARD BUILDER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds and returns a Pane containing all hex squares, wired to the
     * correct click-handler depending on whether we are in LAN mode.
     */
    private Pane buildBoardPane(boolean lan) {
        Pane boardPane = new Pane();
        boardPane.setPrefSize(700, 700);
        boardPane.setMinSize(700, 700);
        boardPane.setMaxSize(700, 700);

        double centerX = 350.0;
        double centerY = 350.0;
        double spacingX = radius * 1.5;
        double spacingY = radius * Math.sqrt(3);

        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (chessBoard.isValidCoord(q, r)) {
                    StackPane sq = new StackPane();
                    sq.setPrefSize(radius * 2, radius * 2);
                    sq.setCursor(Cursor.HAND);

                    double cx = centerX + q * spacingX;
                    double cy = centerY - (r + q / 2.0) * spacingY;
                    sq.setLayoutX(cx - radius);
                    sq.setLayoutY(cy - radius);

                    final int fq = q, fr = r;
                    if (lan) {
                        sq.setOnMouseClicked(e -> handleLanSquareClick(fq, fr));
                    } else {
                        sq.setOnMouseClicked(e -> handleLocalSquareClick(fq, fr));
                    }
                    squarePanes[q + 5][r + 5] = sq;
                    boardPane.getChildren().add(sq);
                }
            }
        }
        return boardPane;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BOARD RENDERING
    // ─────────────────────────────────────────────────────────────────────────

    private void updateBoardDisplay() {
        if (chessBoard == null || turnLabel == null || statusLabel == null)
            return;

        // ── Turn label ────────────────────────────
        if (chessBoard.getCurrentTurn() == ChessBoard.PieceColor.WHITE) {
            turnLabel.setText("White's Turn");
            turnLabel.setTextFill(ACCENT_CYAN);
        } else {
            turnLabel.setText("Black's Turn");
            turnLabel.setTextFill(ACCENT_PINK);
        }

        // ── Game state (check / checkmate / stalemate / draw / lan status) ──
        boolean currentHasMoves = chessBoard.hasValidMoves(chessBoard.getCurrentTurn());
        String drawReason = chessBoard.checkDrawCriteria(); // 50-move / insuf. material / 3-fold

        if (!currentHasMoves) {
            boolean inCheck = chessBoard.isInCheck(chessBoard.getCurrentTurn());
            if (inCheck) {
                String winner = (chessBoard.getCurrentTurn() == ChessBoard.PieceColor.WHITE) ? "Black" : "White";
                statusLabel.setText("CHECKMATE!\n" + winner + " wins! 🎉");
                statusLabel.setTextFill(SUCCESS_GREEN);
            } else {
                statusLabel.setText("STALEMATE — Draw!");
                statusLabel.setTextFill(ACCENT_CYAN);
            }
        } else if (drawReason != null) {
            statusLabel.setText("Draw by " + drawReason + "!");
            statusLabel.setTextFill(ACCENT_CYAN);
        } else if (chessBoard.isInCheck(ChessBoard.PieceColor.WHITE)) {
            statusLabel.setText("White is in CHECK!");
            statusLabel.setTextFill(ERROR_RED);
        } else if (chessBoard.isInCheck(ChessBoard.PieceColor.BLACK)) {
            statusLabel.setText("Black is in CHECK!");
            statusLabel.setTextFill(ERROR_RED);
        } else {
            if (lanMode && myColor != null) {
                boolean myTurn = (chessBoard.getCurrentTurn() == myColor);
                statusLabel.setText(myTurn ? "Your turn!" : "Opponent's turn…");
                statusLabel.setTextFill(myTurn ? SUCCESS_GREEN : TEXT_DIM);
            } else if (botMode) {
                boolean myTurn = (chessBoard.getCurrentTurn() == ChessBoard.PieceColor.WHITE);
                statusLabel.setText(myTurn ? "Your turn (White)" : "Computer thinking…");
                statusLabel.setTextFill(myTurn ? SUCCESS_GREEN : TEXT_DIM);
            } else {
                statusLabel.setText("");
            }
        }

        // ── Draw every hex cell ───────────────────
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (chessBoard.isValidCoord(q, r)) {
                    StackPane sq = squarePanes[q + 5][r + 5];
                    sq.getChildren().clear();

                    // Hex shape
                    Polygon hex = new Polygon();
                    for (int i = 0; i < 6; i++) {
                        double angleRad = Math.toRadians(i * 60);
                        hex.getPoints().addAll(radius * Math.cos(angleRad), radius * Math.sin(angleRad));
                    }

                    int tone = Math.floorMod(q - r, 3);
                    Color baseColor = switch (tone) {
                        case 0 -> Color.web("#cb997e");
                        case 1 -> Color.web("#ddbea9");
                        default -> Color.web("#ffe8d6");
                    };

                    // Highlight king in check
                    Piece p = chessBoard.getPiece(q, r);
                    if (p != null && p.getType() == ChessBoard.PieceType.KING
                            && chessBoard.isInCheck(p.getColor())) {
                        baseColor = Color.web("#801a24");
                    }

                    // Selection highlight
                    Color strokeColor = Color.web("#3a3f61");
                    double strokeWidth = 1.5;
                    if (q == selectedQ && r == selectedR) {
                        strokeColor = Color.web("#ffffff");
                        strokeWidth = 3.0;
                    }

                    hex.setFill(baseColor);
                    hex.setStroke(strokeColor);
                    hex.setStrokeWidth(strokeWidth);
                    sq.getChildren().add(hex);

                    // Piece symbol
                    if (p != null) {
                        String imagePath = "/images/chess/" + p.getImageName();
                        java.net.URL imgUrl = getClass().getResource(imagePath);
                        if (imgUrl != null) {
                            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(imgUrl.toExternalForm()));
                            iv.setFitWidth(radius * 1.5);
                            iv.setFitHeight(radius * 1.5);
                            iv.setPreserveRatio(true);
                            sq.getChildren().add(iv);
                        } else {
                            Label pieceLabel = new Label(p.getSymbol());
                            pieceLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 36));
                            pieceLabel.setTextFill(p.isWhite() ? Color.WHITE : Color.BLACK);
                            sq.getChildren().add(pieceLabel);
                        }
                    }

                    // Valid-move dot
                    if (selectedQ != -999 && selectedR != -999) {
                        if (chessBoard.isValidMove(selectedQ, selectedR, q, r)) {
                            Circle dot = new Circle(7);
                            dot.setFill(chessBoard.getCurrentTurn() == ChessBoard.PieceColor.WHITE
                                    ? Color.web("#00d2ff", 0.6)
                                    : Color.web("#ff6b9d", 0.6));
                            sq.getChildren().add(dot);
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOCAL CLICK HANDLER
    // ─────────────────────────────────────────────────────────────────────────

    private void handleLocalSquareClick(int q, int r) {
        // In bot mode, only allow White to click
        if (botMode && chessBoard.getCurrentTurn() == ChessBoard.PieceColor.BLACK)
            return;
        if (!chessBoard.hasValidMoves(chessBoard.getCurrentTurn()))
            return;
        if (chessBoard.checkDrawCriteria() != null)
            return; // game is a draw — no more moves

        Piece clickedPiece = chessBoard.getPiece(q, r);

        if (selectedQ == -999 && selectedR == -999) {
            if (clickedPiece != null && clickedPiece.getColor() == chessBoard.getCurrentTurn()) {
                selectedQ = q;
                selectedR = r;
            }
        } else {
            if (chessBoard.isValidMove(selectedQ, selectedR, q, r)) {
                ChessBoard.PieceColor movingColor = chessBoard.getCurrentTurn();
                chessBoard.makeMove(selectedQ, selectedR, q, r);
                selectedQ = -999;
                selectedR = -999;
                if (chessBoard.hasPendingPromotion()) {
                    showPromotionDialog(movingColor, false);
                    return;
                }
                updateBoardDisplay();
                maybeTriggerBot();
                return;
            } else if (clickedPiece != null && clickedPiece.getColor() == chessBoard.getCurrentTurn()) {
                selectedQ = q;
                selectedR = r;
            } else {
                selectedQ = -999;
                selectedR = -999;
            }
        }
        updateBoardDisplay();
    }

    private void restartLocalGame() {
        chessBoard.resetBoard();
        selectedQ = -999;
        selectedR = -999;
        updateBoardDisplay();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void styleButton(Button btn, Color bg) {
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(40);
        btn.setCursor(Cursor.HAND);
        btn.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        String normal = "-fx-background-color: " + toHex(bg) + "; -fx-text-fill: white; -fx-background-radius: 8;";
        String hover = "-fx-background-color: " + toHex(bg.brighter())
                + "; -fx-text-fill: white; -fx-background-radius: 8;";
        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(normal));
    }

    private Label lbl(String text, int size, FontWeight weight, Color color) {
        Label l = new Label(text);
        l.setFont(Font.font("SansSerif", weight, size));
        l.setTextFill(color);
        return l;
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
