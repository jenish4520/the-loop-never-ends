package seda_project.control_alt_defeat.tetris;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import seda_project.control_alt_defeat.gamebox.GameHub;
import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TetrisApp {

    private Stage stage;
    private GameHub hub;
    // store these to shut down network on exit
    private TetrisHost activeHost;
    private TetrisClient activeClient;

    // FR03 / FR04 advanced options (local game only)
    private boolean optTwoBlocks = false;
    private boolean optHorizontal = false;
    private int optSpeedLevel = 3;

    private static final Color CARD_BG = Color.web("#1e2341");
    private static final Color CARD_HOVER = Color.web("#282d50");
    private static final Color ACCENT_PURPLE = Color.web("#a882ff");
    private static final Color ACCENT_CYAN = Color.web("#00d2ff");
    private static final Color ACCENT_PINK = Color.web("#ff6b9d");
    private static final Color TEXT_DIM = Color.web("#8c8caa");

    public TetrisApp(Stage stage, GameHub hub) {
        this.stage = stage;
        this.hub = hub;
        // making sure the network shuts down if the window is closed
        stage.addEventHandler(javafx.stage.WindowEvent.WINDOW_HIDING, e -> closeNetwork());
    }

    // Clean up LAN sockets on close or navigation
    private void closeNetwork() {
        if (activeHost != null) {
            activeHost.close();
            activeHost = null;
        }
        if (activeClient != null) {
            activeClient.close();
            activeClient = null;
        }
    }

    public void show() {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f0f1e;");

        Label title = new Label("TETRIS");
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 52));
        title.setTextFill(ACCENT_PURPLE);

        Label subtitle = new Label("Choose How You Want to Play");
        subtitle.setFont(Font.font("SansSerif", 18));
        subtitle.setTextFill(TEXT_DIM);

        HBox cardsRow = new HBox(24);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.setPadding(new Insets(40, 0, 0, 0));

        cardsRow.getChildren().addAll(
                buildModeCard("💻", "LOCAL GAME", "Two Players, One Screen", ACCENT_PURPLE, this::startLocalGame),
                buildModeCard("⌂", "HOST GAME", "Create a LAN Session", ACCENT_CYAN, this::showHostMenu),
                buildModeCard("→", "JOIN GAME", "Connect to a Host", ACCENT_PINK, this::showJoinMenu));

        Label advLabel = new Label("Advanced Options (Local Game)");
        advLabel.setFont(Font.font("SansSerif", 14));
        advLabel.setTextFill(TEXT_DIM);

        Button twoBlocksBtn = buildToggleButton(
                "🎮 Control 2 Blocks",
                "Each player controls two falling pieces simultaneously",
                Color.web("#a882ff"), optTwoBlocks);
        twoBlocksBtn.setOnAction(e -> {
            optTwoBlocks = !optTwoBlocks;
            styleToggleButton(twoBlocksBtn, "Control 2 Blocks", Color.web("#a882ff"), optTwoBlocks);
        });

        Button horizBtn = buildToggleButton(
                "🎮 Horizontal Play",
                "Blocks fall sideways instead of downward",
                Color.web("#00d2ff"), optHorizontal);
        horizBtn.setOnAction(e -> {
            optHorizontal = !optHorizontal;
            styleToggleButton(horizBtn, "↔ Horizontal Play", Color.web("#00d2ff"), optHorizontal);
        });

        HBox advRow = new HBox(16);
        advRow.setAlignment(Pos.CENTER);
        advRow.setPadding(new Insets(10, 0, 0, 0));
        advRow.getChildren().addAll(twoBlocksBtn, horizBtn);

        HBox speedRow = new HBox(12);
        speedRow.setAlignment(Pos.CENTER);
        speedRow.setPadding(new Insets(10, 0, 0, 0));

        Label speedLabel = new Label("Speed");
        speedLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
        speedLabel.setTextFill(TEXT_DIM);

        Label speedValue = new Label(String.valueOf(optSpeedLevel));
        speedValue.setFont(Font.font("SansSerif", FontWeight.BOLD, 18));
        speedValue.setTextFill(ACCENT_CYAN);

        Button speedDown = new Button("-");
        Button speedUp = new Button("+");
        styleSmallButton(speedDown);
        styleSmallButton(speedUp);
        speedDown.setOnAction(e -> {
            if (optSpeedLevel > 1)
                optSpeedLevel--;
            speedValue.setText(String.valueOf(optSpeedLevel));
        });
        speedUp.setOnAction(e -> {
            if (optSpeedLevel < 6)
                optSpeedLevel++;
            speedValue.setText(String.valueOf(optSpeedLevel));
        });

        speedRow.getChildren().addAll(speedLabel, speedDown, speedValue, speedUp);
        // ─────────────────────────────────────────────────────────────────

        HBox toolsRow = new HBox(24);
        toolsRow.setAlignment(Pos.CENTER);
        toolsRow.setPadding(new Insets(20, 0, 0, 0));

        Button designBtn = new Button("Custom Piece Designer");
        styleButton(designBtn);
        designBtn.setOnAction(e -> CustomPieceDesigner.show(stage, this::show));

        Button backBtn = new Button("← Back to Main Menu");
        styleButton(backBtn);
        backBtn.setOnAction(e -> hub.show());

        toolsRow.getChildren().addAll(backBtn, designBtn);

        root.getChildren().addAll(title, subtitle, cardsRow, advLabel, advRow, speedRow, toolsRow);

        Scene scene = new Scene(root, 900, 650);
        stage.setScene(scene);
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

    private void styleButton(Button btn) {
        btn.setStyle(
                "-fx-background-color: #1e2341; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 10 30;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #282d50; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 10 30;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #1e2341; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 10 30;"));
    }

    private void styleSmallButton(Button btn) {
        btn.setStyle(
                "-fx-background-color: #1e2341; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 4 14;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #282d50; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 4 14;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #1e2341; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 4 14;"));
    }

    private void startLocalGame() {
        GameLogic logic = new GameLogic("Player 1", "Player 2", optSpeedLevel);

        logic.twoBlocksMode = optTwoBlocks;
        logic.horizontalMode = optHorizontal;
        logic.initModes();

        TetrisPanel panel = new TetrisPanel(logic, this::show);

        Scene scene = new Scene(panel, 1100, 750);
        stage.setScene(scene);
        stage.centerOnScreen();

        panel.setupKeyEvents();
        panel.start();
    }

    private Button buildToggleButton(String label, String tooltip, Color accent, boolean active) {
        Button btn = new Button(label);
        styleToggleButton(btn, label, accent, active);
        btn.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        return btn;
    }

    private void styleToggleButton(Button btn, String label, Color accent, boolean active) {
        String hex = toHexString(accent);
        if (active) {
            btn.setStyle("-fx-background-color: " + hex + "; -fx-text-fill: #0f0f1e; " +
                    "-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 20; " +
                    "-fx-background-radius: 12;");
        } else {
            btn.setStyle("-fx-background-color: #1e2341; -fx-text-fill: " + hex + "; " +
                    "-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 20; " +
                    "-fx-border-color: " + hex + "; -fx-border-radius: 12; -fx-border-width: 1.5; " +
                    "-fx-background-radius: 12;");
        }
    }

    private void showHostMenu() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f0f1e;");

        Label title = new Label("HOST GAME");
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 32));
        title.setTextFill(Color.WHITE);

        Label p1Label = new Label("Player 1 Name:");
        p1Label.setTextFill(Color.WHITE);
        TextField p1Field = new TextField("Player 1");

        Label status = new Label("Waiting to start...");
        status.setTextFill(Color.GRAY);

        Button startBtn = new Button("Start Hosting (Port 28080)");
        styleButton(startBtn);
        startBtn.setOnAction(e -> {
            closeNetwork(); // kill any old sessions first
            startBtn.setDisable(true);
            status.setText("Hosting on Port 28080... Waiting for Player 2.");

            GameLogic logic = new GameLogic(p1Field.getText(), "Player 2", optSpeedLevel);
            TetrisHost host = new TetrisHost(msg -> {
                if (msg instanceof TetrisMessage) {
                    TetrisMessage tm = (TetrisMessage) msg;
                    if (tm.type == TetrisMessage.Type.PLAYER_NAME && tm.playerName != null) {
                        logic.p2.name = tm.playerName;
                    } else if (tm.type == TetrisMessage.Type.INPUT_LEFT)
                        logic.moveLeft(logic.p2);
                    else if (tm.type == TetrisMessage.Type.INPUT_RIGHT)
                        logic.moveRight(logic.p2);
                    else if (tm.type == TetrisMessage.Type.INPUT_SOFT_DROP)
                        logic.softDrop(logic.p2);
                    else if (tm.type == TetrisMessage.Type.INPUT_HARD_DROP)
                        logic.hardDrop(logic.p2);
                    else if (tm.type == TetrisMessage.Type.INPUT_ROTATE_CW)
                        logic.rotateCW(logic.p2);
                    else if (tm.type == TetrisMessage.Type.INPUT_ROTATE_CCW)
                        logic.rotateCCW(logic.p2);
                }
            }, () -> Platform.runLater(() -> {
                status.setText("Connection Lost!");
                status.setTextFill(Color.RED);
            }));
            activeHost = host;

            new Thread(() -> {
                try {
                    host.start(28080, p1Field.getText());
                    Platform.runLater(() -> {
                        boolean[] broadcastRunning = { true };
                        TetrisPanel[] panelRef = { null };
                        TetrisPanel panel = new TetrisPanel(logic, () -> {
                            broadcastRunning[0] = false;
                            closeNetwork();
                            show();
                        });
                        panelRef[0] = panel;
                        panel.setLanHostMode();
                        panel.setupKeyEvents();

                        host.onDisconnect = () -> Platform.runLater(() -> {
                            broadcastRunning[0] = false;
                            if (panelRef[0] != null) {
                                panelRef[0].showDisconnectOverlay(() -> {
                                    closeNetwork();
                                    show();
                                });
                            }
                        });

                        Thread broadcastThread = new Thread(() -> {
                            while (broadcastRunning[0]) {
                                try {
                                    Thread.sleep(33);
                                } catch (Exception ex) {
                                    break;
                                }
                                TetrisMessage sm = new TetrisMessage(TetrisMessage.Type.STATE_UPDATE);
                                sm.p1 = logic.p1;
                                sm.p2 = logic.p2;
                                host.send(sm);
                            }
                        });
                        broadcastThread.setDaemon(true);
                        broadcastThread.start();

                        Scene scene = new Scene(panel, 1100, 750);
                        stage.setScene(scene);
                        stage.centerOnScreen();
                        panel.start();
                    });
                } catch (Exception ex) {
                    closeNetwork();
                    Platform.runLater(() -> {
                        status.setText("Failed to Host: " + ex.getMessage());
                        startBtn.setDisable(false);
                    });
                }
            }).start();
        });

        Button backBtn = new Button("Back");
        styleButton(backBtn);
        backBtn.setOnAction(e -> {
            closeNetwork();
            show();
        });

        root.getChildren().addAll(title, p1Label, p1Field, startBtn, status, backBtn);
        stage.setScene(new Scene(root, 900, 650));
    }

    private void showJoinMenu() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f0f1e;");

        Label title = new Label("JOIN GAME");
        title.setFont(Font.font("SansSerif", FontWeight.BOLD, 32));
        title.setTextFill(Color.WHITE);

        Label p2Label = new Label("Your Name (Player 2):");
        p2Label.setTextFill(Color.WHITE);
        TextField p2Field = new TextField("Player 2");

        Label ipLabel = new Label("Host IP:");
        ipLabel.setTextFill(Color.WHITE);
        TextField ipField = new TextField("Localhost");

        Label status = new Label("Ready to connect.");
        status.setTextFill(Color.GRAY);

        VBox hostsList = new VBox(5);
        hostsList.setAlignment(Pos.CENTER);

        Button scanBtn = new Button("Scan for LAN Hosts");
        styleButton(scanBtn);
        scanBtn.setOnAction(e -> {
            hostsList.getChildren().clear();
            status.setText("Scanning...");
            new Thread(() -> {
                try (DatagramSocket udpSocket = new DatagramSocket()) {
                    udpSocket.setBroadcast(true);
                    udpSocket.setSoTimeout(1000);

                    byte[] reqData = "TETRIS_DISCOVER".getBytes();
                    DatagramPacket reqPacket = new DatagramPacket(reqData, reqData.length,
                            InetAddress.getByName("255.255.255.255"), 28081);
                    udpSocket.send(reqPacket);

                    byte[] buf = new byte[256];
                    while (true) {
                        try {
                            DatagramPacket resp = new DatagramPacket(buf, buf.length);
                            udpSocket.receive(resp);
                            String data = new String(resp.getData(), 0, resp.getLength());
                            if (data.startsWith("TETRIS_HOST:")) {
                                String[] parts = data.split(":");
                                String hostName = parts[1];
                                String ip = resp.getAddress().getHostAddress();

                                Platform.runLater(() -> {
                                    Button hostBtn = new Button("Join " + hostName + " (" + ip + ")");
                                    hostBtn.setStyle(
                                            "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
                                    hostBtn.setOnAction(ev -> ipField.setText(ip));
                                    hostsList.getChildren().add(hostBtn);
                                });
                            }
                        } catch (Exception ex) {
                            break;
                        } // Timeout or error
                    }
                } catch (Exception ex) {
                }
                Platform.runLater(() -> status.setText("Scan complete."));
            }).start();
        });

        Button connectBtn = new Button("Connect to Host");
        styleButton(connectBtn);
        connectBtn.setOnAction(e -> {
            connectBtn.setDisable(true);
            status.setText("Connecting...");

            TetrisPanel panel = new TetrisPanel(new GameLogic("P1", "P2", optSpeedLevel), () -> {
                closeNetwork();
                show();
            });

            TetrisClient client = new TetrisClient(msg -> {
                if (msg instanceof TetrisMessage) {
                    TetrisMessage tm = (TetrisMessage) msg;
                    if (tm.type == TetrisMessage.Type.STATE_UPDATE) {
                        Platform.runLater(() -> panel.updateState(tm.p1, tm.p2));
                    }
                }
            }, () -> Platform.runLater(() -> panel.showDisconnectOverlay(() -> {
                closeNetwork();
                show();
            })));
            activeClient = client;

            new Thread(() -> {
                try {
                    client.connect(ipField.getText(), 28080);
                    // Send chosen name immediately so host can update P2's display name
                    TetrisMessage nameMsg = new TetrisMessage(TetrisMessage.Type.PLAYER_NAME);
                    nameMsg.playerName = p2Field.getText();
                    client.send(nameMsg);
                    Platform.runLater(() -> {
                        panel.setNetworkMode(true, type -> {
                            client.send(new TetrisMessage(type));
                        });
                        panel.setupKeyEvents();
                        stage.setScene(new Scene(panel, 1100, 750));
                        stage.centerOnScreen();
                        panel.start();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        status.setText("Failed to connect: " + ex.getMessage());
                        connectBtn.setDisable(false);
                    });
                }
            }).start();
        });

        Button backBtn = new Button("Back");
        styleButton(backBtn);
        backBtn.setOnAction(e -> show());

        root.getChildren().addAll(title, p2Label, p2Field, scanBtn, hostsList, ipLabel, ipField, connectBtn, status,
                backBtn);
        stage.setScene(new Scene(root, 900, 650));
    }
}
