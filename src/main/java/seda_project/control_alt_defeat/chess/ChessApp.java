package seda_project.control_alt_defeat.chess;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import seda_project.control_alt_defeat.gamebox.GameHub;

public class ChessApp {
    private final Stage stage;
    private final GameHub hub;
    private final ChessBoard chessBoard;

    private static final Color BG_COLOR = Color.web("#0f0f1e");
    private static final Color CARD_BG = Color.web("#1e2341");
    private static final Color ACCENT_CYAN = Color.web("#00d2ff");
    private static final Color ACCENT_PINK = Color.web("#ff6b9d");
    private static final Color ACCENT_PURPLE = Color.web("#a882ff");
    private static final Color TEXT_DIM = Color.web("#8c8caa");
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
        this.chessBoard = new ChessBoard();
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setBackground(new Background(new BackgroundFill(BG_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));

        // Top Header
        VBox topBox = new VBox(5);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(0, 0, 10, 0));
        Label title = new Label("CHESS");
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 36));
        title.setTextFill(ACCENT_CYAN);
        Label subtitle = new Label("Hexagonal Chess");
        subtitle.setFont(Font.font("SansSerif", 14));
        subtitle.setTextFill(TEXT_DIM);
        topBox.getChildren().addAll(title, subtitle);
        root.setTop(topBox);

        // Center: Custom Hexagonal Board Pane
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

                    final int currentQ = q;
                    final int currentR = r;
                    sq.setOnMouseClicked(e -> handleSquareClick(currentQ, currentR));

                    squarePanes[q + 5][r + 5] = sq;
                    boardPane.getChildren().add(sq);
                }
            }
        }

        HBox boardWrapper = new HBox(boardPane);
        boardWrapper.setAlignment(Pos.CENTER);
        root.setCenter(boardWrapper);

        // Left: Spacer Panel to center the board horizontally
        Region leftSpacer = new Region();
        leftSpacer.setPrefWidth(220);
        root.setLeft(leftSpacer);

        // Right: Sidebar Panel
        VBox sidebar = new VBox(20);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPadding(new Insets(0, 0, 0, 20));
        sidebar.setPrefWidth(220);

        // Turn Indicator Card
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

        // Game status text (like Check, Checkmate)
        statusLabel = new Label("");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        statusLabel.setTextFill(ERROR_RED);
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);

        // Action Buttons
        Button restartBtn = new Button("Restart Game");
        styleButton(restartBtn, SUCCESS_GREEN);
        restartBtn.setOnAction(e -> restartGame());

        Button menuBtn = new Button("Main Menu");
        styleButton(menuBtn, CARD_BG);
        menuBtn.setOnAction(e -> hub.show());

        sidebar.getChildren().addAll(turnCard, statusLabel, restartBtn, menuBtn);
        root.setRight(sidebar);

        // Update display initially
        updateBoardDisplay();

        Scene scene = new Scene(root, 1200, 850);
        stage.setTitle("Glinski's Hexagonal Chess");
        stage.setScene(scene);
    }

    private void styleButton(Button btn, Color bg) {
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(40);
        btn.setCursor(Cursor.HAND);
        btn.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));

        String normalStyle = "-fx-background-color: " + toHexString(bg)
                + "; -fx-text-fill: white; -fx-background-radius: 8;";
        String hoverStyle = "-fx-background-color: " + toHexString(bg.brighter())
                + "; -fx-text-fill: white; -fx-background-radius: 8;";

        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(normalStyle));
    }

    private void updateBoardDisplay() {
        // Update Turn label
        if (chessBoard.getCurrentTurn() == ChessBoard.PieceColor.WHITE) {
            turnLabel.setText("White's Turn");
            turnLabel.setTextFill(ACCENT_CYAN);
        } else {
            turnLabel.setText("Black's Turn");
            turnLabel.setTextFill(ACCENT_PINK);
        }

        // Update Status label (Check, Checkmate, Stalemate)
        boolean whiteCheck = chessBoard.isInCheck(ChessBoard.PieceColor.WHITE);
        boolean blackCheck = chessBoard.isInCheck(ChessBoard.PieceColor.BLACK);
        boolean currentHasMoves = chessBoard.hasValidMoves(chessBoard.getCurrentTurn());

        if (!currentHasMoves) {
            boolean inCheck = chessBoard.isInCheck(chessBoard.getCurrentTurn());
            if (inCheck) {
                String winner = (chessBoard.getCurrentTurn() == ChessBoard.PieceColor.WHITE) ? "Black" : "White";
                statusLabel.setText("CHECKMATE!\n" + winner + " wins!");
                statusLabel.setTextFill(SUCCESS_GREEN);
            } else {
                statusLabel.setText("STALEMATE!\nIt's a Draw!");
                statusLabel.setTextFill(ACCENT_CYAN);
            }
        } else if (whiteCheck) {
            statusLabel.setText("White is in CHECK!");
            statusLabel.setTextFill(ERROR_RED);
        } else if (blackCheck) {
            statusLabel.setText("Black is in CHECK!");
            statusLabel.setTextFill(ERROR_RED);
        } else {
            statusLabel.setText("");
        }

        // Draw Squares and Pieces
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (chessBoard.isValidCoord(q, r)) {
                    StackPane sq = squarePanes[q + 5][r + 5];
                    sq.getChildren().clear();

                    // Polygon hexagon shape
                    Polygon hex = new Polygon();
                    for (int i = 0; i < 6; i++) {
                        double angleRad = Math.toRadians(i * 60);
                        hex.getPoints().addAll(
                                radius * Math.cos(angleRad),
                                radius * Math.sin(angleRad));
                    }

                    // Tones: #cb997e, #ddbea9, #ffe8d6
                    int tone = Math.floorMod(q - r, 3);
                    Color baseColor = switch (tone) {
                        case 0 -> Color.web("#cb997e");
                        case 1 -> Color.web("#ddbea9");
                        default -> Color.web("#ffe8d6");
                    };

                    // Highlight King in check
                    Piece p = chessBoard.getPiece(q, r);
                    if (p != null && p.getType() == ChessBoard.PieceType.KING && chessBoard.isInCheck(p.getColor())) {
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

                    // Piece Symbol
                    if (p != null) {
                        Label pieceLabel = new Label(p.getSymbol());
                        pieceLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 36));
                        pieceLabel.setTextFill(p.isWhite() ? Color.WHITE : Color.BLACK);
                        sq.getChildren().add(pieceLabel);
                    }

                    // Highlight valid moves
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

    private void handleSquareClick(int q, int r) {
        if (!chessBoard.hasValidMoves(chessBoard.getCurrentTurn())) {
            return;
        }

        Piece clickedPiece = chessBoard.getPiece(q, r);

        if (selectedQ == -999 && selectedR == -999) {
            // First click: Select own piece
            if (clickedPiece != null && clickedPiece.getColor() == chessBoard.getCurrentTurn()) {
                selectedQ = q;
                selectedR = r;
            }
        } else {
            // Second click: Make move, select another own piece, or deselect
            if (chessBoard.isValidMove(selectedQ, selectedR, q, r)) {
                chessBoard.makeMove(selectedQ, selectedR, q, r);
                selectedQ = -999;
                selectedR = -999;
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

    private void restartGame() {
        chessBoard.resetBoard();
        selectedQ = -999;
        selectedR = -999;
        updateBoardDisplay();
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
