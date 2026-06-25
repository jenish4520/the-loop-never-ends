package seda_project.control_alt_defeat.gamebox;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.BiConsumer;

public class MenuPanel extends StackPane {

    private static final Color BG_COLOR = Color.web("#0f0f1e");
    private static final Color CARD_BG = Color.web("#1e2341");
    private static final Color CARD_HOVER = Color.web("#282d50");
    private static final Color ACCENT_PURPLE = Color.web("#a882ff");
    private static final Color ACCENT_CYAN = Color.web("#00d2ff");
    private static final Color ACCENT_PINK = Color.web("#ff6b9d");
    private static final Color TEXT_WHITE = Color.web("#f0f0fa");
    private static final Color TEXT_DIM = Color.web("#8c8caa");
    private static final Color SUCCESS_GREEN = Color.web("#2ecc71");
    private static final Color ERROR_RED = Color.web("#e74c3c");

    private final BiConsumer<Integer, Integer> onLocalGame;
    private final BiConsumer<Integer, Integer> onHost;
    private final TriConsumer<String, String, Integer> onJoin;
    private final Runnable onBackToHub;

    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    private enum Mode {
        LOCAL, HOST, JOIN
    }

    private Mode selectedMode;

    private int matchSize = 2;
    private int selectedDeckSize = 20;

    private Label matchSizeLabel;
    private Button[] suggestionButtons;
    private TextField customDeckField;
    private Label deckErrorLabel;
    private Label statusLabel;
    private TextField portField;
    private TextField player1Field;
    private TextField player2Field;
    private VBox player2Col;
    private TextField joinNameField;

    private VBox modeScreen;
    private VBox configScreen;
    private VBox joinScreen;

    public String getPlayer1Name() {
        return player1Field != null ? player1Field.getText().trim() : "";
    }

    public String getPlayer2Name() {
        return player2Field != null ? player2Field.getText().trim() : "";
    }

    public String getJoinPlayerName() {
        return joinNameField != null ? joinNameField.getText().trim() : "";
    }

    public MenuPanel(BiConsumer<Integer, Integer> onLocalGame,
            BiConsumer<Integer, Integer> onHost,
            TriConsumer<String, String, Integer> onJoin,
            Runnable onBackToHub) {
        this.onLocalGame = onLocalGame;
        this.onHost = onHost;
        this.onJoin = onJoin;
        this.onBackToHub = onBackToHub;

        setBackground(new Background(new BackgroundFill(BG_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));

        modeScreen = buildModeScreen();
        configScreen = buildConfigScreen();
        joinScreen = buildJoinScreen();

        getChildren().add(modeScreen);
    }

    private void showScreen(VBox screen) {
        getChildren().clear();
        getChildren().add(screen);
    }

    private VBox buildModeScreen() {
        VBox center = new VBox(10);
        center.setAlignment(Pos.CENTER);

        Label title = makeLabel("MEMORY GAME", 52, FontWeight.BOLD, ACCENT_CYAN);
        Label subtitle = makeLabel("Choose how you want to play", 18, FontWeight.NORMAL, TEXT_DIM);

        HBox cardsRow = new HBox(24);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.setPadding(new Insets(40, 0, 0, 0));

        cardsRow.getChildren().addAll(
                buildModeCard("💻", "LOCAL GAME", "Two Players, One Screen", ACCENT_PURPLE, () -> {
                    selectedMode = Mode.LOCAL;
                    showConfigScreen();
                }),
                buildModeCard("⌂", "HOST A GAME", "Create a LAN Session", ACCENT_CYAN, () -> {
                    selectedMode = Mode.HOST;
                    showConfigScreen();
                }),
                buildModeCard("→", "JOIN A GAME", "Connect to a Host", ACCENT_PINK, () -> {
                    selectedMode = Mode.JOIN;
                    showScreen(joinScreen);
                }));

        Button backBtn = new Button("Back to Main Menu");
        backBtn.setStyle("-fx-background-color: " + toHexString(CARD_BG) + "; -fx-text-fill: " + toHexString(TEXT_DIM)
                + "; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 20;");
        backBtn.setCursor(Cursor.HAND);
        backBtn.setOnAction(e -> onBackToHub.run());

        HBox backRow = new HBox(backBtn);
        backRow.setAlignment(Pos.CENTER);
        backRow.setPadding(new Insets(30, 0, 0, 0));

        center.getChildren().addAll(title, subtitle, cardsRow, backRow);
        return center;
    }

    private VBox buildModeCard(String icon, String title, String desc, Color accent, Runnable onClick) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(220, 220);
        card.setCursor(Cursor.HAND);

        Background normalBg = new Background(new BackgroundFill(CARD_BG, new CornerRadii(20), Insets.EMPTY));
        Background hoverBg = new Background(new BackgroundFill(CARD_HOVER, new CornerRadii(20), Insets.EMPTY));
        card.setBackground(normalBg);
        card.setStyle("-fx-border-color: " + toHexString(accent.darker().darker())
                + "; -fx-border-width: 1.5; -fx-border-radius: 20;");

        Label iconLabel = makeLabel(icon, 48, FontWeight.NORMAL, accent);
        Label titleLabel = makeLabel(title, 17, FontWeight.BOLD, accent);
        Label descLabel = makeLabel(desc, 13, FontWeight.NORMAL, TEXT_DIM);

        card.getChildren().addAll(iconLabel, titleLabel, descLabel);

        card.setOnMouseEntered(e -> {
            card.setBackground(hoverBg);
            card.setStyle(
                    "-fx-border-color: " + toHexString(accent) + "; -fx-border-width: 2.5; -fx-border-radius: 20;");
        });
        card.setOnMouseExited(e -> {
            card.setBackground(normalBg);
            card.setStyle("-fx-border-color: " + toHexString(accent.darker().darker())
                    + "; -fx-border-width: 1.5; -fx-border-radius: 20;");
        });
        card.setOnMouseClicked(e -> onClick.run());

        return card;
    }

