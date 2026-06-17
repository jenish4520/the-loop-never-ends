package seda_project.control_alt_defeat.tetris;

public class GameLogic {

    public PlayerState p1;
    public PlayerState p2;

    public boolean twoBlocksMode = false;
    public boolean horizontalMode = false;

    private int speedLevel;
    private int blocksGenerated = 0;

    public GameLogic(String name1, String name2) {
        this(name1, name2, 3);
    }

    public GameLogic(String name1, String name2, int speedLevel) {
        this.speedLevel = clampSpeed(speedLevel);

        p1 = new PlayerState(1, name1, this.speedLevel);
        p1.spawnNext();
        while (p1.nextPiece.isSpecial()) {
            p1.bag.add(p1.nextPiece);
            java.util.Collections.shuffle(p1.bag);
            p1.nextPiece = p1.pullFromBag();
        }

        p2 = new PlayerState(2, name2, this.speedLevel);
        p2.spawnNext();
        while (p2.nextPiece.isSpecial()) {
            p2.bag.add(p2.nextPiece);
            java.util.Collections.shuffle(p2.bag);
            p2.nextPiece = p2.pullFromBag();
        }
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public void initModes() {
        if (twoBlocksMode) {
            p1.nextPiece2 = p1.pullFromBag();
            while (p1.nextPiece2.isSpecial()) {
                p1.bag.add(p1.nextPiece2);
                java.util.Collections.shuffle(p1.bag);
                p1.nextPiece2 = p1.pullFromBag();
            }
            p2.nextPiece2 = p2.pullFromBag();
            while (p2.nextPiece2.isSpecial()) {
                p2.bag.add(p2.nextPiece2);
                java.util.Collections.shuffle(p2.bag);
                p2.nextPiece2 = p2.pullFromBag();
            }
            p1.spawnNext2();
            while (p1.nextPiece2.isSpecial()) {
                p1.bag.add(p1.nextPiece2);
                java.util.Collections.shuffle(p1.bag);
                p1.nextPiece2 = p1.pullFromBag();
            }
            p2.spawnNext2();
            while (p2.nextPiece2.isSpecial()) {
                p2.bag.add(p2.nextPiece2);
                java.util.Collections.shuffle(p2.bag);
                p2.nextPiece2 = p2.pullFromBag();
            }
        }
    }

    public void update(long currentTime) {
        updatePlayer(p1, currentTime);
        updatePlayer(p2, currentTime);
    }

    private void updatePlayer(PlayerState p, long currentTime) {
        if (p.isGameOver) return;
        updateFallingPiece(p, false, currentTime);
        if (twoBlocksMode) updateFallingPiece(p, true, currentTime);
    }

    private void updateFallingPiece(PlayerState p, boolean secondPiece, long currentTime) {
        Tetromino piece = getPiece(p, secondPiece);
        if (piece == null) return;

        long lastFall = secondPiece ? p.lastFallTime2 : p.lastFallTime;
        if (currentTime - lastFall > p.getFallDelay()) {
            Tetromino moved = piece.copy();
            moved.y++;
            if (isValidWithPeer(p, moved, secondPiece)) {
                setPiece(p, secondPiece, moved);
                setLastFallTime(p, secondPiece, currentTime);
                checkActivePowerups(p, secondPiece);
                tryPortal(p, secondPiece);
            } else if (getLockStartTime(p, secondPiece) == 0) {
                if (!p.board.isValid(moved)) {
                    setLockStartTime(p, secondPiece, currentTime);
                }
            }
        }

        piece = getPiece(p, secondPiece);
        if (piece == null) return;

        long lockStart = getLockStartTime(p, secondPiece);
        int lockResets = getLockResets(p, secondPiece);
        if (lockStart > 0 && (currentTime - lockStart > 500 || lockResets >= 15)) {
            Tetromino moved = piece.copy();
            moved.y++;
            if (!isValidWithPeer(p, moved, secondPiece)) {
                if (!p.board.isValid(moved)) {
                    if (secondPiece) lockPiece2(p);
                    else lockPiece(p);
                } else {
                    setLockStartTime(p, secondPiece, 0);
                }
            } else {
                setLockStartTime(p, secondPiece, 0);
            }
        }
    }

    // Checks if the piece is valid on the board and does not overlap the other active piece.
    private boolean isValidWithPeer(PlayerState p, Tetromino t, boolean isSecondPiece) {
        if (!p.board.isValid(t)) return false;
        if (!twoBlocksMode) return true;
        // Choose the OTHER active piece as the peer
        Tetromino peer = isSecondPiece ? p.activePiece : p.activePiece2;
        if (peer == null) return true;
        int[][] shape1 = t.getShape();
        int[][] shape2 = peer.getShape();
        for (int r1 = 0; r1 < shape1.length; r1++) {
            for (int c1 = 0; c1 < shape1[r1].length; c1++) {
                if (shape1[r1][c1] != 0) {
                    for (int r2 = 0; r2 < shape2.length; r2++) {
                        for (int c2 = 0; c2 < shape2[r2].length; c2++) {
                            if (shape2[r2][c2] != 0
                                    && t.x + c1 == peer.x + c2
                                    && t.y + r1 == peer.y + r2) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private void spawnSwapPowerup() {
        PlayerState target = Math.random() < 0.5 ? p1 : p2;
        int[] cell = randomEmptyCell(target.board);
        if (cell == null) return;
        target.board.hasSwapPowerup = true;
        target.board.swapX = cell[0];
        target.board.swapY = cell[1];
    }

    private void spawnPowerup(String type) {
        PlayerState target = Math.random() < 0.5 ? p1 : p2;
        int[] cell = randomEmptyCell(target.board);
        if (cell == null) return;

        switch (type) {
            case "speedUp":
                target.board.hasSpeedUpPowerup = true;
                target.board.speedUpX = cell[0];
                target.board.speedUpY = cell[1];
                break;
            case "slowSelf":
                target.board.hasSlowSelfPowerup = true;
                target.board.slowSelfX = cell[0];
                target.board.slowSelfY = cell[1];
                break;
            case "delayOpponentRot":
                target.board.hasDelayOpponentRotPowerup = true;
                target.board.delayOpponentRotX = cell[0];
                target.board.delayOpponentRotY = cell[1];
                break;
            case "delaySelfRot":
                target.board.hasDelaySelfRotPowerup = true;
                target.board.delaySelfRotX = cell[0];
                target.board.delaySelfRotY = cell[1];
                break;
            case "slowOpponent":
                target.board.hasSlowOpponentPowerup = true;
                target.board.slowOpponentX = cell[0];
                target.board.slowOpponentY = cell[1];
                break;
        }
    }

    private void spawnPortal() {
        PlayerState target = Math.random() < 0.5 ? p1 : p2;
        int[] cell = randomEmptyCell(target.board);
        if (cell == null) return;
        target.board.hasPortal = true;
        target.board.portalX = cell[0];
        target.board.portalY = cell[1];
    }

    private void spawnRandomPowerup() {
        String[] types = { "swap", "speedUp", "slowSelf", "delayOpponentRot", "delaySelfRot", "slowOpponent", "portal" };
        String chosen = types[(int)(Math.random() * types.length)];
        if (chosen.equals("swap")) {
            spawnSwapPowerup();
        } else if (chosen.equals("portal")) {
            spawnPortal();
        } else {
            spawnPowerup(chosen);
        }
    }

    private int[] randomEmptyCell(Board board) {
        int emptyCount = 0;
        for (int y = board.getTopRow(); y < Board.HEIGHT; y++) {
            for (int x = 0; x < Board.WIDTH; x++) {
                if (board.grid[y][x] == null && !hasItemAt(board, x, y)) emptyCount++;
            }
        }
        if (emptyCount == 0) return null;

        int chosen = (int)(Math.random() * emptyCount);
        int c = 0;
        for (int y = board.getTopRow(); y < Board.HEIGHT; y++) {
            for (int x = 0; x < Board.WIDTH; x++) {
                if (board.grid[y][x] == null && !hasItemAt(board, x, y)) {
                    if (c == chosen) return new int[]{x, y};
                    c++;
                }
            }
        }
        return null;
    }

    private boolean hasItemAt(Board board, int x, int y) {
        return (board.hasSwapPowerup && board.swapX == x && board.swapY == y)
                || (board.hasSpeedUpPowerup && board.speedUpX == x && board.speedUpY == y)
                || (board.hasSlowSelfPowerup && board.slowSelfX == x && board.slowSelfY == y)
                || (board.hasDelayOpponentRotPowerup && board.delayOpponentRotX == x && board.delayOpponentRotY == y)
                || (board.hasDelaySelfRotPowerup && board.delaySelfRotX == x && board.delaySelfRotY == y)
                || (board.hasSlowOpponentPowerup && board.slowOpponentX == x && board.slowOpponentY == y)
                || (board.hasPortal && board.portalX == x && board.portalY == y);
    }

    private boolean pieceHits(Tetromino piece, int px, int py) {
        if (piece == null) return false;
        int[][] shape = piece.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0 && piece.x + c == px && piece.y + r == py) {
                    return true;
                }
            }
        }
        return false;
    }

    public void lockPiece(PlayerState p) {
        finishPiece(p, false);
    }

    private void lockPiece2(PlayerState p) {
        finishPiece(p, true);
    }

    private void finishPiece(PlayerState p, boolean secondPiece) {
        Tetromino piece = getPiece(p, secondPiece);
        if (piece == null) return;
        if (tryPortal(p, secondPiece)) return;

        if (piece.isSpecial()) {
            explodePiece(p, piece);
            setPiece(p, secondPiece, null);
            spawnPiece(p, secondPiece);
            return;
        }

        p.board.lock(piece);
        applyPowerups(p, piece);
        setPiece(p, secondPiece, null);
        clearAndScore(p, !secondPiece);
        spawnPiece(p, secondPiece);
    }

    private void explodePiece(PlayerState p, Tetromino piece) {
        int[] cell = impactCell(piece);
        if (piece.type == Tetromino.Type.RADIUS_BOMB) {
            p.board.clearRadius(cell[0], cell[1], 3);
        } else if (piece.type == Tetromino.Type.COLUMN_BOMB) {
            p.board.clearBelow(cell[0], cell[1]);
        }
    }

    private int[] impactCell(Tetromino piece) {
        int[][] shape = piece.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) return new int[]{piece.x + c, piece.y + r};
            }
        }
        return new int[]{piece.x, piece.y};
    }

    private void applyPowerups(PlayerState p, Tetromino piece) {
        if (p.board.hasSwapPowerup && pieceHits(piece, p.board.swapX, p.board.swapY)) {
            p.board.hasSwapPowerup = false;
            p1.board.swapFlashEndTime = System.currentTimeMillis() + 1000;
            p2.board.swapFlashEndTime = System.currentTimeMillis() + 1000;
            String[][] tempGrid = p1.board.grid;
            p1.board.grid = p2.board.grid;
            p2.board.grid = tempGrid;
        }

        if (p.board.hasSpeedUpPowerup && pieceHits(piece, p.board.speedUpX, p.board.speedUpY)) {
            opponentOf(p).speedUpEndTime = System.currentTimeMillis() + 10000;
            opponentOf(p).board.speedUpFlashEndTime = System.currentTimeMillis() + 1000;
            p.board.hasSpeedUpPowerup = false;
        }

        if (p.board.hasSlowSelfPowerup && pieceHits(piece, p.board.slowSelfX, p.board.slowSelfY)) {
            p.slowDownEndTime = System.currentTimeMillis() + 10000;
            p.board.slowSelfFlashEndTime = System.currentTimeMillis() + 1000;
            p.board.hasSlowSelfPowerup = false;
        }

        if (p.board.hasDelayOpponentRotPowerup && pieceHits(piece, p.board.delayOpponentRotX, p.board.delayOpponentRotY)) {
            opponentOf(p).rotationDelayEndTime = System.currentTimeMillis() + 10000;
            opponentOf(p).board.delayOpponentRotFlashEndTime = System.currentTimeMillis() + 1000;
            p.board.hasDelayOpponentRotPowerup = false;
        }

        if (p.board.hasDelaySelfRotPowerup && pieceHits(piece, p.board.delaySelfRotX, p.board.delaySelfRotY)) {
            p.rotationDelayEndTime = System.currentTimeMillis() + 10000;
            p.board.delaySelfRotFlashEndTime = System.currentTimeMillis() + 1000;
            p.board.hasDelaySelfRotPowerup = false;
        }

        if (p.board.hasSlowOpponentPowerup && pieceHits(piece, p.board.slowOpponentX, p.board.slowOpponentY)) {
            opponentOf(p).slowDownEndTime = System.currentTimeMillis() + 10000;
            opponentOf(p).board.slowOpponentFlashEndTime = System.currentTimeMillis() + 1000;
            p.board.hasSlowOpponentPowerup = false;
        }
    }

    private void checkActivePowerups(PlayerState p, boolean secondPiece) {
        Tetromino piece = getPiece(p, secondPiece);
        if (piece == null) return;
        applyPowerups(p, piece);
    }

    private void clearAndScore(PlayerState p, boolean backToBackAllowed) {
        int[] fullRows = p.board.getFullRows();
        if (fullRows.length == 0) return;

        p.board.clearRows(fullRows, p.id == 2);
        
        if (p.activePiece != null) {
            int shift = 0;
            for (int r : fullRows) {
                if (r > p.activePiece.y) shift++;
            }
            p.activePiece.y += shift;
        }
        if (twoBlocksMode && p.activePiece2 != null) {
            int shift = 0;
            for (int r : fullRows) {
                if (r > p.activePiece2.y) shift++;
            }
            p.activePiece2.y += shift;
        }

        int lines = fullRows.length;
        applyLinePressure(p, lines);

        p.linesCleared += lines;
        int baseScore = 0;
        switch (lines) {
            case 1: baseScore = 100; break;
            case 2: baseScore = 300; break;
            case 3: baseScore = 500; break;
            case 4: baseScore = 800; break;
        }

        if (backToBackAllowed) {
            if (lines == 4) {
                if (p.backToBack) baseScore = 1200;
                p.backToBack = true;
            } else {
                p.backToBack = false;
            }
        }

        p.score += baseScore * (1 + p.linesCleared / 10);
    }

    private void applyLinePressure(PlayerState p, int lines) {
        PlayerState opp = opponentOf(p);
        p.board.grow(lines);
        opp.board.shrink(lines);
        fitActivePieces(p);
        fitActivePieces(opp);
    }

    private void fitActivePieces(PlayerState p) {
        fitPiece(p, false);
        if (twoBlocksMode) fitPiece(p, true);
    }

    private void fitPiece(PlayerState p, boolean secondPiece) {
        Tetromino piece = getPiece(p, secondPiece);
        if (piece == null) return;
        while (piece.y < p.board.getTopRow()) piece.y++;
        if (!p.board.isValid(piece)) p.isGameOver = true;
    }

    private boolean tryPortal(PlayerState p, boolean secondPiece) {
        Tetromino piece = getPiece(p, secondPiece);
        if (piece == null || !p.board.hasPortal) return false;
        if (!pieceHits(piece, p.board.portalX, p.board.portalY)) return false;

        PlayerState opp = opponentOf(p);
        Tetromino forwarded = piece.queuedCopy();
        if (twoBlocksMode && secondPiece && opp.nextPiece2 != null) {
            opp.nextPiece2 = forwarded;
        } else {
            opp.nextPiece = forwarded;
        }

        p.board.hasPortal = false;
        p.board.portalFlashEndTime = System.currentTimeMillis() + 1000;
        setPiece(p, secondPiece, null);
        spawnPiece(p, secondPiece);
        return true;
    }

    public void moveLeft(PlayerState p) {
        if (p.isGameOver) return;
        int dx = (p.id == 1) ? -1 : 1;
        movePiece(p, false, dx);
        if (twoBlocksMode) movePiece(p, true, dx);
    }

    public void moveRight(PlayerState p) {
        if (p.isGameOver) return;
        int dx = (p.id == 1) ? 1 : -1;
        movePiece(p, false, dx);
        if (twoBlocksMode) movePiece(p, true, dx);
    }

    // ---- Per-piece public methods for two-blocks independent control ----

    public void moveLeftPiece1(PlayerState p) {
        if (p.isGameOver) return;
        movePiece(p, false, (p.id == 1) ? -1 : 1);
    }

    public void moveRightPiece1(PlayerState p) {
        if (p.isGameOver) return;
        movePiece(p, false, (p.id == 1) ? 1 : -1);
    }

    public void moveLeftPiece2(PlayerState p) {
        if (p.isGameOver) return;
        movePiece(p, true, (p.id == 1) ? -1 : 1);
    }

    public void moveRightPiece2(PlayerState p) {
        if (p.isGameOver) return;
        movePiece(p, true, (p.id == 1) ? 1 : -1);
    }

    public void rotateCWPiece1(PlayerState p) {
        rotatePiece(p, false, true);
    }

    public void rotateCWPiece2(PlayerState p) {
        rotatePiece(p, true, true);
    }

    public void hardDropPiece1(PlayerState p) {
        if (p.isGameOver) return;
        if (hardDropPiece(p, false)) {
            Tetromino moved = p.activePiece.copy();
            moved.y++;
            if (!p.board.isValid(moved)) {
                lockPiece(p);
            }
        }
    }

    public void hardDropPiece2(PlayerState p) {
        if (p.isGameOver) return;
        if (p.activePiece2 != null && hardDropPiece(p, true)) {
            Tetromino moved = p.activePiece2.copy();
            moved.y++;
            if (!p.board.isValid(moved)) {
                lockPiece2(p);
            }
        }
    }

    public void softDropPiece1(PlayerState p) {
        if (p.isGameOver) return;
        softDropPiece(p, false);
    }

    public void softDropPiece2(PlayerState p) {
        if (p.isGameOver) return;
        softDropPiece(p, true);
    }

    // ---- End per-piece methods ----

    private void movePiece(PlayerState p, boolean secondPiece, int dx) {
        Tetromino piece = getPiece(p, secondPiece);
        if (piece == null) return;
        Tetromino moved = piece.copy();
        moved.x += dx;
        if (isValidWithPeer(p, moved, secondPiece)) {
            setPiece(p, secondPiece, moved);
            resetLock(p, secondPiece);
            checkActivePowerups(p, secondPiece);
            tryPortal(p, secondPiece);
        }
    }

    public void softDrop(PlayerState p) {
        if (p.isGameOver) return;
        softDropPiece(p, false);
        if (twoBlocksMode) softDropPiece(p, true);
    }

    private void softDropPiece(PlayerState p, boolean secondPiece) {
        Tetromino piece = getPiece(p, secondPiece);
        if (piece == null) return;

        Tetromino moved = piece.copy();
        moved.y++;
        if (isValidWithPeer(p, moved, secondPiece)) {
            setPiece(p, secondPiece, moved);
            setLastFallTime(p, secondPiece, System.currentTimeMillis());
            checkActivePowerups(p, secondPiece);
            tryPortal(p, secondPiece);
        }
    }

    public void hardDrop(PlayerState p) {
        if (p.isGameOver) return;
        if (hardDropPiece(p, false)) {
            Tetromino moved = p.activePiece.copy();
            moved.y++;
            if (!p.board.isValid(moved)) {
                lockPiece(p);
            }
        }
        if (twoBlocksMode && p.activePiece2 != null && hardDropPiece(p, true)) {
            Tetromino moved = p.activePiece2.copy();
            moved.y++;
            if (!p.board.isValid(moved)) {
                lockPiece2(p);
            }
        }
    }

    private boolean hardDropPiece(PlayerState p, boolean secondPiece) {
        Tetromino piece = getPiece(p, secondPiece);
        if (piece == null) return false;

        int dropped = 0;
        while (true) {
            Tetromino moved = piece.copy();
            moved.y++;
            if (!isValidWithPeer(p, moved, secondPiece)) break;
            setPiece(p, secondPiece, moved);
            piece = moved;
            dropped++;
            checkActivePowerups(p, secondPiece);
            if (tryPortal(p, secondPiece)) return false;
        }

        return true;
    }

    public void rotateCW(PlayerState p) {
        rotatePiece(p, false, true);
        if (twoBlocksMode) rotatePiece(p, true, true);
    }

    public void rotateCCW(PlayerState p) {
        rotatePiece(p, false, false);
        if (twoBlocksMode) rotatePiece(p, true, false);
    }

    private void rotatePiece(PlayerState p, boolean secondPiece, boolean clockwise) {
        if (p.isGameOver || System.currentTimeMillis() < p.rotationDelayEndTime) return;
        Tetromino piece = getPiece(p, secondPiece);
        if (piece == null || piece.isSpecial()) return;

        Tetromino rotated = piece.copy();
        int fromState = rotated.state;
        if (clockwise) rotated.rotateCW();
        else rotated.rotateCCW();
        tryKicks(p, rotated, fromState, rotated.state, clockwise, secondPiece);
    }

    private void tryKicks(PlayerState p, Tetromino t, int fromState, int toState, boolean isCW, boolean secondPiece) {
        int[][] kicks = WallKicks.getKicks(t.type, fromState, toState, isCW);
        for (int[] kick : kicks) {
            Tetromino test = t.copy();
            test.x += (p.id == 1) ? kick[0] : -kick[0];
            test.y += (p.id == 1) ? -kick[1] : kick[1];
            if (isValidWithPeer(p, test, secondPiece)) {
                setPiece(p, secondPiece, test);
                resetLock(p, secondPiece);
                checkActivePowerups(p, secondPiece);
                tryPortal(p, secondPiece);
                return;
            }
        }
    }

    private void resetLock(PlayerState p, boolean secondPiece) {
        if (secondPiece) {
            if (p.lockStartTime2 > 0 && p.lockResets2 < 15) {
                p.lockStartTime2 = System.currentTimeMillis();
                p.lockResets2++;
            }
        } else if (p.lockStartTime > 0 && p.lockResets < 15) {
            p.lockStartTime = System.currentTimeMillis();
            p.lockResets++;
        }
    }

    public Tetromino getGhost(PlayerState p) {
        if (p.activePiece == null || p.activePiece.isSpecial()) return null;
        return ghostFor(p, p.activePiece, false);
    }

    public Tetromino getGhost2(PlayerState p) {
        if (p.activePiece2 == null || p.activePiece2.isSpecial()) return null;
        return ghostFor(p, p.activePiece2, true);
    }

    private Tetromino ghostFor(PlayerState p, Tetromino piece, boolean isSecondPiece) {
        Tetromino ghost = piece.copy();
        while (true) {
            ghost.y++;
            if (!isValidWithPeer(p, ghost, isSecondPiece)) {
                ghost.y--;
                break;
            }
        }
        return ghost;
    }

    private Tetromino getPiece(PlayerState p, boolean secondPiece) {
        return secondPiece ? p.activePiece2 : p.activePiece;
    }

    private void setPiece(PlayerState p, boolean secondPiece, Tetromino piece) {
        if (secondPiece) p.activePiece2 = piece;
        else p.activePiece = piece;
    }

    private void spawnPiece(PlayerState p, boolean secondPiece) {
        if (secondPiece) p.spawnNext2();
        else p.spawnNext();

        blocksGenerated++;
        if (blocksGenerated % 4 == 0) {
            spawnRandomPowerup();
        }
    }

    private void setLastFallTime(PlayerState p, boolean secondPiece, long time) {
        if (secondPiece) p.lastFallTime2 = time;
        else p.lastFallTime = time;
    }

    private long getLockStartTime(PlayerState p, boolean secondPiece) {
        return secondPiece ? p.lockStartTime2 : p.lockStartTime;
    }

    private void setLockStartTime(PlayerState p, boolean secondPiece, long time) {
        if (secondPiece) p.lockStartTime2 = time;
        else p.lockStartTime = time;
    }

    private int getLockResets(PlayerState p, boolean secondPiece) {
        return secondPiece ? p.lockResets2 : p.lockResets;
    }

    private PlayerState opponentOf(PlayerState p) {
        return p == p1 ? p2 : p1;
    }

    private int clampSpeed(int speed) {
        if (speed < 1) return 1;
        if (speed > 6) return 6;
        return speed;
    }

    private long randomDelay(int base, int range) {
        return base + (long)(Math.random() * range);
    }
}
