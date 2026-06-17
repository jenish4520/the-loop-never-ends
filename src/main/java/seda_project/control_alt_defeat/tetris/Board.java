package seda_project.control_alt_defeat.tetris;

import java.io.Serializable;
import java.util.Arrays;

public class Board implements Serializable {
    public static final int WIDTH = 10;
    public static final int HEIGHT = 24;
    public static final int DEFAULT_VISIBLE_ROWS = 20;
    public static final int MIN_VISIBLE_ROWS = 12;

    public String[][] grid = new String[HEIGHT][WIDTH];
    public int visibleRows = DEFAULT_VISIBLE_ROWS;

    public int linesClearedTotal = 0;
    public int level = 1;
    public int score = 0;

    public boolean[] flashedRows = new boolean[HEIGHT];
    public boolean needsFlash = false;

    public boolean hasSwapPowerup = false;
    public int swapX = -1, swapY = -1;
    public long swapFlashEndTime = 0;

    public boolean hasSpeedUpPowerup = false;
    public int speedUpX = -1, speedUpY = -1;

    public boolean hasSlowSelfPowerup = false;
    public int slowSelfX = -1, slowSelfY = -1;

    public boolean hasDelayOpponentRotPowerup = false;
    public int delayOpponentRotX = -1, delayOpponentRotY = -1;

    public boolean hasDelaySelfRotPowerup = false;
    public int delaySelfRotX = -1, delaySelfRotY = -1;

    public boolean hasSlowOpponentPowerup = false;
    public int slowOpponentX = -1, slowOpponentY = -1;

    public boolean hasPortal = false;
    public int portalX = -1, portalY = -1;
    public long portalFlashEndTime = 0;

    public long speedUpFlashEndTime = 0;
    public long slowSelfFlashEndTime = 0;
    public long delayOpponentRotFlashEndTime = 0;
    public long delaySelfRotFlashEndTime = 0;
    public long slowOpponentFlashEndTime = 0;

    public Board() {
        for (int y = 0; y < HEIGHT; y++) {
            Arrays.fill(grid[y], null);
        }
    }

    public int getTopRow() {
        return HEIGHT - visibleRows;
    }

    public boolean isValid(Tetromino t) {
        int[][] shape = t.getShape();
        int topRow = getTopRow();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int boardX = t.x + c;
                    int boardY = t.y + r;
                    if (boardX < 0 || boardX >= WIDTH || boardY < topRow || boardY >= HEIGHT) {
                        return false;
                    }
                    if (grid[boardY][boardX] != null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void lock(Tetromino t) {
        int[][] shape = t.getShape();
        int topRow = getTopRow();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int boardX = t.x + c;
                    int boardY = t.y + r;
                    if (boardY >= topRow && boardY < HEIGHT && boardX >= 0 && boardX < WIDTH) {
                        grid[boardY][boardX] = t.colorHex;
                    }
                }
            }
        }
    }

    public int[] getFullRows() {
        int[] fullRows = new int[HEIGHT];
        int count = 0;
        for (int y = getTopRow(); y < HEIGHT; y++) {
            boolean full = true;
            for (int x = 0; x < WIDTH; x++) {
                if (grid[y][x] == null) {
                    full = false;
                    break;
                }
            }
            if (full) {
                fullRows[count++] = y;
            }
        }
        return Arrays.copyOf(fullRows, count);
    }

    public void clearRows(int[] rows, boolean isP2) {
        if (rows.length == 0) return;

        boolean[] cleared = new boolean[HEIGHT];
        for (int row : rows) {
            if (row >= 0 && row < HEIGHT) {
                cleared[row] = true;
            }
        }

        String[][] newGrid = new String[HEIGHT][WIDTH];
        int writeY = HEIGHT - 1;
        for (int y = HEIGHT - 1; y >= getTopRow(); y--) {
            if (!cleared[y]) {
                System.arraycopy(grid[y], 0, newGrid[writeY--], 0, WIDTH);
            }
        }
        grid = newGrid;
        clearHiddenItems();
    }

    public void grow(int lines) {
        visibleRows = Math.min(HEIGHT, visibleRows + lines);
    }

    public void shrink(int lines) {
        visibleRows = Math.max(MIN_VISIBLE_ROWS, visibleRows - lines);
        clearHiddenItems();
    }

    public void clearRadius(int centerX, int centerY, int radius) {
        int topRow = getTopRow();
        int radiusSquared = radius * radius;
        for (int y = topRow; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int dx = x - centerX;
                int dy = y - centerY;
                if (dx * dx + dy * dy <= radiusSquared) {
                    grid[y][x] = null;
                }
            }
        }
    }

    public void clearBelow(int x, int y) {
        int topRow = getTopRow();
        for (int row = y; row < HEIGHT; row++) {
            if (row >= topRow && row < HEIGHT && x >= 0 && x < WIDTH) {
                grid[row][x] = null;
            }
        }
    }

    private void clearHiddenItems() {
        int topRow = getTopRow();
        for (int y = 0; y < topRow; y++) {
            Arrays.fill(grid[y], null);
        }
        if (hasSwapPowerup && swapY < topRow) hasSwapPowerup = false;
        if (hasSpeedUpPowerup && speedUpY < topRow) hasSpeedUpPowerup = false;
        if (hasSlowSelfPowerup && slowSelfY < topRow) hasSlowSelfPowerup = false;
        if (hasDelayOpponentRotPowerup && delayOpponentRotY < topRow) hasDelayOpponentRotPowerup = false;
        if (hasDelaySelfRotPowerup && delaySelfRotY < topRow) hasDelaySelfRotPowerup = false;
        if (hasSlowOpponentPowerup && slowOpponentY < topRow) hasSlowOpponentPowerup = false;
        if (hasPortal && portalY < topRow) hasPortal = false;
    }
}