    private VBox buildConfigScreen() {
        VBox center = new VBox(10);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(600);

        Button backBtn = makeBackButton();
        HBox backBox = new HBox(backBtn);
        backBox.setAlignment(Pos.CENTER_LEFT);
        backBox.setPadding(new Insets(0, 0, 10, 0));

        Label configTitle = makeLabel("Configure Your Game", 32, FontWeight.BOLD, TEXT_WHITE);

        HBox namesRow = new HBox(40);
        namesRow.setAlignment(Pos.CENTER);
        namesRow.setPadding(new Insets(10, 0, 10, 0));

        VBox p1Col = new VBox(5);
        p1Col.setAlignment(Pos.CENTER);
        Label p1Label = makeLabel("Player 1 Name", 14, FontWeight.BOLD, ACCENT_CYAN);
        player1Field = new TextField("Player 1");
        player1Field.setStyle("-fx-font-size: 16px; -fx-alignment: center; -fx-pref-width: 150px;");
        p1Col.getChildren().addAll(p1Label, player1Field);

        player2Col = new VBox(5);
        player2Col.setAlignment(Pos.CENTER);
        Label p2Label = makeLabel("Player 2 Name", 14, FontWeight.BOLD, ACCENT_PINK);
        player2Field = new TextField("Player 2");
        player2Field.setStyle("-fx-font-size: 16px; -fx-alignment: center; -fx-pref-width: 150px;");
        player2Col.getChildren().addAll(p2Label, player2Field);

        namesRow.getChildren().addAll(p1Col, player2Col);

        Label nTitle = makeLabel("Match Size (n)", 20, FontWeight.BOLD, ACCENT_CYAN);
        Label nDesc = makeLabel("How Many Identical Cards Form a Match", 14, FontWeight.NORMAL, TEXT_DIM);

        HBox nRow = new HBox(16);
        nRow.setAlignment(Pos.CENTER);
        Button minusBtn = makeRoundButton("−", ACCENT_CYAN);
        matchSizeLabel = makeLabel(String.valueOf(matchSize), 36, FontWeight.BOLD, TEXT_WHITE);
        Button plusBtn = makeRoundButton("+", ACCENT_CYAN);

        minusBtn.setOnAction(e -> {
            if (matchSize > 1) {
                matchSize--;
                updateConfigUI();
            }
        });
        plusBtn.setOnAction(e -> {
            if (matchSize < 45) {
                matchSize++;
                updateConfigUI();
            }
        });

        nRow.getChildren().addAll(minusBtn, matchSizeLabel, plusBtn);

        Label deckTitle = makeLabel("Number of Cards", 20, FontWeight.BOLD, ACCENT_PURPLE);

        HBox sugRow = new HBox(16);
        sugRow.setAlignment(Pos.CENTER);
        suggestionButtons = new Button[3];
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            suggestionButtons[i] = new Button();
            suggestionButtons[i].setPrefSize(140, 60);
            suggestionButtons[i].setCursor(Cursor.HAND);
            suggestionButtons[i].setOnAction(e -> selectSuggestion(idx));
            sugRow.getChildren().add(suggestionButtons[i]);
        }

