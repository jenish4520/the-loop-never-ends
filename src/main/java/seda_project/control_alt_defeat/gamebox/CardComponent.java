package seda_project.control_alt_defeat.gamebox;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

public class CardComponent extends StackPane {

    private static final Map<String, Image> imageCache = new HashMap<>();

    private static Image getImage(String name) {
        if (name == null) return null;
        return imageCache.computeIfAbsent(name, k -> {
            try {
                java.net.URL url = CardComponent.class.getResource("/images/" + k + ".png");
                if (url != null) {
                    return new Image(url.toExternalForm());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    private static final Color CARD_BACK_TOP = Color.web("#6c3483");
    private static final Color CARD_BACK_BOTTOM = Color.web("#8e44ad");
    private static final Color CARD_FACE_BG = Color.web("#fdf6e3");
    private static final Color CARD_FACE_BORDER = Color.web("#c8beaa");
    private static final Color MATCHED_BORDER = Color.web("#2ecc71");
    private static final Color ATTEMPT_BORDER = Color.web("#ffd700");
    private static final Color HOVER_GLOW = Color.web("#00d2ff80");

    private static final Color[] SYMBOL_COLORS = {
        Color.web("#e74c3c"), Color.web("#3498db"), Color.web("#2ecc71"), Color.web("#9b59b6"),
        Color.web("#f1c40f"), Color.web("#e67e22"), Color.web("#1abc9c"), Color.web("#ec4899"),
        Color.web("#6366f1"), Color.web("#14b8a6")
    };

    private Card card;
    private final int index;

    private boolean targetFaceUp = false;
    private boolean isFaceUpVisual = false;

    private StackPane frontPane;
    private StackPane backPane;
    private Rectangle hoverGlow;
    private Rectangle borderRect;
    private Text matchedPlayerText;

    public CardComponent(int index, IntConsumer onClick) {
        this.index = index;

        setPrefSize(180, 220);
        setMinSize(180, 220);
        setMaxSize(180, 220);
        setCursor(Cursor.HAND);

        backPane = createBackPane();
        frontPane = createFrontPane();
        frontPane.setVisible(false);

        hoverGlow = new Rectangle(172, 212);
        hoverGlow.setArcWidth(14);
        hoverGlow.setArcHeight(14);
        hoverGlow.setFill(Color.TRANSPARENT);
        hoverGlow.setStroke(HOVER_GLOW);
        hoverGlow.setStrokeWidth(3);
        hoverGlow.setVisible(false);

        getChildren().addAll(backPane, frontPane, hoverGlow);

        setOnMouseClicked(e -> {
            if (card != null && card.isClickable()) {
                onClick.accept(index);
            }
        });

        setOnMouseEntered(e -> {
            if (card != null && card.isClickable()) {
                hoverGlow.setVisible(true);
            }
        });

        setOnMouseExited(e -> {
            hoverGlow.setVisible(false);
        });
    }

    private StackPane createBackPane() {
        StackPane pane = new StackPane();
        Rectangle bg = new Rectangle(172, 212);
        bg.setArcWidth(14);
        bg.setArcHeight(14);
        LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, CARD_BACK_TOP), new Stop(1, CARD_BACK_BOTTOM));
        bg.setFill(gradient);
        bg.setStroke(Color.web("#501e64"));
        bg.setStrokeWidth(1.5);

        Text qText = new Text("?");
        qText.setFont(Font.font("SansSerif", FontWeight.BOLD, 64));
        qText.setFill(Color.web("#ffffff78"));

        pane.getChildren().addAll(bg, qText);
        return pane;
    }

    private StackPane createFrontPane() {
        StackPane pane = new StackPane();
        borderRect = new Rectangle(172, 212);
        borderRect.setArcWidth(14);
        borderRect.setArcHeight(14);
        borderRect.setFill(CARD_FACE_BG);
        borderRect.setStroke(CARD_FACE_BORDER);
        borderRect.setStrokeWidth(1.5);

        matchedPlayerText = new Text();
        matchedPlayerText.setFont(Font.font("SansSerif", FontWeight.BOLD, 40));
        StackPane.setAlignment(matchedPlayerText, Pos.TOP_RIGHT);
        StackPane.setMargin(matchedPlayerText, new javafx.geometry.Insets(5, 10, 0, 0));

        pane.getChildren().addAll(borderRect, matchedPlayerText);
        return pane;
    }

    public void updateCard(Card card, boolean inCurrentAttempt) {
        boolean wasClickable = (this.card != null && this.card.isClickable());
        this.card = card;

        setCursor(card != null && card.isClickable() ? Cursor.HAND : Cursor.DEFAULT);
        if (!card.isClickable()) hoverGlow.setVisible(false);

        boolean newFaceUp = (card != null) && (card.isFaceUp() || card.isMatched());

        if (newFaceUp != targetFaceUp) {
            targetFaceUp = newFaceUp;
            playFlipAnimation(newFaceUp);
        } else {
            updateVisuals(inCurrentAttempt);
        }
    }

    private void playFlipAnimation(boolean toFaceUp) {
        ScaleTransition shrink = new ScaleTransition(Duration.millis(150), this);
        shrink.setFromX(1.0);
        shrink.setToX(0.0);
        shrink.setInterpolator(Interpolator.EASE_IN);

        shrink.setOnFinished(e -> {
            isFaceUpVisual = toFaceUp;
            frontPane.setVisible(isFaceUpVisual);
            backPane.setVisible(!isFaceUpVisual);
            updateVisuals(card != null && card.isFaceUp() && !card.isMatched());

            ScaleTransition grow = new ScaleTransition(Duration.millis(150), this);
            grow.setFromX(0.0);
            grow.setToX(1.0);
            grow.setInterpolator(Interpolator.EASE_OUT);
            grow.play();
        });

        shrink.play();
    }

    private void updateVisuals(boolean inCurrentAttempt) {
        if (!isFaceUpVisual) return;

        if (frontPane.getChildren().size() > 2) {
            frontPane.getChildren().remove(1);
        }

        if (card.getSymbol() != null) {
            Image img = getImage(card.getSymbol());
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(152);
                iv.setFitHeight(192);
                iv.setPreserveRatio(true);
                frontPane.getChildren().add(1, iv);
            } else {
                Text symText = new Text(card.getSymbol());
                Color symColor = SYMBOL_COLORS[Math.abs(card.getSymbol().hashCode()) % SYMBOL_COLORS.length];
                symText.setFill(symColor);
                symText.setFont(Font.font("SansSerif", FontWeight.BOLD, 40));
                frontPane.getChildren().add(1, symText);
            }
        }

        if (card.isMatched()) {
            int player = card.getMatchedByPlayer();
            if (player > 0) {
                matchedPlayerText.setText(String.valueOf(player));
                matchedPlayerText.setFill(player == 1 ? Color.web("#00b4dc") : Color.web("#dc5082"));
                borderRect.setStroke(player == 2 ? Color.web("#e74c3c") : MATCHED_BORDER);
            } else {
                matchedPlayerText.setText("✓");
                matchedPlayerText.setFill(MATCHED_BORDER);
                borderRect.setStroke(MATCHED_BORDER);
            }
            borderRect.setStrokeWidth(3);
        } else {
            matchedPlayerText.setText("");
            if (inCurrentAttempt) {
                borderRect.setStroke(ATTEMPT_BORDER);
                borderRect.setStrokeWidth(3);
            } else {
                borderRect.setStroke(CARD_FACE_BORDER);
                borderRect.setStrokeWidth(1.5);
            }
        }
    }
}
