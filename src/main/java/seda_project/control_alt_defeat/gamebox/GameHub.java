package seda_project.control_alt_defeat.gamebox;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import seda_project.control_alt_defeat.tetris.TetrisApp;

public class GameHub {
    private Stage stage;

    private static final Color CARD_BG = Color.web("#1e2341");
    private static final Color CARD_HOVER = Color.web("#282d50");
    private static final Color ACCENT_CYAN = Color.web("#00d2ff");
    private static final Color ACCENT_PINK = Color.web("#ff6b9d");
    private static final Color TEXT_DIM = Color.web("#8c8caa");

    public GameHub(Stage stage) {
        this.stage = stage;
        stage.setTitle("Retro Games");
        stage.setWidth(900);
        stage.setHeight(650);
        stage.setMinWidth(900);
        stage.setMinHeight(650);
    }

    public void show() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f0f1e;");

        Label title = new Label("Retro Games");
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 48));
        title.setTextFill(Color.web("#f0f0fa"));

        Label subtitle = new Label("Select a Game to Play");
        subtitle.setFont(Font.font("SansSerif", 18));
        subtitle.setTextFill(TEXT_DIM);

        HBox cardsRow = new HBox(30);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.setPadding(new Insets(40, 0, 0, 0));

        VBox memoryCard = buildModeCard("1.", "MEMORY GAME", "Match the Cards", ACCENT_CYAN, () -> {
            GameBox memoryGame = new GameBox(stage, this);
            memoryGame.show();
        });

        VBox tetrisCard = buildModeCard("2.", "TETRIS", "Classic Block Puzzle", ACCENT_PINK, () -> {
            TetrisApp tetrisGame = new TetrisApp(stage, this);
            tetrisGame.show();
        });

        cardsRow.getChildren().addAll(memoryCard, tetrisCard);
        root.getChildren().addAll(title, subtitle, cardsRow);

        Scene scene = new Scene(root, 900, 650);
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildModeCard(String icon, String titleStr, String descStr, Color accent, Runnable onClick) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(220, 220);
        card.setCursor(Cursor.HAND);

        Background normalBg = new Background(new BackgroundFill(CARD_BG, new CornerRadii(20), Insets.EMPTY));
        Background hoverBg = new Background(new BackgroundFill(CARD_HOVER, new CornerRadii(20), Insets.EMPTY));
        card.setBackground(normalBg);
        card.setStyle("-fx-border-color: " + toHexString(accent.darker().darker())
                + "; -fx-border-width: 1.5; -fx-border-radius: 20;");

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("SansSerif", 48));
        iconLabel.setTextFill(accent);

        Label titleLabel = new Label(titleStr);
        titleLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 17));
        titleLabel.setTextFill(accent);

        Label descLabel = new Label(descStr);
        descLabel.setFont(Font.font("SansSerif", 13));
        descLabel.setTextFill(TEXT_DIM);

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

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
