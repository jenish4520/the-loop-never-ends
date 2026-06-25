package seda_project.control_alt_defeat.chess;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
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
    private Label whiteScoreLabel;
    private Label blackScoreLabel;
    private Label drawOfferLabel;
    private Button offerDrawBtn;
    private Button acceptDrawBtn;
    private Button declineDrawBtn;
    private final double radius = 33.0;

    private boolean gameOver = false;
    private String resultMessage = "";
    private double whiteScore = 0.0;
    private double blackScore = 0.0;
    private ChessBoard.PieceColor drawOfferBy = null;

    private boolean customEditorMode = false;
    private ChessBoard.PieceColor customStartingTurn = ChessBoard.PieceColor.WHITE;
    private ChessBoard.PieceColor customPieceColor = ChessBoard.PieceColor.WHITE;
    private ChessBoard.PieceType customPieceType = ChessBoard.PieceType.KING;
    private boolean customEraseMode = false;

    public ChessApp(Stage stage, GameHub hub) {
        this.stage = stage;
        this.hub = hub;
    }

    /** Always start with the mode selection screen. */
    public void show() {
        showModeScreen();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODE SELECTION SCREEN
    // ─────────────────────────────────────────────────────────────────────────

    private void showModeScreen() {
        closeLanConnection();
        customEditorMode = false;

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
                buildModeCard("", "VS COMPUTER", "Play Against the Chess Bot", SUCCESS_GREEN, this::startBotGame),
                buildModeCard("C", "CUSTOM POSITION", "Build Your Own Start", ACCENT_PURPLE, this::showCustomPositionScreen));

        Button backBtn = new Button("← Back to Main Menu");
        backBtn.setStyle("-fx-background-color: " + toHex(CARD_BG) + "; -fx-text-fill: " + toHex(TEXT_DIM)
                + "; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 20;");
        backBtn.setCursor(Cursor.HAND);
        backBtn.setOnAction(e -> hub.show());

        HBox backRow = new HBox(backBtn);
        backRow.setAlignment(Pos.CENTER);
        backRow.setPadding(new Insets(30, 0, 0, 0));

        root.getChildren().addAll(title, subtitle, cardsRow, backRow);

        Scene scene = new Scene(root, 1320, 650);
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
        customEditorMode = false;
        lanMode = false;
        myColor = null;
        botMode = false;
        chessBoard = new ChessBoard();
        resetGameState();
        selectedQ = -999;
        selectedR = -999;
        showLocalGameScreen();
    }

    // ── Bot mode ───────────────────────────────────────────────────────────────
    private boolean botMode = false;

    private void startBotGame() {
        customEditorMode = false;
        lanMode = false;
        myColor = null;
        botMode = true;
        chessBoard = new ChessBoard();
        resetGameState();
        selectedQ = -999;
        selectedR = -999;
        showLocalGameScreen();
    }

    /** After the player makes a move, let the bot take its turn if it’s black’s go. */
    private void maybeTriggerBot() {
        if (gameOver)
            return;
        if (!botMode)
            return;
        if (chessBoard.getCurrentTurn() != ChessBoard.PieceColor.BLACK)
            return;
        if (!chessBoard.hasValidMoves(ChessBoard.PieceColor.BLACK))
            return;

        bgExecutor.submit(() -> {
            // short pause so the player can actually see their own move
            try {
                Thread.sleep(350);
            } catch (InterruptedException ignored) {
            }
            Move botMove = chessBoard.getBotMove(ChessBoard.PieceColor.BLACK);
            if (botMove != null) {
                Platform.runLater(() -> {
                    if (gameOver)
                        return;
                    ChessBoard.PieceColor movingColor = chessBoard.getCurrentTurn();
                    chessBoard.makeMove(botMove.fromQ, botMove.fromR, botMove.toQ, botMove.toR);
                    revokeDrawOfferAfterMove(movingColor);
                    // bot always promotes to Queen — no need for a dialog on its side
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
        showPromotionDialog(promotingColor, isLan, null);
    }

    private void showPromotionDialog(ChessBoard.PieceColor promotingColor, boolean isLan,
                                     java.util.function.Consumer<ChessBoard.PieceType> afterPromotion) {
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

        // the 4 pieces a pawn can promote to
        record PromOpt(String icon, String name, ChessBoard.PieceType type) {
        }
        java.util.List<PromOpt> opts = java.util.List.of(
                new PromOpt("♛", "Queen", ChessBoard.PieceType.QUEEN),
                new PromOpt("♜", "Rook", ChessBoard.PieceType.ROOK),
                new PromOpt("♝", "Bishop", ChessBoard.PieceType.BISHOP),
                new PromOpt("♞", "Knight", ChessBoard.PieceType.KNIGHT));

        // get a ref to the current root so we can stack the overlay on top
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
                if (afterPromotion != null) {
                    afterPromotion.accept(opt.type());
                }
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
        customEditorMode = false;
        botMode = false;
        chessBoard = new ChessBoard();
        resetGameState();
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

        // wait for a client in the background; update UI on the main thread
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
        customEditorMode = false;
        botMode = false;
        resetGameState();

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
                // HOST will send CHESS_ACTION("START:…") once the game begins
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
                    ChessBoard.PieceColor movingColor = chessBoard.getCurrentTurn();
                    // Opponent made a move — apply it and redraw
                    boolean moved = chessBoard.makeMove(
                            msg.getChessFromQ(), msg.getChessFromR(),
                            msg.getChessToQ(), msg.getChessToR());
                    // If the opponent's move caused a pawn promotion, auto-promote to Queen
                    // (the opponent already showed a choice dialog on their side)
                    if (moved && chessBoard.hasPendingPromotion()) {
                        chessBoard.completePromotion(parsePromotionType(msg.getChessPromotion()));
                    }
                    if (moved) {
                        revokeDrawOfferAfterMove(movingColor);
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
                        resetGameState();
                        myColor = ChessBoard.PieceColor.BLACK;
                        lanMode = true;
                        selectedQ = -999;
                        selectedR = -999;
                        showLanGameScreen("Client — Playing as Black");

                    } else if (action.equals("RESIGN")) {
                        // Opponent resigned
                        finishByResignation(opposite(myColor));
                        String winner = colorName(myColor);
                        if (statusLabel != null) {
                            statusLabel.setText("Opponent resigned!\n" + winner + " wins! 🎉");
                            statusLabel.setTextFill(SUCCESS_GREEN);
                        }

                    } else if (action.equals("DRAW_OFFER")) {
                        drawOfferBy = opposite(myColor);
                        updateBoardDisplay();

                    } else if (action.equals("DRAW_ACCEPT")) {
                        finishDraw("Draw agreed.");

                    } else if (action.equals("DRAW_DECLINE")) {
                        drawOfferBy = null;
                        updateBoardDisplay();

                    } else if (action.equals("RESTART")) {
                        chessBoard.resetBoard();
                        resetGameState();
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

        VBox scoreCard = buildScoreCard();
        drawOfferLabel = buildDrawOfferLabel();
        VBox actionPanel = buildActionPanel();

        Button modeBtn = new Button("Change Mode");
        styleButton(modeBtn, ACCENT_PURPLE);
        modeBtn.setOnAction(e -> showModeScreen());

        Button menuBtn = new Button("Main Menu");
        styleButton(menuBtn, CARD_BG);
        menuBtn.setOnAction(e -> {
            closeLanConnection();
            hub.show();
        });

        sidebar.getChildren().addAll(roleCard, turnCard, scoreCard, statusLabel, drawOfferLabel, actionPanel, modeBtn, menuBtn);
        root.setRight(sidebar);

        updateBoardDisplay();

        Scene scene = new Scene(root, 1200, 850);
        stage.setTitle("Hexagonal Chess — LAN (" + roleText + ")");
        stage.setScene(scene);
    }

    /** Click handler for LAN games — ignore any input when it’s not our turn. */
    private void handleLanSquareClick(int q, int r) {
        if (gameOver)
            return;
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
                ChessBoard.PieceColor movingColor = chessBoard.getCurrentTurn();
                chessBoard.makeMove(fromQ, fromR, q, r);
                revokeDrawOfferAfterMove(movingColor);
                selectedQ = -999;
                selectedR = -999;
                if (chessBoard.hasPendingPromotion()) {
                    showPromotionDialog(movingColor, true,
                            type -> sendNetworkMessage(GameMessage.chessMove(fromQ, fromR, q, r, type.name())));
                    return;
                }
                sendNetworkMessage(GameMessage.chessMove(fromQ, fromR, q, r));
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

    /** Route the message through whichever connection is open (host or client). */
    private void sendNetworkMessage(GameMessage msg) {
        if (gameHost != null)
            gameHost.sendMessage(msg);
        else if (gameClient != null)
            gameClient.sendMessage(msg);
    }

    /** Shut down and null out any open LAN connections. */
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
        customEditorMode = false;
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

        VBox scoreCard = buildScoreCard();
        drawOfferLabel = buildDrawOfferLabel();
        VBox actionPanel = buildActionPanel();

        Button restartBtn = new Button("Restart Game");
        styleButton(restartBtn, SUCCESS_GREEN);
        restartBtn.setOnAction(e -> restartLocalGame());

        Button modeBtn = new Button("Change Mode");
        styleButton(modeBtn, ACCENT_PURPLE);
        modeBtn.setOnAction(e -> showModeScreen());

        Button menuBtn = new Button("Main Menu");
        styleButton(menuBtn, CARD_BG);
        menuBtn.setOnAction(e -> hub.show());

        sidebar.getChildren().addAll(turnCard, scoreCard, statusLabel, drawOfferLabel, actionPanel, restartBtn, modeBtn, menuBtn);
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
                    if (customEditorMode) {
                        sq.setOnMouseClicked(e -> handleCustomSquareClick(fq, fr, e.getButton()));
                    } else if (lan) {
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
        if (!customEditorMode) {
            evaluateGameEnd();
        }
        updateScoreLabels();
        updateActionControls();

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

        if (gameOver) {
            statusLabel.setText(resultMessage);
            statusLabel.setTextFill(SUCCESS_GREEN);
        } else if (!currentHasMoves) {
            boolean inCheck = chessBoard.isInCheck(chessBoard.getCurrentTurn());
            if (inCheck) {
                String winner = (chessBoard.getCurrentTurn() == ChessBoard.PieceColor.WHITE) ? "Black" : "White";
                statusLabel.setText("CHECKMATE!\n" + winner + " wins! 🎉");
                statusLabel.setTextFill(SUCCESS_GREEN);
            } else {
                statusLabel.setText("STALEMATE!");
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
                    if ((q == chessBoard.getLastFromQ() && r == chessBoard.getLastFromR())
                            || (q == chessBoard.getLastToQ() && r == chessBoard.getLastToR())) {
                        baseColor = Color.web("#f2d16b");
                    }

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
        if (gameOver)
            return;
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
                revokeDrawOfferAfterMove(movingColor);
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
        resetGameState();
        selectedQ = -999;
        selectedR = -999;
        updateBoardDisplay();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildScoreCard() {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.setBackground(new Background(new BackgroundFill(CARD_BG, new CornerRadii(10), Insets.EMPTY)));
        card.setStyle("-fx-border-color: #2b3154; -fx-border-radius: 10; -fx-border-width: 1.5;");

        Label title = lbl("POINTS", 11, FontWeight.BOLD, TEXT_DIM);
        HBox scoreRow = new HBox(14);
        scoreRow.setAlignment(Pos.CENTER);

        VBox whiteBox = new VBox(3);
        whiteBox.setAlignment(Pos.CENTER);
        Label whiteTitle = lbl("White", 12, FontWeight.BOLD, ACCENT_CYAN);
        whiteScoreLabel = lbl(formatScore(whiteScore), 24, FontWeight.BOLD, TEXT_WHITE);
        whiteBox.getChildren().addAll(whiteTitle, whiteScoreLabel);

        VBox blackBox = new VBox(3);
        blackBox.setAlignment(Pos.CENTER);
        Label blackTitle = lbl("Black", 12, FontWeight.BOLD, ACCENT_PINK);
        blackScoreLabel = lbl(formatScore(blackScore), 24, FontWeight.BOLD, TEXT_WHITE);
        blackBox.getChildren().addAll(blackTitle, blackScoreLabel);

        scoreRow.getChildren().addAll(whiteBox, blackBox);
        card.getChildren().addAll(title, scoreRow);
        return card;
    }

    private Label buildDrawOfferLabel() {
        Label label = new Label("");
        label.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        label.setTextFill(ACCENT_CYAN);
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private VBox buildActionPanel() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);

        offerDrawBtn = new Button("Offer Draw");
        styleButton(offerDrawBtn, ACCENT_CYAN);
        offerDrawBtn.setOnAction(e -> offerDraw());

        acceptDrawBtn = new Button("Accept Draw");
        styleButton(acceptDrawBtn, SUCCESS_GREEN);
        acceptDrawBtn.setOnAction(e -> acceptDrawOffer());

        declineDrawBtn = new Button("Decline Draw");
        styleButton(declineDrawBtn, CARD_BG);
        declineDrawBtn.setOnAction(e -> declineDrawOffer());

        Button resignBtn = new Button("Resign");
        styleButton(resignBtn, ERROR_RED);
        resignBtn.setOnAction(e -> resignCurrentPlayer());

        box.getChildren().addAll(offerDrawBtn, acceptDrawBtn, declineDrawBtn, resignBtn);
        return box;
    }

    private void resetGameState() {
        gameOver = false;
        resultMessage = "";
        whiteScore = 0.0;
        blackScore = 0.0;
        drawOfferBy = null;
        updateScoreLabels();
        updateActionControls();
    }

    private void evaluateGameEnd() {
        if (gameOver || chessBoard == null) {
            return;
        }

        ChessBoard.PieceColor turn = chessBoard.getCurrentTurn();
        boolean currentHasMoves = chessBoard.hasValidMoves(turn);
        if (!currentHasMoves) {
            if (chessBoard.isInCheck(turn)) {
                finishWin(opposite(turn), "CHECKMATE!\n" + colorName(opposite(turn)) + " wins.");
            } else {
                finishStalemate(turn);
            }
            return;
        }

        String drawReason = chessBoard.checkDrawCriteria();
        if (drawReason != null) {
            finishDraw("Draw by " + drawReason + ".");
        }
    }

    private void finishWin(ChessBoard.PieceColor winner, String message) {
        whiteScore = winner == ChessBoard.PieceColor.WHITE ? 1.0 : 0.0;
        blackScore = winner == ChessBoard.PieceColor.BLACK ? 1.0 : 0.0;
        setFinished(message);
    }

    private void finishDraw(String message) {
        whiteScore = 0.5;
        blackScore = 0.5;
        setFinished(message + "\nWhite 0.5 - Black 0.5");
    }

    private void finishStalemate(ChessBoard.PieceColor stalemated) {
        ChessBoard.PieceColor scorer = opposite(stalemated);
        whiteScore = scorer == ChessBoard.PieceColor.WHITE ? 0.75 : 0.25;
        blackScore = scorer == ChessBoard.PieceColor.BLACK ? 0.75 : 0.25;
        setFinished("STALEMATE!\n" + colorName(scorer) + " gets 0.75, " + colorName(stalemated) + " gets 0.25.");
    }

    private void finishByResignation(ChessBoard.PieceColor loser) {
        ChessBoard.PieceColor winner = opposite(loser);
        finishWin(winner, colorName(loser) + " resigned.\n" + colorName(winner) + " wins.");
    }

    private void setFinished(String message) {
        gameOver = true;
        resultMessage = message;
        drawOfferBy = null;
        selectedQ = -999;
        selectedR = -999;
        updateScoreLabels();
        updateActionControls();
        if (statusLabel != null) {
            statusLabel.setText(resultMessage);
            statusLabel.setTextFill(SUCCESS_GREEN);
        }
    }

    private void offerDraw() {
        if (gameOver || chessBoard == null || drawOfferBy != null) {
            return;
        }
        if (botMode && chessBoard.getCurrentTurn() != ChessBoard.PieceColor.WHITE) {
            return;
        }
        if (lanMode) {
            if (myColor == null) {
                return;
            }
            drawOfferBy = myColor;
            sendNetworkMessage(GameMessage.chessAction("DRAW_OFFER"));
        } else {
            drawOfferBy = chessBoard.getCurrentTurn();
        }
        updateBoardDisplay();
    }

    private void acceptDrawOffer() {
        if (gameOver || drawOfferBy == null) {
            return;
        }
        if (lanMode) {
            sendNetworkMessage(GameMessage.chessAction("DRAW_ACCEPT"));
        }
        finishDraw("Draw agreed.");
    }

    private void declineDrawOffer() {
        if (drawOfferBy == null) {
            return;
        }
        if (lanMode) {
            sendNetworkMessage(GameMessage.chessAction("DRAW_DECLINE"));
        }
        drawOfferBy = null;
        updateBoardDisplay();
    }

    private void resignCurrentPlayer() {
        if (gameOver || chessBoard == null) {
            return;
        }
        ChessBoard.PieceColor loser;
        if (lanMode && myColor != null) {
            loser = myColor;
            sendNetworkMessage(GameMessage.chessAction("RESIGN"));
        } else if (botMode) {
            loser = ChessBoard.PieceColor.WHITE;
        } else {
            loser = chessBoard.getCurrentTurn();
        }
        finishByResignation(loser);
        updateBoardDisplay();
    }

    private void revokeDrawOfferAfterMove(ChessBoard.PieceColor movingColor) {
        if (drawOfferBy != null && drawOfferBy != movingColor) {
            drawOfferBy = null;
        }
    }

    private void updateScoreLabels() {
        if (whiteScoreLabel != null) {
            whiteScoreLabel.setText(formatScore(whiteScore));
        }
        if (blackScoreLabel != null) {
            blackScoreLabel.setText(formatScore(blackScore));
        }
    }

    private void updateActionControls() {
        if (drawOfferLabel != null) {
            if (drawOfferBy == null) {
                drawOfferLabel.setText("");
            } else if (lanMode && drawOfferBy == myColor) {
                drawOfferLabel.setText("Draw offered. Waiting for opponent.");
            } else if (lanMode) {
                drawOfferLabel.setText("Opponent offered a draw.");
            } else if (botMode) {
                drawOfferLabel.setText("Draw offered to computer.");
            } else {
                drawOfferLabel.setText(colorName(drawOfferBy) + " offered a draw.");
            }
        }

        boolean offerActive = drawOfferBy != null;
        if (offerDrawBtn != null) {
            boolean canOffer = !gameOver && chessBoard != null && !offerActive;
            if (botMode) {
                canOffer = canOffer && chessBoard.getCurrentTurn() == ChessBoard.PieceColor.WHITE;
            }
            if (lanMode) {
                canOffer = canOffer && myColor != null;
            }
            offerDrawBtn.setDisable(!canOffer);
        }

        boolean canAnswer = !gameOver && offerActive;
        if (canAnswer && lanMode) {
            canAnswer = drawOfferBy != myColor;
        } else if (canAnswer && botMode) {
            canAnswer = false;
        } else if (canAnswer && chessBoard != null) {
            canAnswer = drawOfferBy != chessBoard.getCurrentTurn();
        }

        if (acceptDrawBtn != null) {
            acceptDrawBtn.setVisible(canAnswer);
            acceptDrawBtn.setManaged(canAnswer);
        }
        if (declineDrawBtn != null) {
            declineDrawBtn.setVisible(canAnswer);
            declineDrawBtn.setManaged(canAnswer);
        }
    }

    private String formatScore(double score) {
        if (score == 1.0) {
            return "1";
        }
        if (score == 0.75) {
            return "0.75";
        }
        if (score == 0.5) {
            return "0.5";
        }
        if (score == 0.25) {
            return "0.25";
        }
        return "0";
    }

    private ChessBoard.PieceColor opposite(ChessBoard.PieceColor color) {
        return color == ChessBoard.PieceColor.WHITE ? ChessBoard.PieceColor.BLACK : ChessBoard.PieceColor.WHITE;
    }

    private String colorName(ChessBoard.PieceColor color) {
        return color == ChessBoard.PieceColor.WHITE ? "White" : "Black";
    }

    private ChessBoard.PieceType parsePromotionType(String value) {
        if (value != null) {
            try {
                return ChessBoard.PieceType.valueOf(value);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ChessBoard.PieceType.QUEEN;
    }

    private void showCustomPositionScreen() {
        closeLanConnection();
        customEditorMode = true;
        lanMode = false;
        botMode = false;
        myColor = null;
        chessBoard = new ChessBoard();
        chessBoard.clearBoard();
        resetGameState();
        selectedQ = -999;
        selectedR = -999;
        customStartingTurn = ChessBoard.PieceColor.WHITE;
        customPieceColor = ChessBoard.PieceColor.WHITE;
        customPieceType = ChessBoard.PieceType.KING;
        customEraseMode = false;

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setBackground(new Background(new BackgroundFill(BG_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));

        VBox topBox = new VBox(5);
        topBox.setAlignment(Pos.CENTER);
        Label title = new Label("CUSTOM POSITION");
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 34));
        title.setTextFill(ACCENT_PURPLE);
        Label subtitle = new Label("Build a legal Glinski chess start position");
        subtitle.setFont(Font.font("SansSerif", 14));
        subtitle.setTextFill(TEXT_DIM);
        topBox.getChildren().addAll(title, subtitle);
        root.setTop(topBox);

        Pane boardPane = buildBoardPane(false);
        HBox boardWrapper = new HBox(boardPane);
        boardWrapper.setAlignment(Pos.CENTER);
        root.setCenter(boardWrapper);

        VBox sidebar = new VBox(14);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPadding(new Insets(0, 0, 0, 20));
        sidebar.setPrefWidth(260);

        Label pieceTitle = lbl("PIECE", 12, FontWeight.BOLD, TEXT_DIM);
        ComboBox<String> pieceBox = new ComboBox<>();
        pieceBox.getItems().addAll("King", "Queen", "Rook", "Bishop", "Knight", "Pawn", "Erase");
        pieceBox.setValue("King");
        pieceBox.setMaxWidth(Double.MAX_VALUE);
        pieceBox.setOnAction(e -> {
            String value = pieceBox.getValue();
            customEraseMode = "Erase".equals(value);
            if (!customEraseMode) {
                customPieceType = ChessBoard.PieceType.valueOf(value.toUpperCase());
            }
        });

        Label colorTitle = lbl("COLOR", 12, FontWeight.BOLD, TEXT_DIM);
        ComboBox<String> colorBox = new ComboBox<>();
        colorBox.getItems().addAll("White", "Black");
        colorBox.setValue("White");
        colorBox.setMaxWidth(Double.MAX_VALUE);
        colorBox.setOnAction(e -> customPieceColor = "White".equals(colorBox.getValue())
                ? ChessBoard.PieceColor.WHITE : ChessBoard.PieceColor.BLACK);

        Label turnTitle = lbl("STARTING TURN", 12, FontWeight.BOLD, TEXT_DIM);
        HBox turnRow = new HBox(8);
        turnRow.setAlignment(Pos.CENTER);
        Button whiteTurnBtn = new Button("White");
        Button blackTurnBtn = new Button("Black");
        styleButton(whiteTurnBtn, ACCENT_CYAN);
        styleButton(blackTurnBtn, ACCENT_PINK);
        whiteTurnBtn.setOnAction(e -> {
            customStartingTurn = ChessBoard.PieceColor.WHITE;
            chessBoard.setCurrentTurn(customStartingTurn);
            updateCustomEditorDisplay();
        });
        blackTurnBtn.setOnAction(e -> {
            customStartingTurn = ChessBoard.PieceColor.BLACK;
            chessBoard.setCurrentTurn(customStartingTurn);
            updateCustomEditorDisplay();
        });
        turnRow.getChildren().addAll(whiteTurnBtn, blackTurnBtn);

        statusLabel = new Label("");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);

        Button clearBtn = new Button("Clear Board");
        styleButton(clearBtn, CARD_BG);
        clearBtn.setOnAction(e -> {
            chessBoard.clearBoard();
            chessBoard.setCurrentTurn(customStartingTurn);
            updateCustomEditorDisplay();
        });

        Button standardBtn = new Button("Standard Setup");
        styleButton(standardBtn, ACCENT_PURPLE);
        standardBtn.setOnAction(e -> {
            chessBoard.resetBoard();
            customStartingTurn = ChessBoard.PieceColor.WHITE;
            chessBoard.setCurrentTurn(customStartingTurn);
            updateCustomEditorDisplay();
        });

        Button startBtn = new Button("Start Game");
        styleButton(startBtn, SUCCESS_GREEN);
        startBtn.setOnAction(e -> startCustomGameFromEditor());

        Button backBtn = new Button("Change Mode");
        styleButton(backBtn, CARD_BG);
        backBtn.setOnAction(e -> showModeScreen());

        sidebar.getChildren().addAll(pieceTitle, pieceBox, colorTitle, colorBox, turnTitle, turnRow,
                statusLabel, clearBtn, standardBtn, startBtn, backBtn);
        root.setRight(sidebar);

        updateCustomEditorDisplay();

        Scene scene = new Scene(root, 1200, 850);
        stage.setTitle("Chess - Custom Position");
        stage.setScene(scene);
    }

    private void handleCustomSquareClick(int q, int r, MouseButton button) {
        if (!customEditorMode) {
            return;
        }
        boolean removePiece = button == MouseButton.SECONDARY || customEraseMode;
        chessBoard.setPiece(q, r, removePiece ? null : new Piece(customPieceType, customPieceColor));
        updateCustomEditorDisplay();
    }

    private void updateCustomEditorDisplay() {
        if (chessBoard == null || statusLabel == null) {
            return;
        }
        chessBoard.setCurrentTurn(customStartingTurn);
        String validation = chessBoard.validatePosition(customStartingTurn);
        if (validation == null) {
            statusLabel.setText("Position is valid.");
            statusLabel.setTextFill(SUCCESS_GREEN);
        } else {
            statusLabel.setText(validation);
            statusLabel.setTextFill(ERROR_RED);
        }

        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (chessBoard.isValidCoord(q, r)) {
                    StackPane sq = squarePanes[q + 5][r + 5];
                    if (sq == null) {
                        continue;
                    }
                    sq.getChildren().clear();

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

                    Piece p = chessBoard.getPiece(q, r);
                    if (p != null && p.getType() == ChessBoard.PieceType.KING && chessBoard.isInCheck(p.getColor())) {
                        baseColor = Color.web("#801a24");
                    }

                    hex.setFill(baseColor);
                    hex.setStroke(Color.web("#3a3f61"));
                    hex.setStrokeWidth(1.5);
                    sq.getChildren().add(hex);

                    if (p != null) {
                        addPieceNode(sq, p);
                    }
                }
            }
        }
    }

    private void startCustomGameFromEditor() {
        chessBoard.setCurrentTurn(customStartingTurn);
        String validation = chessBoard.validatePosition(customStartingTurn);
        if (validation != null) {
            statusLabel.setText(validation);
            statusLabel.setTextFill(ERROR_RED);
            return;
        }
        chessBoard.startFromCurrentPosition();
        customEditorMode = false;
        botMode = false;
        lanMode = false;
        myColor = null;
        resetGameState();
        selectedQ = -999;
        selectedR = -999;
        showLocalGameScreen();
    }

    private void addPieceNode(StackPane sq, Piece p) {
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