        HBox customRow = new HBox(10);
        customRow.setAlignment(Pos.CENTER);
        Label orLabel = makeLabel("or enter custom:", 14, FontWeight.NORMAL, TEXT_DIM);
        customDeckField = new TextField();
        customDeckField.setStyle("-fx-font-size: 16px; -fx-alignment: center; -fx-pref-width: 80px;");
        Button applyBtn = new Button("Apply");
        applyBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        applyBtn.setOnAction(e -> applyCustomDeck());
        customRow.getChildren().addAll(orLabel, customDeckField, applyBtn);

        deckErrorLabel = makeLabel(" ", 13, FontWeight.NORMAL, ERROR_RED);

        portField = new TextField(String.valueOf(GameHost.DEFAULT_PORT));
        portField.setStyle("-fx-font-size: 16px; -fx-alignment: center; -fx-pref-width: 100px;");

        Button startBtn = makeBigButton("START GAME", SUCCESS_GREEN);
        startBtn.setOnAction(e -> handleStart());

        statusLabel = makeLabel(" ", 15, FontWeight.NORMAL, TEXT_DIM);

        center.getChildren().addAll(
                backBox, configTitle, namesRow,
                nTitle, nDesc, nRow,
                deckTitle, sugRow, customRow, deckErrorLabel,
                startBtn, statusLabel);

