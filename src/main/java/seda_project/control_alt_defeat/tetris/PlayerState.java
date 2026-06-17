package seda_project.control_alt_defeat.tetris;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerState implements Serializable {
    public int id; // 1 or 2
    public String name;
    public Board board;
    public Tetromino activePiece;
    public Tetromino nextPiece;
    public List<Tetromino> bag = new ArrayList<>();

    // Second active piece state
    public Tetromino activePiece2 = null;
    public Tetromino nextPiece2 = null;
    public long lastFallTime2 = 0;
    public int lockResets2 = 0;
    public long lockStartTime2 = 0;

    public int linesCleared = 0;
    public int score = 0;
    public boolean backToBack = false;

    public long slowDownEndTime = 0;
    public long speedUpEndTime = 0;
    public long rotationDelayEndTime = 0;

    public boolean isGameOver = false;
    public int speedLevel = 3;

    public long lastFallTime = 0;
    public int lockResets = 0;
    public long lockStartTime = 0;

    public PlayerState(int id, String name) {
        this(id, name, 3);
    }

    public PlayerState(int id, String name, int speedLevel) {
        this.id = id;
        this.name = name;
        this.speedLevel = speedLevel;
        this.board = new Board();
        refillBag();
        nextPiece = pullFromBag();
        while (nextPiece.isSpecial()) {
            bag.add(nextPiece);
            Collections.shuffle(bag);
            nextPiece = pullFromBag();
        }
    }

    public void refillBag() {
        bag.add(Tetromino.create(Tetromino.Type.I));
        bag.add(Tetromino.create(Tetromino.Type.J));
        bag.add(Tetromino.create(Tetromino.Type.L));
        bag.add(Tetromino.create(Tetromino.Type.O));
        bag.add(Tetromino.create(Tetromino.Type.S));
        bag.add(Tetromino.create(Tetromino.Type.T));
        bag.add(Tetromino.create(Tetromino.Type.Z));
        bag.add(Tetromino.create(Tetromino.Type.RADIUS_BOMB));
        bag.add(Tetromino.create(Tetromino.Type.COLUMN_BOMB));
        for (Tetromino custom : CustomPieceDesigner.customPieces) {
            bag.add(custom.copy());
        }
        Collections.shuffle(bag);
    }

    public Tetromino pullFromBag() {
        if (bag.isEmpty()) {
            refillBag();
        }
        return bag.remove(0);
    }

    public void spawnNext() {
        activePiece = nextPiece.queuedCopy();
        nextPiece = pullFromBag();

        int pieceWidth = activePiece.getShape()[0].length;
        activePiece.x = Board.WIDTH / 2 - pieceWidth / 2;
        activePiece.y = board.getTopRow();

        if (!board.isValid(activePiece)) {
            isGameOver = true;
        }

        lockResets = 0;
        lockStartTime = 0;
    }

    // Spawn the second piece offsetting it to avoid overlap
    public void spawnNext2() {
        activePiece2 = nextPiece2.queuedCopy();
        nextPiece2 = pullFromBag();

        int pieceWidth = activePiece2.getShape()[0].length;
        activePiece2.x = Board.WIDTH / 4 - pieceWidth / 2;
        activePiece2.y = board.getTopRow();

        if (!board.isValid(activePiece2)) {
            activePiece2.x = Board.WIDTH * 3 / 4 - pieceWidth / 2;
            if (!board.isValid(activePiece2)) {
                isGameOver = true;
            }
        }

        lockResets2 = 0;
        lockStartTime2 = 0;
    }


    public int getFallDelay() {
        int delay = 1200 - (speedLevel * 150) - ((linesCleared / 2) * 50);
        if (delay < 60) delay = 60;

        // Apply slow down modifier
        if (System.currentTimeMillis() < slowDownEndTime) {
            delay = delay * 2;
        }

        // Apply speed up modifier
        if (System.currentTimeMillis() < speedUpEndTime) {
            delay = delay / 2;
        }

        return delay;
    }
}

