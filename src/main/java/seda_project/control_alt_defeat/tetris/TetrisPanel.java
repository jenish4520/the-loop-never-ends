package seda_project.control_alt_defeat.tetris;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.HashSet;
import java.util.Set;

public class TetrisPanel extends StackPane {

    private Canvas canvas;
    private GameLogic logic;
    private boolean running = false;
    private Thread loopThread;

    private int CELL = 14;
    private int BOARD_W = Board.WIDTH * CELL;
    private int BOARD_H = Board.HEIGHT * CELL;

    private boolean isClient = false;
    private boolean isLanHost = false;
    private java.util.function.Consumer<TetrisMessage.Type> outMessage;

    private Set<KeyCode> activeKeys = new HashSet<>();
    private long lastDAS_P1 = 0;
    private long lastDAS_P2 = 0;
    private long lastRepeat_P1 = 0;
    private long lastRepeat_P2 = 0;
    private boolean initialDAS_P1 = false;
    private boolean initialDAS_P2 = false;

    // Extra DAS state for two-blocks mode (second piece per player)
    private long lastDAS_P1b = 0;
    private long lastDAS_P2b = 0;
    private long lastRepeat_P1b = 0;
    private long lastRepeat_P2b = 0;
    private boolean initialDAS_P1b = false;
    private boolean initialDAS_P2b = false;

    private Button restartBtn;