        updateConfigUI();
        return center;
    }

    private void showConfigScreen() {
        updateConfigUI();
        player2Col.setVisible(selectedMode == Mode.LOCAL);
        player2Col.setManaged(selectedMode == Mode.LOCAL);
        showScreen(configScreen);
    }

    private void updateConfigUI() {
        matchSizeLabel.setText(String.valueOf(matchSize));

        int[] sizes = getSuggestedDeckSizes(matchSize);
        String[] labels = { "Small", "Medium", "Large" };

        for (int i = 0; i < 3; i++) {
            if (i < sizes.length) {
                suggestionButtons[i].setText(sizes[i] + " cards\n" + labels[i]);
                suggestionButtons[i].setVisible(true);
                suggestionButtons[i].setManaged(true);
                styleUnselected(suggestionButtons[i]);
            } else {
                suggestionButtons[i].setVisible(false);
                suggestionButtons[i].setManaged(false);
            }
        }

        if (sizes.length > 0) {
            int defaultIdx = Math.min(1, sizes.length - 1);
            selectedDeckSize = sizes[defaultIdx];
            styleSelected(suggestionButtons[defaultIdx]);
        } else {
            selectedDeckSize = -1;
        }
        deckErrorLabel.setText(" ");
        customDeckField.setText("");
    }

    private int[] getSuggestedDeckSizes(int n) {
        java.util.List<Integer> validSizes = new java.util.ArrayList<>();

        int small = n * Math.max(1, (int) Math.ceil(12.0 / n));
        if (small <= 45)
            validSizes.add(small);

        int medium = n * Math.max(2, (int) Math.ceil(20.0 / n));
        if (medium <= small)
            medium = small + n;
        if (medium <= 45)
            validSizes.add(medium);

        int large = n * Math.max(3, (int) Math.ceil(30.0 / n));
        if (large <= medium)
            large = medium + n;
        if (large <= 45)
            validSizes.add(large);

        if (validSizes.isEmpty()) {
            int maxMult = 45 / n;
            if (maxMult > 0)
                validSizes.add(n * maxMult);
        }

        return validSizes.stream().mapToInt(Integer::intValue).toArray();
    }

    private void selectSuggestion(int idx) {
        int[] sizes = getSuggestedDeckSizes(matchSize);
        if (idx >= sizes.length)
            return;
        selectedDeckSize = sizes[idx];
        for (int i = 0; i < sizes.length; i++) {
            if (i == idx)
                styleSelected(suggestionButtons[i]);
            else
                styleUnselected(suggestionButtons[i]);
        }
        deckErrorLabel.setText(" ");
        customDeckField.setText("");
    }

    private void applyCustomDeck() {
        String text = customDeckField.getText().trim();
        if (text.isEmpty())
            return;
        try {
            int size = Integer.parseInt(text);
            if (size < GameConfig.MIN_DECK_SIZE || size > GameConfig.MAX_DECK_SIZE) {
                GameConfig.validate(matchSize, size);
            }
            if (size % matchSize != 0) {
                int lower = (size / matchSize) * matchSize;
                int upper = lower + matchSize;

                String tryMsg = "Try ";
                if (lower >= GameConfig.MIN_DECK_SIZE && upper <= GameConfig.MAX_DECK_SIZE) {
                    tryMsg += lower + " or " + upper;
                } else if (lower >= GameConfig.MIN_DECK_SIZE) {
                    tryMsg += lower;
                } else if (upper <= GameConfig.MAX_DECK_SIZE) {
                    tryMsg += upper;
                } else {
                    tryMsg = "No valid sizes";
                }

                deckErrorLabel.setText("Must be Divisible by " + matchSize + ". " + tryMsg);
                selectedDeckSize = -1;
                return;
            }
            GameConfig.validate(matchSize, size);
            selectedDeckSize = size;
            deckErrorLabel.setText(" ");
            for (Button b : suggestionButtons)
                styleUnselected(b);
        } catch (NumberFormatException ex) {
            deckErrorLabel.setText("Enter a Valid Number");
            selectedDeckSize = -1;
        } catch (IllegalArgumentException ex) {
            deckErrorLabel.setText(ex.getMessage());
            selectedDeckSize = -1;
        }
    }

    private void handleStart() {
        if (selectedDeckSize <= 0) {
            deckErrorLabel.setText("Please Select or Enter a Valid Deck Size");
            return;
        }
        try {
            GameConfig.validate(matchSize, selectedDeckSize);
        } catch (IllegalArgumentException ex) {
            deckErrorLabel.setText(ex.getMessage());
            return;
        }

        if (selectedMode == Mode.LOCAL) {
            onLocalGame.accept(matchSize, selectedDeckSize);
        } else if (selectedMode == Mode.HOST) {
            statusLabel.setText("Waiting for Player 2 to Connect...");
            statusLabel.setTextFill(ACCENT_CYAN);
            onHost.accept(matchSize, selectedDeckSize);
        }
    }

    private void styleSelected(Button btn) {
        btn.setStyle(
                "-fx-background-color: " + toHexString(ACCENT_PURPLE) + "; -fx-text-fill: black; -fx-border-color: "
                        + toHexString(ACCENT_PURPLE.brighter()) + "; -fx-border-width: 2; -fx-font-weight: bold;");
    }

    private void styleUnselected(Button btn) {
        btn.setStyle(
                "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #3c3c5a; -fx-border-width: 1;");
    }

    private VBox buildJoinScreen() {
        VBox center = new VBox(15);
        center.setAlignment(Pos.CENTER);
        center.setMaxWidth(400);

        Button backBtn = makeBackButton();
        HBox backBox = new HBox(backBtn);
        backBox.setAlignment(Pos.CENTER_LEFT);

        Label joinTitle = makeLabel("Join a Game", 32, FontWeight.BOLD, ACCENT_PINK);
        Label joinDesc = makeLabel("Enter Your Name and the Host's Connection Details", 16, FontWeight.NORMAL,
                TEXT_DIM);

        Label yourNameLabel = makeLabel("Your Name", 18, FontWeight.BOLD, ACCENT_PINK);
        joinNameField = new TextField("Player 2");
        joinNameField.setStyle("-fx-font-size: 20px; -fx-alignment: center; -fx-pref-width: 280px;");

        Label ipLabel = makeLabel("Host IP Address", 18, FontWeight.BOLD, TEXT_WHITE);
        TextField ipField = new TextField();
        ipField.setStyle(
                "-fx-font-family: monospace; -fx-font-size: 22px; -fx-alignment: center; -fx-pref-width: 320px;");

        Label portLabel = makeLabel("Port", 18, FontWeight.BOLD, TEXT_WHITE);
        TextField joinPort = new TextField(String.valueOf(GameHost.DEFAULT_PORT));
        joinPort.setStyle(
                "-fx-font-family: monospace; -fx-font-size: 22px; -fx-alignment: center; -fx-pref-width: 200px;");

        Button connectBtn = makeBigButton("CONNECT", ACCENT_PINK);
        Label joinStatus = makeLabel(" ", 15, FontWeight.NORMAL, TEXT_DIM);

        connectBtn.setOnAction(e -> {
            String addr = ipField.getText().trim();
            if (addr.isEmpty()) {
                joinStatus.setText("Please Enter the Host IP Address");
                joinStatus.setTextFill(ERROR_RED);
                return;
            }
            String joinName = joinNameField.getText().trim();
            if (joinName.isEmpty())
                joinName = "Player 2";
            try {
                int port = Integer.parseInt(joinPort.getText().trim());
                joinStatus.setText("Connecting to " + addr + ":" + port + "...");
                joinStatus.setTextFill(ACCENT_PINK);
                onJoin.accept(joinName, addr, port);
            } catch (NumberFormatException ex) {
                joinStatus.setText("Invalid Port Number");
                joinStatus.setTextFill(ERROR_RED);
            }
        });

        center.getChildren().addAll(backBox, joinTitle, joinDesc, yourNameLabel, joinNameField, ipLabel, ipField,
                portLabel, joinPort, connectBtn, joinStatus);
        return center;
    }

    private Label makeLabel(String text, int size, FontWeight weight, Color color) {
        Label label = new Label(text);
        label.setFont(Font.font("SansSerif", weight, size));
        label.setTextFill(color);
        return label;
    }

    private Button makeBackButton() {
        Button btn = new Button("← Back");
        btn.setStyle("-fx-background-color: " + toHexString(CARD_BG) + "; -fx-text-fill: " + toHexString(TEXT_DIM)
                + "; -fx-font-weight: bold; -fx-font-size: 16px;");
        btn.setCursor(Cursor.HAND);
        btn.setOnAction(e -> showScreen(modeScreen));
        return btn;
    }

    private Button makeBigButton(String text, Color color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + toHexString(color)
                + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 20px;");
        btn.setPrefSize(260, 52);
        btn.setCursor(Cursor.HAND);
        return btn;
    }

    private Button makeRoundButton(String text, Color color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + toHexString(CARD_BG) + "; -fx-text-fill: " + toHexString(color)
                + "; -fx-border-color: " + toHexString(color.darker())
                + "; -fx-font-weight: bold; -fx-font-size: 22px;");
        btn.setPrefSize(50, 50);
        btn.setCursor(Cursor.HAND);
        return btn;
    }

    public void setStatus(String text, Color color) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(text);
                statusLabel.setTextFill(color);
            }
        });
    }

    public int getPort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (Exception e) {
            return GameHost.DEFAULT_PORT;
        }
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
