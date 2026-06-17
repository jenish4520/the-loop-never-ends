package seda_project.control_alt_defeat.tetris;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;
import javafx.scene.layout.HBox;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Insets;

public class CustomPieceDesigner {

    public static List<Tetromino> customPieces = new ArrayList<>();

    public static void loadPieces() {
        customPieces.clear();
        try {
            File f = new File("custom_pieces.txt");
            if (!f.exists())
                return;
            Scanner sc = new Scanner(f);
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty())
                    continue;
                String[] parts = line.split(":");
                String hex = parts[0];
                String bits = parts[1];
                int[][][] shapes = new int[4][5][5];
                int idx = 0;
                for (int r = 0; r < 5; r++) {
                    for (int c = 0; c < 5; c++) {
                        shapes[0][r][c] = bits.charAt(idx++) == '1' ? 1 : 0;
                    }
                }
                for (int i = 1; i < 4; i++) {
                    for (int r = 0; r < 5; r++) {
                        for (int c = 0; c < 5; c++) {
                            shapes[i][c][4 - r] = shapes[i - 1][r][c];
                        }
                    }
                }
                customPieces.add(new Tetromino(Tetromino.Type.CUSTOM, hex, shapes));
            }
            sc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void savePieces() {
        try {
            PrintWriter pw = new PrintWriter("custom_pieces.txt");
            for (Tetromino t : customPieces) {
                pw.print(t.colorHex + ":");
                int[][] s = t.getShape();
                for (int r = 0; r < 5; r++) {
                    for (int c = 0; c < 5; c++) {
                        pw.print(s[r][c]);
                    }
                }
                pw.println();
            }
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void show(Stage stage, Runnable onBack) {
        loadPieces();

        HBox mainLayout = new HBox(40);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-background-color: #0f0f1e;");

        // Left side: list of pieces
        VBox leftBox = new VBox(10);
        leftBox.setAlignment(Pos.TOP_CENTER);
        leftBox.setPadding(new Insets(40));

        Label listTitle = new Label("Saved Pieces");
        listTitle.setFont(Font.font("SansSerif", 24));
        listTitle.setTextFill(Color.WHITE);
        leftBox.getChildren().add(listTitle);

        VBox piecesList = new VBox(5);
        Runnable refreshList = () -> {
            piecesList.getChildren().clear();
            for (int i = 0; i < customPieces.size(); i++) {
                Tetromino t = customPieces.get(i);
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER);

                Canvas c = new Canvas(50, 50);
                GraphicsContext gc = c.getGraphicsContext2D();
                gc.setFill(Color.web(t.colorHex));
                int[][] s = t.getShape();
                for (int r = 0; r < 5; r++) {
                    for (int col = 0; col < 5; col++) {
                        if (s[r][col] == 1)
                            gc.fillRect(col * 10, r * 10, 10, 10);
                    }
                }

                Button delBtn = new Button("X");
                delBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                int finalI = i;
                delBtn.setOnAction(e -> {
                    customPieces.remove(finalI);
                    savePieces();
                    // trigger refresh
                    piecesList.getChildren().remove(row);
                });

                row.getChildren().addAll(c, delBtn);
                piecesList.getChildren().add(row);
            }
        };
        refreshList.run();
        leftBox.getChildren().add(piecesList);

        // Right side: designer
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);

        Label title = new Label("DESIGN CUSTOM PIECE");
        title.setFont(Font.font("SansSerif", 32));
        title.setTextFill(Color.WHITE);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(2);
        grid.setVgap(2);

        boolean[][] state = new boolean[5][5];
        Button[][] buttons = new Button[5][5];

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                Button btn = new Button();
                btn.setPrefSize(40, 40);
                btn.setStyle("-fx-background-color: #202020; -fx-border-color: #404040;");
                int finalR = r;
                int finalC = c;
                btn.setOnAction(e -> {
                    state[finalR][finalC] = !state[finalR][finalC];
                    btn.setStyle(state[finalR][finalC] ? "-fx-background-color: #ffffff; -fx-border-color: #404040;"
                            : "-fx-background-color: #202020; -fx-border-color: #404040;");
                });
                buttons[r][c] = btn;
                grid.add(btn, c, r);
            }
        }

        Label msg = new Label();
        msg.setTextFill(Color.RED);

        Button saveBtn = new Button("Save Piece");
        saveBtn.setStyle("-fx-background-color: #a882ff; -fx-text-fill: white; -fx-font-size: 16px;");
        saveBtn.setOnAction(e -> {
            if (!validateConnected(state)) {
                msg.setText("Piece must be fully connected and not empty!");
                return;
            }
            int[][][] shapes = new int[4][5][5];
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 5; c++) {
                    shapes[0][r][c] = state[r][c] ? 1 : 0;
                }
            }
            // generate rotations
            for (int i = 1; i < 4; i++) {
                for (int r = 0; r < 5; r++) {
                    for (int c = 0; c < 5; c++) {
                        shapes[i][c][4 - r] = shapes[i - 1][r][c];
                    }
                }
            }
            // Add custom piece
            String hex = String.format("#%06x", (int) (Math.random() * 0xFFFFFF));
            Tetromino custom = new Tetromino(Tetromino.Type.CUSTOM, hex, shapes);
            customPieces.add(custom);
            savePieces();
            refreshList.run();
            msg.setText("Saved!");
            msg.setTextFill(Color.GREEN);

            // clear grid
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 5; c++) {
                    state[r][c] = false;
                    buttons[r][c].setStyle("-fx-background-color: #202020; -fx-border-color: #404040;");
                }
            }
        });

        Button cancelBtn = new Button("Return");
        cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 16px;");
        cancelBtn.setOnAction(e -> onBack.run());

        root.getChildren().addAll(title, grid, msg, saveBtn, cancelBtn);
        mainLayout.getChildren().addAll(leftBox, root);

        stage.setScene(new Scene(mainLayout, 900, 650));
    }

    private static boolean validateConnected(boolean[][] state) {
        int count = 0;
        int startR = -1, startC = -1;
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                if (state[r][c]) {
                    count++;
                    startR = r;
                    startC = c;
                }
            }
        }
        if (count == 0)
            return false;

        boolean[][] visited = new boolean[5][5];
        Queue<int[]> q = new LinkedList<>();
        q.add(new int[] { startR, startC });
        visited[startR][startC] = true;

        int connectedCount = 0;
        while (!q.isEmpty()) {
            int[] curr = q.poll();
            connectedCount++;
            int r = curr[0], c = curr[1];

            int[][] dirs = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
            for (int[] d : dirs) {
                int nr = r + d[0];
                int nc = c + d[1];
                if (nr >= 0 && nr < 5 && nc >= 0 && nc < 5 && state[nr][nc] && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    q.add(new int[] { nr, nc });
                }
            }
        }
        return connectedCount == count;
    }
}