    public TetrisPanel(GameLogic logic, Runnable onBack) {
        this.logic = logic;
        canvas = new Canvas(900, 650);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        Button backBtn = new Button("\u2190 Quit Game");
        backBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        backBtn.setOnAction(e -> {
            stop();
            onBack.run();
        });
        StackPane.setAlignment(backBtn, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(backBtn, new javafx.geometry.Insets(10));

        restartBtn = new Button("Restart Game");
        restartBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 28;");
        restartBtn.setVisible(false);
        restartBtn.setOnAction(e -> {
            // Reset: rebuild logic preserving mode options
            GameLogic newLogic = new GameLogic(logic.p1.name, logic.p2.name, logic.getSpeedLevel());
            newLogic.twoBlocksMode  = logic.twoBlocksMode;
            newLogic.horizontalMode = logic.horizontalMode;
            newLogic.initModes();
            this.logic = newLogic;
            restartBtn.setVisible(false);
            requestFocus();
        });
        StackPane.setAlignment(restartBtn, javafx.geometry.Pos.CENTER);
        StackPane.setMargin(restartBtn, new javafx.geometry.Insets(120, 0, 0, 0));

        getChildren().addAll(canvas, backBtn, restartBtn);

        setFocusTraversable(true);
        setOnMouseClicked(e -> requestFocus());
        setOnKeyPressed(e -> activeKeys.add(e.getCode()));
        setOnKeyReleased(e -> {
            activeKeys.remove(e.getCode());
            // Two-blocks mode: reset DAS for each independent key
            if (logic != null && logic.twoBlocksMode) {
                // P1 piece1: LEFT / RIGHT
                if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.RIGHT) initialDAS_P1 = false;
                // P1 piece2: COMMA / SLASH
                if (e.getCode() == KeyCode.COMMA || e.getCode() == KeyCode.SLASH) initialDAS_P1b = false;
                // P2 piece1: A / D
                if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.D) initialDAS_P2 = false;
                // P2 piece2: F / H
                if (e.getCode() == KeyCode.F || e.getCode() == KeyCode.H) initialDAS_P2b = false;
                return;
            }
            if (logic != null && logic.horizontalMode) {
                boolean horizontal = e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN
                        || e.getCode() == KeyCode.W || e.getCode() == KeyCode.S;
                if (isClient || isLanHost) {
                    if (horizontal) { initialDAS_P1 = false; initialDAS_P2 = false; }
                } else {
                    if (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN) initialDAS_P1 = false;
                    if (e.getCode() == KeyCode.W  || e.getCode() == KeyCode.S)    initialDAS_P2 = false;
                }
            } else {
                boolean horizontal = e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.RIGHT
                        || e.getCode() == KeyCode.A || e.getCode() == KeyCode.D;
                if (isClient || isLanHost) {
                    if (horizontal) { initialDAS_P1 = false; initialDAS_P2 = false; }
                } else {
                    if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.RIGHT) initialDAS_P1 = false;
                    if (e.getCode() == KeyCode.A    || e.getCode() == KeyCode.D)     initialDAS_P2 = false;
                }
            }
        });
    }

    public void setNetworkMode(boolean isClient, java.util.function.Consumer<TetrisMessage.Type> outMessage) {
        this.isClient = isClient;
        this.outMessage = outMessage;
    }

    public void setLanHostMode() {
        this.isLanHost = true;
    }

    public void updateState(PlayerState p1, PlayerState p2) {
        this.logic.p1 = p1;
        this.logic.p2 = p2;
    }

    public void start() {
        Platform.runLater(this::requestFocus);
        running = true;
        loopThread = new Thread(() -> {
            while (running) {
                long now = System.currentTimeMillis();
                handleInput(now);
                if (!isClient) {
                    logic.update(now);
                }
                Platform.runLater(this::render);
                long elapsed = System.currentTimeMillis() - now;
                long sleepTime = 16 - elapsed;
                if (sleepTime > 0) {
                    try { Thread.sleep(sleepTime); } catch (InterruptedException e) {}
                }
            }
        });
        loopThread.setDaemon(true);
        loopThread.start();
    }

    public void stop() {
        running = false;
    }

    // Input handling

    private void handleInput(long now) {
        if (isClient) {
            boolean goLeft  = activeKeys.contains(KeyCode.A)   || activeKeys.contains(KeyCode.LEFT);
            boolean goRight = activeKeys.contains(KeyCode.D)   || activeKeys.contains(KeyCode.RIGHT);
            boolean softDrp = activeKeys.contains(KeyCode.UP)  || activeKeys.contains(KeyCode.W);
            if (goLeft) {
                if (!initialDAS_P2) {
                    outMessage.accept(TetrisMessage.Type.INPUT_LEFT);
                    initialDAS_P2 = true; lastDAS_P2 = now;
                } else if (now - lastDAS_P2 > 170 && now - lastRepeat_P2 > 50) {
                    outMessage.accept(TetrisMessage.Type.INPUT_LEFT);
                    lastRepeat_P2 = now;
                }
            } else if (goRight) {
                if (!initialDAS_P2) {
                    outMessage.accept(TetrisMessage.Type.INPUT_RIGHT);
                    initialDAS_P2 = true; lastDAS_P2 = now;
                } else if (now - lastDAS_P2 > 170 && now - lastRepeat_P2 > 50) {
                    outMessage.accept(TetrisMessage.Type.INPUT_RIGHT);
                    lastRepeat_P2 = now;
                }
            }
            if (softDrp) outMessage.accept(TetrisMessage.Type.INPUT_SOFT_DROP);
            return;
        }

        if (isLanHost) {
            boolean goLeft  = activeKeys.contains(KeyCode.A)    || activeKeys.contains(KeyCode.LEFT);
            boolean goRight = activeKeys.contains(KeyCode.D)    || activeKeys.contains(KeyCode.RIGHT);
            boolean softDrp = activeKeys.contains(KeyCode.DOWN) || activeKeys.contains(KeyCode.S);
            if (goLeft) {
                if (!initialDAS_P1) {
                    logic.moveLeft(logic.p1);
                    initialDAS_P1 = true; lastDAS_P1 = now;
                } else if (now - lastDAS_P1 > 170 && now - lastRepeat_P1 > 50) {
                    logic.moveLeft(logic.p1); lastRepeat_P1 = now;
                }
            } else if (goRight) {
                if (!initialDAS_P1) {
                    logic.moveRight(logic.p1);
                    initialDAS_P1 = true; lastDAS_P1 = now;
                } else if (now - lastDAS_P1 > 170 && now - lastRepeat_P1 > 50) {
                    logic.moveRight(logic.p1); lastRepeat_P1 = now;
                }
            }
            if (softDrp) logic.softDrop(logic.p1);
            return;
        }

        // Two-blocks mode input handling
        if (logic.twoBlocksMode) {
            // P1 piece1: LEFT / RIGHT arrows (move), UP = rotate, DOWN = hard drop (handled in key press)
            if (activeKeys.contains(KeyCode.LEFT)) {
                if (!initialDAS_P1) {
                    logic.moveLeftPiece1(logic.p1); initialDAS_P1 = true; lastDAS_P1 = now;
                } else if (now - lastDAS_P1 > 170 && now - lastRepeat_P1 > 50) {
                    logic.moveLeftPiece1(logic.p1); lastRepeat_P1 = now;
                }
            } else if (activeKeys.contains(KeyCode.RIGHT)) {
                if (!initialDAS_P1) {
                    logic.moveRightPiece1(logic.p1); initialDAS_P1 = true; lastDAS_P1 = now;
                } else if (now - lastDAS_P1 > 170 && now - lastRepeat_P1 > 50) {
                    logic.moveRightPiece1(logic.p1); lastRepeat_P1 = now;
                }
            }

            // P1 piece2: J / L (move), I = rotate, K = hard drop (handled in key press)
            if (activeKeys.contains(KeyCode.J)) {
                if (!initialDAS_P1b) {
                    logic.moveLeftPiece2(logic.p1); initialDAS_P1b = true; lastDAS_P1b = now;
                } else if (now - lastDAS_P1b > 170 && now - lastRepeat_P1b > 50) {
                    logic.moveLeftPiece2(logic.p1); lastRepeat_P1b = now;
                }
            } else if (activeKeys.contains(KeyCode.L)) {
                if (!initialDAS_P1b) {
                    logic.moveRightPiece2(logic.p1); initialDAS_P1b = true; lastDAS_P1b = now;
                } else if (now - lastDAS_P1b > 170 && now - lastRepeat_P1b > 50) {
                    logic.moveRightPiece2(logic.p1); lastRepeat_P1b = now;
                }
            }

            // P2 piece1: A / D (move), S = rotate, W = hard drop (handled in key press)
            if (activeKeys.contains(KeyCode.A)) {
                if (!initialDAS_P2) {
                    logic.moveLeftPiece1(logic.p2); initialDAS_P2 = true; lastDAS_P2 = now;
                } else if (now - lastDAS_P2 > 170 && now - lastRepeat_P2 > 50) {
                    logic.moveLeftPiece1(logic.p2); lastRepeat_P2 = now;
                }
            } else if (activeKeys.contains(KeyCode.D)) {
                if (!initialDAS_P2) {
                    logic.moveRightPiece1(logic.p2); initialDAS_P2 = true; lastDAS_P2 = now;
                } else if (now - lastDAS_P2 > 170 && now - lastRepeat_P2 > 50) {
                    logic.moveRightPiece1(logic.p2); lastRepeat_P2 = now;
                }
            }

            // P2 piece2: F / H (move), T = rotate, G = hard drop (handled in key press)
            if (activeKeys.contains(KeyCode.F)) {
                if (!initialDAS_P2b) {
                    logic.moveLeftPiece2(logic.p2); initialDAS_P2b = true; lastDAS_P2b = now;
                } else if (now - lastDAS_P2b > 170 && now - lastRepeat_P2b > 50) {
                    logic.moveLeftPiece2(logic.p2); lastRepeat_P2b = now;
                }
            } else if (activeKeys.contains(KeyCode.H)) {
                if (!initialDAS_P2b) {
                    logic.moveRightPiece2(logic.p2); initialDAS_P2b = true; lastDAS_P2b = now;
                } else if (now - lastDAS_P2b > 170 && now - lastRepeat_P2b > 50) {
                    logic.moveRightPiece2(logic.p2); lastRepeat_P2b = now;
                }
            }
            return;
        }


        if (logic.horizontalMode) {
            // Horizontal Mode Controls (P1 falls RIGHT, P2 falls LEFT)
            if (activeKeys.contains(KeyCode.DOWN)) {
                if (!initialDAS_P1) { logic.moveLeft(logic.p1); initialDAS_P1 = true; lastDAS_P1 = now; }
                else if (now - lastDAS_P1 > 170 && now - lastRepeat_P1 > 50) { logic.moveLeft(logic.p1); lastRepeat_P1 = now; }
            } else if (activeKeys.contains(KeyCode.UP)) {
                if (!initialDAS_P1) { logic.moveRight(logic.p1); initialDAS_P1 = true; lastDAS_P1 = now; }
                else if (now - lastDAS_P1 > 170 && now - lastRepeat_P1 > 50) { logic.moveRight(logic.p1); lastRepeat_P1 = now; }
            }
            if (activeKeys.contains(KeyCode.RIGHT)) logic.softDrop(logic.p1);

            if (activeKeys.contains(KeyCode.S)) {
                if (!initialDAS_P2) { logic.moveLeft(logic.p2); initialDAS_P2 = true; lastDAS_P2 = now; }
                else if (now - lastDAS_P2 > 170 && now - lastRepeat_P2 > 50) { logic.moveLeft(logic.p2); lastRepeat_P2 = now; }
            } else if (activeKeys.contains(KeyCode.W)) {
                if (!initialDAS_P2) { logic.moveRight(logic.p2); initialDAS_P2 = true; lastDAS_P2 = now; }
                else if (now - lastDAS_P2 > 170 && now - lastRepeat_P2 > 50) { logic.moveRight(logic.p2); lastRepeat_P2 = now; }
            }
            if (activeKeys.contains(KeyCode.A)) logic.softDrop(logic.p2);
        } else {
            // Standard local controls
            if (activeKeys.contains(KeyCode.LEFT)) {
                if (!initialDAS_P1) {
                    logic.moveLeft(logic.p1); initialDAS_P1 = true; lastDAS_P1 = now;
                } else if (now - lastDAS_P1 > 170 && now - lastRepeat_P1 > 50) {
                    logic.moveLeft(logic.p1); lastRepeat_P1 = now;
                }
            } else if (activeKeys.contains(KeyCode.RIGHT)) {
                if (!initialDAS_P1) {
                    logic.moveRight(logic.p1); initialDAS_P1 = true; lastDAS_P1 = now;
                } else if (now - lastDAS_P1 > 170 && now - lastRepeat_P1 > 50) {
                    logic.moveRight(logic.p1); lastRepeat_P1 = now;
                }
            }
            if (activeKeys.contains(KeyCode.DOWN)) logic.softDrop(logic.p1);

            if (activeKeys.contains(KeyCode.A)) {
                if (!initialDAS_P2) {
                    logic.moveLeft(logic.p2); initialDAS_P2 = true; lastDAS_P2 = now;
                } else if (now - lastDAS_P2 > 170 && now - lastRepeat_P2 > 50) {
                    logic.moveLeft(logic.p2); lastRepeat_P2 = now;
                }
            } else if (activeKeys.contains(KeyCode.D)) {
                if (!initialDAS_P2) {
                    logic.moveRight(logic.p2); initialDAS_P2 = true; lastDAS_P2 = now;
                } else if (now - lastDAS_P2 > 170 && now - lastRepeat_P2 > 50) {
                    logic.moveRight(logic.p2); lastRepeat_P2 = now;
                }
            }
            if (activeKeys.contains(KeyCode.W)) logic.softDrop(logic.p2);
        }
    }

    // Handle single presses (rotate, hard drop)
    public void setupKeyEvents() {
        setOnKeyPressed(e -> {
            activeKeys.add(e.getCode());
            if (isClient) {
                switch(e.getCode()) {
                    case DOWN: case S:      outMessage.accept(TetrisMessage.Type.INPUT_ROTATE_CW);  break;
                    case Z:    case Q:      outMessage.accept(TetrisMessage.Type.INPUT_ROTATE_CCW); break;
                    case SPACE: case SHIFT: outMessage.accept(TetrisMessage.Type.INPUT_HARD_DROP);  break;
                    default: break;
                }
                return;
            }
            if (isLanHost) {
                switch(e.getCode()) {
                    case UP:   case W:      logic.rotateCW(logic.p1);  break;
                    case Z:    case Q:      logic.rotateCCW(logic.p1); break;
                    case SPACE: case SHIFT: logic.hardDrop(logic.p1);  break;
                    default: break;
                }
                return;
            }
            // Independent per-piece key actions (Two-blocks mode)
            if (logic.twoBlocksMode) {
                switch(e.getCode()) {
                    // P1 piece1
                    case UP:     logic.rotateCWPiece1(logic.p1);  break;  // rotate piece1
                    case DOWN:   logic.hardDropPiece1(logic.p1);  break;  // hard drop piece1
                    // P1 piece2
                    case I:      logic.rotateCWPiece2(logic.p1);  break;  // rotate piece2
                    case K:      logic.hardDropPiece2(logic.p1);  break;  // hard drop piece2
                    // P2 piece1
                    case S:      logic.rotateCWPiece1(logic.p2);  break;  // rotate piece1
                    case W:      logic.hardDropPiece1(logic.p2);  break;  // hard drop piece1
                    // P2 piece2
                    case T:      logic.rotateCWPiece2(logic.p2);  break;  // rotate piece2
                    case G:      logic.hardDropPiece2(logic.p2);  break;  // hard drop piece2
                    default: break;
                }
                return;
            }

            if (logic.horizontalMode) {
                switch(e.getCode()) {
                    case LEFT:  logic.hardDrop(logic.p1); break;
                    case X:     logic.rotateCW(logic.p1); break;
                    case Z:     logic.rotateCCW(logic.p1); break;
                    case D:     logic.hardDrop(logic.p2); break;
                    case E:     logic.rotateCW(logic.p2); break;
                    case Q:     logic.rotateCCW(logic.p2); break;
                    default: break;
                }
            } else {
                switch(e.getCode()) {
                    case UP:    logic.rotateCW(logic.p1);  break;
                    case Z:     logic.rotateCCW(logic.p1); break;
                    case SPACE: logic.hardDrop(logic.p1);  break;
                    case S:     logic.rotateCW(logic.p2);  break;
                    case Q:     logic.rotateCCW(logic.p2); break;
                    case SHIFT: logic.hardDrop(logic.p2);  break;
                    default: break;
                }
            }
        });
    }

    public void showDisconnectOverlay(Runnable onQuit) {
        stop();
        Platform.runLater(() -> {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            double w = canvas.getWidth(), h = canvas.getHeight();
            gc.setFill(Color.color(0, 0, 0, 0.82));
            gc.fillRect(0, 0, w, h);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 48));
            gc.setFill(Color.web("#e74c3c"));
            String line1 = "CONNECTION LOST";
            gc.fillText(line1, w / 2 - line1.length() * 13, h / 2 - 30);
            gc.setFont(Font.font("SansSerif", 22));
            gc.setFill(Color.web("#8c8caa"));
            String line2 = "The other player has disconnected.";
            gc.fillText(line2, w / 2 - line2.length() * 6, h / 2 + 20);
            Button quitBtn = new Button("Return to Menu");
            quitBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 28;");
            quitBtn.setOnAction(ev -> onQuit.run());
            StackPane.setAlignment(quitBtn, javafx.geometry.Pos.CENTER);
            StackPane.setMargin(quitBtn, new javafx.geometry.Insets(80, 0, 0, 0));
            getChildren().add(quitBtn);
        });
    }

    // Rendering

    private void render() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0f0f1e"));
        gc.fillRect(0, 0, w, h);

        boolean horiz = logic.horizontalMode;

        // Visual horizontal rotation for horizontal board mode
        int visCols = horiz ? Math.max(logic.p1.board.visibleRows, logic.p2.board.visibleRows) : Board.WIDTH;
        int visRows = horiz ? Board.WIDTH : Math.max(logic.p1.board.visibleRows, logic.p2.board.visibleRows);

        if (horiz) {
            int totalCols = logic.p1.board.visibleRows + logic.p2.board.visibleRows;
            int byWidth = (int)((w - 60) / (totalCols + 2));
            int byHeight = (int)((h - 150) / Board.WIDTH);
            CELL = Math.max(8, Math.min(byWidth, byHeight));
            BOARD_W = visCols * CELL;
            BOARD_H = visRows * CELL;

            int boardW2 = boardPixelWidth(logic.p2, true);
            int boardW1 = boardPixelWidth(logic.p1, true);
            double gap = 20;
            double offX2 = w / 2 - (boardW1 + boardW2 + gap) / 2;
            double offX1 = offX2 + boardW2 + gap;
            double offY = Math.max(95, h / 2 - BOARD_H / 2);

            drawPlayerState(gc, logic.p2, offX2, offY, horiz);
            drawPlayerState(gc, logic.p1, offX1, offY, horiz);

            double labelX = w / 2 - 60;
            double labelY = h - 30;
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
            gc.setFill(Color.web("#00d2ff"));
            gc.fillText("\u2194 HORIZONTAL MODE", labelX, labelY);
            if (logic.twoBlocksMode) {
                gc.setFill(Color.web("#a882ff"));
                gc.fillText("\u25c8 DUAL BLOCK MODE", labelX, labelY + 15);
            }
        } else {
            int totalRows = logic.p1.board.visibleRows + logic.p2.board.visibleRows;
            int byHeight = (int)((h - 40) / (totalRows + 2));
            int byWidth = (int)((w - 260) / Board.WIDTH);
            CELL = Math.max(8, Math.min(byHeight, byWidth));
            BOARD_W = visCols * CELL;
            BOARD_H = visRows * CELL;

            int boardH2 = boardPixelHeight(logic.p2, false);
            double offY2 = 10;
            double offY1 = offY2 + boardH2 + 20;
            double offX  = w / 2 - BOARD_W / 2;

            drawPlayerState(gc, logic.p2, offX, offY2, horiz);
            drawPlayerState(gc, logic.p1, offX, offY1, horiz);

            double labelX = offX + BOARD_W + 10;
            if (logic.twoBlocksMode) {
                gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
                gc.setFill(Color.web("#a882ff"));
                gc.fillText("\u25c8 DUAL BLOCK MODE", labelX, offY1 - 5);
            }
        }

        // Game over overlay
        if (logic.p1.isGameOver && logic.p2.isGameOver) {
            gc.setFill(Color.color(0, 0, 0, 0.8));
            gc.fillRect(0, 0, w, h);
            if (!restartBtn.isVisible()) restartBtn.setVisible(true);

            String msg = "DRAW!";
            Color c = Color.WHITE;
            if (logic.p1.score > logic.p2.score) {
                msg = logic.p1.name.toUpperCase() + " WINS!";
                c = Color.web("#00d2ff");
            } else if (logic.p2.score > logic.p1.score) {
                msg = logic.p2.name.toUpperCase() + " WINS!";
                c = Color.web("#ff6b9d");
            }
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 56));
            gc.setFill(Color.BLACK);
            gc.fillText(msg, w/2 - msg.length() * 15, h/2 - 20 + 4);
            gc.setFill(c);
            gc.fillText(msg, w/2 - msg.length() * 15 - 4, h/2 - 20);
        }
    }

    private int boardPixelWidth(PlayerState p, boolean horiz) {
        return (horiz ? p.board.visibleRows : Board.WIDTH) * CELL;
    }

    private int boardPixelHeight(PlayerState p, boolean horiz) {
        return (horiz ? Board.WIDTH : p.board.visibleRows) * CELL;
    }

    private void drawPlayerState(GraphicsContext gc, PlayerState p, double offX, double offY, boolean horiz) {
        int boardW = boardPixelWidth(p, horiz);
        int boardH = boardPixelHeight(p, horiz);

        // Stats panel
        double textX = horiz ? offX : offX - 120;
        double textY = horiz ? offY - 80 : offY + 20;

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 20));
        gc.fillText(p.name, textX, textY);

        gc.setFont(Font.font("SansSerif", 16));
        if (horiz) {
            gc.setFill(Color.web("#8c8caa"));
            gc.fillText("Score: ", textX, textY + 25);
            gc.setFill(Color.WHITE);
            gc.fillText(String.valueOf(p.score), textX + 50, textY + 25);

            gc.setFill(Color.web("#8c8caa"));
            gc.fillText("Lines: ", textX + 120, textY + 25);
            gc.setFill(Color.WHITE);
            gc.fillText(String.valueOf(p.linesCleared), textX + 170, textY + 25);

            double effX = textX;
            double effY = textY + 45;
            long now = System.currentTimeMillis();
            if (now < p.slowDownEndTime) {
                gc.setFill(Color.web("#00d2ff"));
                gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
                gc.fillText("\u2b07 SLOWED", effX, effY);
                effX += 80;
            }
            if (now < p.speedUpEndTime) {
                gc.setFill(Color.web("#ff6b9d"));
                gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
                gc.fillText("\u2b06 SPED UP", effX, effY);
                effX += 80;
            }
            // Show rotation lock indicator if active
            if (now < p.rotationDelayEndTime) {
                gc.setFill(Color.web("#ff9900"));
                gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
                gc.fillText("\u27f3 ROT. LOCKED", effX, effY);
                effX += 100;
            }

            // Controls legend in two-blocks mode
            if (logic.twoBlocksMode) {
                double ctrlX = textX + 200; // shift to the right
                double ctrlY = textY;
                gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 11));
                gc.setFill(Color.web("#a882ff"));
                gc.fillText("Controls:", ctrlX, ctrlY);
                
                gc.setFont(Font.font("SansSerif", 10));
                if (p.id == 1) {
                    gc.setFill(Color.web("#00d2ff"));
                    gc.fillText("B1: \u2190 \u2192 (Move) | \u2191 (Rot) | \u2193 (Drop)", ctrlX, ctrlY + 13);
                    gc.setFill(Color.web("#ff6b9d"));
                    gc.fillText("B2: J L (Move) | I (Rot) | K (Drop)", ctrlX, ctrlY + 26);
                } else {
                    gc.setFill(Color.web("#00d2ff"));
                    gc.fillText("B1: A D (Move) | S (Rot) | W (Drop)", ctrlX, ctrlY + 13);
                    gc.setFill(Color.web("#ff6b9d"));
                    gc.fillText("B2: F H (Move) | T (Rot) | G (Drop)", ctrlX, ctrlY + 26);
                }
            }
        } else {
            gc.setFill(Color.web("#8c8caa"));
            gc.fillText("Score", textX, textY + 40);
            gc.setFill(Color.WHITE);
            gc.fillText(String.valueOf(p.score), textX, textY + 60);

            gc.setFill(Color.web("#8c8caa"));
            gc.fillText("Lines", textX, textY + 100);
            gc.setFill(Color.WHITE);
            gc.fillText(String.valueOf(p.linesCleared), textX, textY + 120);

            double effY = textY + 145;
            long now = System.currentTimeMillis();
            if (now < p.slowDownEndTime) {
                gc.setFill(Color.web("#00d2ff"));
                gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
                gc.fillText("\u2b07 SLOWED", textX, effY);
                effY += 16;
            }
            if (now < p.speedUpEndTime) {
                gc.setFill(Color.web("#ff6b9d"));
                gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
                gc.fillText("\u2b06 SPED UP", textX, effY);
                effY += 16;
            }
            // Show rotation lock indicator if active
            if (now < p.rotationDelayEndTime) {
                gc.setFill(Color.web("#ff9900"));
                gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
                gc.fillText("\u27f3 ROT. LOCKED", textX, effY);
                effY += 16;
            }

            // Controls legend in two-blocks mode
            if (logic.twoBlocksMode) {
                effY += 6;
                gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 11));
                gc.setFill(Color.web("#a882ff"));
                gc.fillText("Controls:", textX, effY);
                effY += 14;
                gc.setFont(Font.font("SansSerif", 10));
                if (p.id == 1) {
                    // Player 1 controls
                    gc.setFill(Color.web("#00d2ff"));
                    gc.fillText("B1: \u2190 \u2192 (Move)", textX, effY); effY += 13;
                    gc.fillText("       \u2191 (Rot)  \u2193 (Drop)", textX, effY); effY += 13;
                    gc.setFill(Color.web("#ff6b9d"));
                    gc.fillText("B2: J L (Move)", textX, effY); effY += 13;
                    gc.fillText("       I (Rot)  K (Drop)", textX, effY);
                } else {
                    // Player 2 controls
                    gc.setFill(Color.web("#00d2ff"));
                    gc.fillText("B1: A D (Move)", textX, effY); effY += 13;
                    gc.fillText("       S (Rot)  W (Drop)", textX, effY); effY += 13;
                    gc.setFill(Color.web("#ff6b9d"));
                    gc.fillText("B2: F H (Move)", textX, effY); effY += 13;
                    gc.fillText("       T (Rot)  G (Drop)", textX, effY);
                }
            }
        }

        // Next piece preview
        double nextX = horiz ? offX : offX + boardW + 30;
        double nextY = horiz ? offY + boardH + 20 : offY + 30;

        gc.setFont(Font.font("SansSerif", 16));
        gc.setFill(Color.web("#8c8caa"));
        
        if (horiz) {
            gc.fillText("Next", nextX, nextY);
            if (p.nextPiece != null) {
                drawShape(gc, p.nextPiece, nextX, nextY + 15, false);
            }
            if (logic.twoBlocksMode && p.nextPiece2 != null) {
                gc.setFill(Color.web("#8c8caa"));
                gc.fillText("Next2", nextX + 120, nextY);
                drawShape(gc, p.nextPiece2, nextX + 120, nextY + 15, false);
            }
        } else {
            gc.fillText("Next", nextX, nextY);
            if (p.nextPiece != null) {
                drawShape(gc, p.nextPiece, nextX, nextY + 20, false);
            }
            if (logic.twoBlocksMode && p.nextPiece2 != null) {
                gc.setFill(Color.web("#8c8caa"));
                gc.fillText("Next2", nextX, nextY + 85);
                drawShape(gc, p.nextPiece2, nextX, nextY + 105, false);
            }
        }

        // Board background
        gc.setFill(Color.web("#202020"));
        gc.fillRect(offX, offY, boardW, boardH);

        // Grid lines
        gc.setStroke(Color.web("#303030"));
        gc.setLineWidth(1);
        int visCols = horiz ? p.board.visibleRows : Board.WIDTH;
        int visRows = horiz ? Board.WIDTH : p.board.visibleRows;
        for (int i = 0; i <= visCols; i++) gc.strokeLine(offX + i*CELL, offY, offX + i*CELL, offY + boardH);
        for (int i = 0; i <= visRows; i++) gc.strokeLine(offX, offY + i*CELL, offX + boardW, offY + i*CELL);

        // Locked cells
        for (int y = p.board.getTopRow(); y < Board.HEIGHT; y++) {
            for (int x = 0; x < Board.WIDTH; x++) {
                if (p.board.grid[y][x] != null) {
                    drawCell(gc, offX, offY, x, y, p.board.grid[y][x], p, horiz);
                }
            }
        }

        // Ghost pieces
        Tetromino ghost = logic.getGhost(p);
        if (ghost != null) drawGhostShape(gc, ghost, offX, offY, p, horiz);

        // Second piece ghost
        if (logic.twoBlocksMode) {
            Tetromino ghost2 = logic.getGhost2(p);
            if (ghost2 != null) drawGhostShape(gc, ghost2, offX, offY, p, horiz);
        }

        // Power-ups

        // Teleporter/swap power-up
        if (p.board.hasSwapPowerup) {
            double[] pos = pwPos(offX, offY, p.board.swapX, p.board.swapY, p, horiz);
            gc.setFill(Color.MAGENTA);
            gc.fillOval(pos[0]+2, pos[1]+2, CELL-4, CELL-4);
            gc.setStroke(Color.web("#ff00ff", 0.7));
            gc.setLineWidth(1.5);
            gc.strokeOval(pos[0]+1, pos[1]+1, CELL-2, CELL-2);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, Math.max(8, CELL-4)));
            gc.fillText("\u21c4", pos[0]+CELL*0.1, pos[1]+CELL*0.78);
        }

        // Speed up opponent power-up
        if (p.board.hasSpeedUpPowerup) {
            double[] pos = pwPos(offX, offY, p.board.speedUpX, p.board.speedUpY, p, horiz);
            gc.setFill(Color.RED);
            gc.fillRect(pos[0], pos[1], CELL, CELL);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, Math.max(8, CELL-4)));
            gc.fillText("F", pos[0]+CELL/4.0, pos[1]+CELL*0.75);
        }

        // Slow self power-up
        if (p.board.hasSlowSelfPowerup) {
            double[] pos = pwPos(offX, offY, p.board.slowSelfX, p.board.slowSelfY, p, horiz);
            gc.setFill(Color.BLUE);
            gc.fillRect(pos[0], pos[1], CELL, CELL);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, Math.max(8, CELL-4)));
            gc.fillText("S", pos[0]+CELL/4.0, pos[1]+CELL*0.75);
        }

        // Delay opponent rotation power-up
        if (p.board.hasDelayOpponentRotPowerup) {
            double[] pos = pwPos(offX, offY, p.board.delayOpponentRotX, p.board.delayOpponentRotY, p, horiz);
            gc.setFill(Color.ORANGE);
            gc.fillRect(pos[0], pos[1], CELL, CELL);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, Math.max(8, CELL-4)));
            gc.fillText("R", pos[0]+CELL/4.0, pos[1]+CELL*0.75);
        }

        // Delay self rotation power-up
        if (p.board.hasDelaySelfRotPowerup) {
            double[] pos = pwPos(offX, offY, p.board.delaySelfRotX, p.board.delaySelfRotY, p, horiz);
            gc.setFill(Color.PURPLE);
            gc.fillRect(pos[0], pos[1], CELL, CELL);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, Math.max(8, CELL-4)));
            gc.fillText("D", pos[0]+CELL/4.0, pos[1]+CELL*0.75);
        }

        // Slow opponent power-up
        if (p.board.hasSlowOpponentPowerup) {
            double[] pos = pwPos(offX, offY, p.board.slowOpponentX, p.board.slowOpponentY, p, horiz);
            gc.setFill(Color.web("#00d2ff"));
            gc.fillRect(pos[0], pos[1], CELL, CELL);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, Math.max(8, CELL-4)));
            gc.fillText("\u2745", pos[0]+CELL*0.1, pos[1]+CELL*0.78);
        }

        if (p.board.hasPortal) {
            double[] pos = pwPos(offX, offY, p.board.portalX, p.board.portalY, p, horiz);
            gc.setFill(Color.web("#18f5d5"));
            gc.fillOval(pos[0]+1, pos[1]+1, CELL-2, CELL-2);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(pos[0]+2, pos[1]+2, CELL-4, CELL-4);
            gc.setFill(Color.web("#0f0f1e"));
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, Math.max(8, CELL-4)));
            gc.fillText("P", pos[0]+CELL/4.0, pos[1]+CELL*0.75);
        }

        // Swap/teleport flash
        if (System.currentTimeMillis() < p.board.swapFlashEndTime) {
            gc.setFill(Color.color(1, 0, 1, 0.5));
            gc.fillRect(offX, offY, boardW, boardH);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 34));
            gc.fillText("TELEPORT!", offX + boardW * 0.1, offY + boardH / 2.0);
        }

        if (System.currentTimeMillis() < p.board.portalFlashEndTime) {
            gc.setFill(Color.color(0, 0.82, 1, 0.35));
            gc.fillRect(offX, offY, boardW, boardH);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 30));
            gc.fillText("PORTAL", offX + boardW * 0.18, offY + boardH / 2.0);
        }

        if (System.currentTimeMillis() < p.board.speedUpFlashEndTime) {
            gc.setFill(Color.color(1, 0, 0, 0.4));
            gc.fillRect(offX, offY, boardW, boardH);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 30));
            gc.fillText("SPED UP!", offX + boardW * 0.18, offY + boardH / 2.0);
        }

        if (System.currentTimeMillis() < p.board.slowSelfFlashEndTime) {
            gc.setFill(Color.color(0, 0, 1, 0.4));
            gc.fillRect(offX, offY, boardW, boardH);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 30));
            gc.fillText("SLOWED!", offX + boardW * 0.18, offY + boardH / 2.0);
        }

        if (System.currentTimeMillis() < p.board.delayOpponentRotFlashEndTime) {
            gc.setFill(Color.color(0.9, 0.5, 0, 0.4));
            gc.fillRect(offX, offY, boardW, boardH);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
            gc.fillText("ROT. LOCKED!", offX + boardW * 0.05, offY + boardH / 2.0);
        }

        if (System.currentTimeMillis() < p.board.delaySelfRotFlashEndTime) {
            gc.setFill(Color.color(0.5, 0, 0.5, 0.4));
            gc.fillRect(offX, offY, boardW, boardH);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
            gc.fillText("ROT. LOCKED!", offX + boardW * 0.05, offY + boardH / 2.0);
        }

        if (System.currentTimeMillis() < p.board.slowOpponentFlashEndTime) {
            gc.setFill(Color.color(0, 0.8, 1, 0.4));
            gc.fillRect(offX, offY, boardW, boardH);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 30));
            gc.fillText("SLOWED!", offX + boardW * 0.18, offY + boardH / 2.0);
        }

        // Active falling pieces
        if (!p.isGameOver) {
            if (p.activePiece != null) {
                drawActiveShape(gc, p.activePiece, offX, offY, p, horiz);
            }
            // Second active piece
            if (logic.twoBlocksMode && p.activePiece2 != null) {
                drawActiveShape(gc, p.activePiece2, offX, offY, p, horiz);
            }
        } else {
            gc.setFill(Color.color(0, 0, 0, 0.5));
            gc.fillRect(offX, offY, boardW, boardH);
        }
    }

    // Coordinate helpers

    // Convert board grid position (bx, by) to screen pixels, handling rotations and player 2 inversion.
    private double[] toRenderPos(double offX, double offY, int bx, int by, PlayerState p, boolean horiz) {
        boolean inverted = p.id == 2;
        int row = by - p.board.getTopRow();
        double rx, ry;
        if (horiz) {
            // 90 degrees CCW: col=by, row=WIDTH-1-bx
            if (inverted) {
                // 180 degrees on top of CCW = 90 degrees CW: col=HEIGHT-1-by, row=bx
                rx = offX + (p.board.visibleRows - 1 - row) * CELL;
                ry = offY + bx * CELL;
            } else {
                rx = offX + row * CELL;
                ry = offY + (Board.WIDTH - 1 - bx) * CELL;
            }
        } else {
            if (inverted) {
                rx = offX + (Board.WIDTH  - 1 - bx) * CELL;
                ry = offY + (p.board.visibleRows - 1 - row) * CELL;
            } else {
                rx = offX + bx * CELL;
                ry = offY + row * CELL;
            }
        }
        return new double[]{rx, ry};
    }

    // Helper for converting board grid coordinates to render positions for power-ups.
    private double[] pwPos(double offX, double offY, int bx, int by, PlayerState p, boolean horiz) {
        return toRenderPos(offX, offY, bx, by, p, horiz);
    }

    // Drawing helpers

    private void drawShape(GraphicsContext gc, Tetromino t, double px, double py, boolean isGhost) {
        int[][] shape = t.getShape();
        // Next-piece preview — always shown in standard orientation (no horiz rotation)
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    double cx = px + c * CELL;
                    double cy = py + r * CELL;
                    drawBeveledCell(gc, cx, cy, t.colorHex, isGhost);
                    drawSpecialMark(gc, t.type, cx, cy);
                }
            }
        }
    }

    private void drawActiveShape(GraphicsContext gc, Tetromino t,
                                 double offX, double offY, PlayerState p, boolean horiz) {
        int[][] shape = t.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    double[] pos = toRenderPos(offX, offY, t.x + c, t.y + r, p, horiz);
                    drawBeveledCell(gc, pos[0], pos[1], t.colorHex, false);
                    drawSpecialMark(gc, t.type, pos[0], pos[1]);
                }
            }
        }
    }

    private void drawGhostShape(GraphicsContext gc, Tetromino t,
                                double offX, double offY, PlayerState p, boolean horiz) {
        int[][] shape = t.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    double[] pos = toRenderPos(offX, offY, t.x + c, t.y + r, p, horiz);
                    gc.setStroke(Color.web(t.colorHex));
                    gc.setLineWidth(2);
                    gc.strokeRect(pos[0] + 2, pos[1] + 2, CELL - 4, CELL - 4);
                }
            }
        }
    }

    private void drawCell(GraphicsContext gc, double offX, double offY,
                          int bx, int by, String hex, PlayerState p, boolean horiz) {
        double[] pos = toRenderPos(offX, offY, bx, by, p, horiz);
        drawBeveledCell(gc, pos[0], pos[1], hex, false);
    }

    private void drawSpecialMark(GraphicsContext gc, Tetromino.Type type, double cx, double cy) {
        String mark = null;
        if (type == Tetromino.Type.RADIUS_BOMB) mark = "R";
        if (type == Tetromino.Type.COLUMN_BOMB) mark = "C";
        if (mark == null) return;
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("SansSerif", FontWeight.BOLD, Math.max(8, CELL - 3)));
        gc.fillText(mark, cx + CELL * 0.25, cy + CELL * 0.78);
    }

    private void drawBeveledCell(GraphicsContext gc, double cx, double cy, String hex, boolean isGhost) {
        Color base = Color.web(hex);
        if (isGhost) {
            gc.setFill(Color.color(base.getRed(), base.getGreen(), base.getBlue(), 0.3));
            gc.fillRect(cx, cy, CELL, CELL);
            return;
        }
        gc.setFill(base);
        gc.fillRect(cx, cy, CELL, CELL);
        // Highlight top-left
        gc.setFill(Color.color(1, 1, 1, 0.4));
        gc.fillPolygon(
            new double[]{cx, cx+CELL, cx+CELL-4, cx+4, cx+4},
            new double[]{cy, cy,      cy+4,       cy+4, cy+CELL-4}, 5);
        // Shadow bottom-right
        gc.setFill(Color.color(0, 0, 0, 0.4));
        gc.fillPolygon(
            new double[]{cx,      cx+CELL, cx+CELL, cx+CELL-4, cx+4},
            new double[]{cy+CELL, cy+CELL, cy,      cy+4,       cy+CELL-4}, 5);
    }
}
