package seda_project.control_alt_defeat.gamebox;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class GamePanel extends BorderPane {

    private static final Color PLAYER1_COLOR = Color.web("#00d2ff");
    private static final Color PLAYER2_COLOR = Color.web("#ff6b9d");
    private static final Color TEXT_COLOR = Color.web("#e6e6f0");
    private static final Color DIM_TEXT = Color.web("#9696aa");
    private static final Color GOLD = Color.web("#ffd700");
    private static final Color SUCCESS_GREEN = Color.web("#2ecc71");
    private static final Color ERROR_RED = Color.web("#e74c3c");

    private final int localPlayerNumber;
    private final boolean localMode;
    private final String player1Name;
    private final String player2Name;
    private final IntConsumer onCardClick;
    private final Runnable onRestartClick;
    private final Runnable onBackClick;

    private Label player1NameLabel;
    private Label player1ScoreLabel;
    private Label player2NameLabel;
    private Label player2ScoreLabel;
    private Label turnLabel;
    private Label statusLabel;
    private GridPane cardGridPanel;
    private StackPane centerContainer;
    private Button restartButton;

    private List<CardComponent> cardComponents = new ArrayList<>();
    private GameState currentState;

    public GamePanel(int localPlayerNumber, boolean localMode, String p1Name, String p2Name, IntConsumer onCardClick, Runnable onRestartClick, Runnable onBackClick) {
        this.localPlayerNumber = localPlayerNumber;
        this.localMode = localMode;
        this.player1Name = p1Name == null || p1Name.isBlank() ? "Player 1" : p1Name;
        this.player2Name = p2Name == null || p2Name.isBlank() ? "Player 2" : p2Name;
        this.onCardClick = onCardClick;
        this.onRestartClick = onRestartClick;
        this.onBackClick = onBackClick;

        setPadding(new Insets(15, 20, 15, 20));

        setTop(createTopBar());
        setBottom(createStatusBar());

        cardGridPanel = new GridPane();
        cardGridPanel.setAlignment(Pos.CENTER);
        cardGridPanel.setHgap(8);
        cardGridPanel.setVgap(8);
        
        Group gridGroup = new Group(cardGridPanel);
        centerContainer = new StackPane(gridGroup);
        centerContainer.setPadding(new Insets(10));
        
        centerContainer.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> updateScale());
        cardGridPanel.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> updateScale());
        
        setCenter(centerContainer);
    }

    private BorderPane createTopBar() {
        BorderPane topBar = new BorderPane();
        topBar.setPadding(new Insets(5, 10, 15, 10));

        VBox p1Panel = createPlayerPanel(1);
        player1NameLabel = (Label) ((HBox) p1Panel.getChildren().get(0)).getChildren().get(0);
        player1ScoreLabel = (Label) p1Panel.getChildren().get(1);

        VBox p2Panel = createPlayerPanel(2);
        player2NameLabel = (Label) ((HBox) p2Panel.getChildren().get(0)).getChildren().get(0);
        player2ScoreLabel = (Label) p2Panel.getChildren().get(1);

        turnLabel = new Label("Waiting...");
        turnLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 18));
        turnLabel.setTextFill(GOLD);
        
        HBox centerBox = new HBox(turnLabel);
        centerBox.setAlignment(Pos.CENTER);

        topBar.setLeft(p1Panel);
        topBar.setCenter(centerBox);
        topBar.setRight(p2Panel);

        return topBar;
    }

    private String getPlayerDisplayName(int playerNum) {
        String name = (playerNum == 1) ? player1Name : player2Name;
        if (!localMode && playerNum == localPlayerNumber) name += " (You)";
        return name;
    }

    private VBox createPlayerPanel(int playerNum) {
        VBox panel = new VBox();
        panel.setAlignment(Pos.CENTER);
        panel.setPrefSize(200, 60);

        Color playerColor = (playerNum == 1) ? PLAYER1_COLOR : PLAYER2_COLOR;
        String name = getPlayerDisplayName(playerNum);

        HBox nameRow = new HBox(5);
        nameRow.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 16));
        nameLabel.setTextFill(playerColor);
        nameRow.getChildren().add(nameLabel);

        Label scoreLabel = new Label("0");
        scoreLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 28));
        scoreLabel.setTextFill(TEXT_COLOR);

        panel.getChildren().addAll(nameRow, scoreLabel);
        return panel;
    }

    private BorderPane createStatusBar() {
        BorderPane bar = new BorderPane();
        bar.setPadding(new Insets(10, 10, 5, 10));

        statusLabel = new Label(" ");
        statusLabel.setFont(Font.font("SansSerif", 14));
        statusLabel.setTextFill(DIM_TEXT);
        
        HBox centerBox = new HBox(statusLabel);
        centerBox.setAlignment(Pos.CENTER);

        restartButton = new Button("Restart Game");
        restartButton.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        restartButton.setStyle("-fx-background-color: " + toHexString(SUCCESS_GREEN) + "; -fx-text-fill: white;");
        restartButton.setCursor(Cursor.HAND);
        restartButton.setPrefSize(130, 36);
        restartButton.setVisible(false);
        restartButton.setOnAction(e -> onRestartClick.run());

        Button backButton = new Button("Launcher Menu");
        backButton.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        backButton.setStyle("-fx-background-color: " + toHexString(ERROR_RED) + "; -fx-text-fill: " + toHexString(TEXT_COLOR) + ";");
        backButton.setCursor(Cursor.HAND);
        backButton.setPrefSize(130, 36);
        backButton.setOnAction(e -> onBackClick.run());

        bar.setLeft(backButton);
        bar.setCenter(centerBox);
        bar.setRight(restartButton);

        return bar;
    }

    public void updateState(GameState state) {
        this.currentState = state;

        player1ScoreLabel.setText(String.valueOf(state.getPlayer1Score()));
        player2ScoreLabel.setText(String.valueOf(state.getPlayer2Score()));

        boolean p1Active = state.getActivePlayer() == 1;
        player1NameLabel.setText(p1Active ? "👑 " + getPlayerDisplayName(1) : getPlayerDisplayName(1));
        player2NameLabel.setText(!p1Active ? "👑 " + getPlayerDisplayName(2) : getPlayerDisplayName(2));
        
        player1NameLabel.setTextFill(p1Active ? PLAYER1_COLOR : DIM_TEXT);
        player2NameLabel.setTextFill(!p1Active ? PLAYER2_COLOR : DIM_TEXT);
        player1ScoreLabel.setTextFill(p1Active ? PLAYER1_COLOR : TEXT_COLOR);
        player2ScoreLabel.setTextFill(!p1Active ? PLAYER2_COLOR : TEXT_COLOR);

        if (state.getPhase() == GamePhase.GAME_OVER) {
            int winner = state.getWinner();
            if (winner == 0) {
                turnLabel.setText("It's a Draw!");
                turnLabel.setTextFill(GOLD);
            } else {
                Color wColor = (winner == 1) ? PLAYER1_COLOR : PLAYER2_COLOR;
                String winnerName = (winner == 1) ? player1Name : player2Name;
                turnLabel.setText(winnerName + " Wins!");
                turnLabel.setTextFill(wColor);
            }
            restartButton.setVisible(true);
        } else if (state.getPhase() == GamePhase.RESOLVING) {
            turnLabel.setText("");
            turnLabel.setTextFill(GOLD);
        } else {
            if (localMode) {
                String playerName = (state.getActivePlayer() == 1) ? player1Name : player2Name;
                turnLabel.setText(playerName + "'s Turn");
                Color turnColor = (state.getActivePlayer() == 1) ? PLAYER1_COLOR : PLAYER2_COLOR;
                turnLabel.setTextFill(turnColor);
            } else {
                boolean myTurn = state.getActivePlayer() == localPlayerNumber;
                turnLabel.setText(myTurn ? "Your Turn" : "Opponent's Turn");
                turnLabel.setTextFill(myTurn ? GOLD : DIM_TEXT);
            }
            restartButton.setVisible(false);
        }

        String msg = state.getStatusMessage();
        if (msg != null && !msg.isEmpty()) {
            statusLabel.setText(msg);
        } else {
            int matched = state.getTotalMatched();
            int total = state.getDeckSize();
            statusLabel.setText("Matched: " + matched + " / " + total +
                " | Match size: " + state.getMatchSize());
        }

        if (cardComponents.size() != state.getCards().size()) {
            initializeCardGrid(state);
        }

        List<Card> cards = state.getCards();
        List<Integer> attempt = state.getCurrentAttempt();
        for (int i = 0; i < cards.size(); i++) {
            cardComponents.get(i).updateCard(cards.get(i), attempt.contains(i));
        }
    }

    private void initializeCardGrid(GameState state) {
        cardGridPanel.getChildren().clear();
        cardComponents.clear();

        int total = state.getDeckSize();
        int rows = (int) Math.sqrt(total);
        while (total % rows != 0) {
            rows--;
        }
        int cols = total / rows;

        for (int i = 0; i < total; i++) {
            CardComponent cc = new CardComponent(i, onCardClick);
            cardComponents.add(cc);
            cardGridPanel.add(cc, i % cols, i / cols);
        }
    }

    public void disableInput() {
        for (CardComponent cc : cardComponents) {
            cc.setDisable(true);
        }
    }

    public void showError(String message) {
        statusLabel.setText("ERROR: " + message);
        statusLabel.setTextFill(ERROR_RED);
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }

    private void updateScale() {
        double containerWidth = centerContainer.getWidth() - centerContainer.getPadding().getLeft() - centerContainer.getPadding().getRight();
        double containerHeight = centerContainer.getHeight() - centerContainer.getPadding().getTop() - centerContainer.getPadding().getBottom();
        
        double gridWidth = cardGridPanel.getWidth();
        double gridHeight = cardGridPanel.getHeight();
        
        if (gridWidth > 0 && gridHeight > 0 && containerWidth > 0 && containerHeight > 0) {
            double scaleX = containerWidth / gridWidth;
            double scaleY = containerHeight / gridHeight;
            double scale = Math.min(scaleX, scaleY);
            
            if (scale > 1.0) {
                scale = 1.0;
            }
            
            cardGridPanel.setScaleX(scale);
            cardGridPanel.setScaleY(scale);
        }
    }
}
