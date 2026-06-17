package seda_project.control_alt_defeat.chess;

public class ChessBoard {

    public enum PieceType {
        PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING
    }

    public enum PieceColor {
        WHITE, BLACK
    }



    private final Piece[][] board = new Piece[11][11];
    private PieceColor currentTurn = PieceColor.WHITE;

    private int lastFromQ = -999;
    private int lastFromR = -999;
    private int lastToQ = -999;
    private int lastToR = -999;

    private int halfMoveClock = 0;
    private final java.util.List<String> positionHistory = new java.util.ArrayList<>();

    public ChessBoard() {
        resetBoard();
    }

    public boolean isValidCoord(int q, int r) {
        return HexCoord.isValid(q, r);
    }

    public Piece getPiece(int q, int r) {
        if (!isValidCoord(q, r)) return null;
        return board[q + 5][r + 5];
    }

    public void setPiece(int q, int r, Piece p) {
        if (isValidCoord(q, r)) {
            board[q + 5][r + 5] = p;
        }
    }

    public PieceColor getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(PieceColor color) {
        this.currentTurn = color;
    }

    public int getHalfMoveClock() {
        return halfMoveClock;
    }

    public java.util.List<String> getPositionHistory() {
        return positionHistory;
    }

    public int getLastFromQ() { return lastFromQ; }
    public int getLastFromR() { return lastFromR; }
    public int getLastToQ() { return lastToQ; }
    public int getLastToR() { return lastToR; }

    public void setLastMove(int fq, int fr, int tq, int tr) {
        this.lastFromQ = fq;
        this.lastFromR = fr;
        this.lastToQ = tq;
        this.lastToR = tr;
    }

    public void resetBoard() {
        currentTurn = PieceColor.WHITE;
        lastFromQ = -999;
        lastFromR = -999;
        lastToQ = -999;
        lastToR = -999;
        halfMoveClock = 0;
        positionHistory.clear();

        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (isValidCoord(q, r)) {
                    board[q + 5][r + 5] = null;
                }
            }
        }

        // Setup White pieces
        setupPiece("c1", PieceType.ROOK, PieceColor.WHITE);
        setupPiece("i1", PieceType.ROOK, PieceColor.WHITE);
        setupPiece("d1", PieceType.KNIGHT, PieceColor.WHITE);
        setupPiece("h1", PieceType.KNIGHT, PieceColor.WHITE);
        setupPiece("e1", PieceType.QUEEN, PieceColor.WHITE);
        setupPiece("g1", PieceType.KING, PieceColor.WHITE);
        setupPiece("f1", PieceType.BISHOP, PieceColor.WHITE);
        setupPiece("f2", PieceType.BISHOP, PieceColor.WHITE);
        setupPiece("f3", PieceType.BISHOP, PieceColor.WHITE);

        setupPiece("b1", PieceType.PAWN, PieceColor.WHITE);
        setupPiece("c2", PieceType.PAWN, PieceColor.WHITE);
        setupPiece("d3", PieceType.PAWN, PieceColor.WHITE);
        setupPiece("e4", PieceType.PAWN, PieceColor.WHITE);
        setupPiece("f5", PieceType.PAWN, PieceColor.WHITE);
        setupPiece("g4", PieceType.PAWN, PieceColor.WHITE);
        setupPiece("h3", PieceType.PAWN, PieceColor.WHITE);
        setupPiece("i2", PieceType.PAWN, PieceColor.WHITE);
        setupPiece("k1", PieceType.PAWN, PieceColor.WHITE);

        // Setup Black pieces
        setupPiece("c8", PieceType.ROOK, PieceColor.BLACK);
        setupPiece("i8", PieceType.ROOK, PieceColor.BLACK);
        setupPiece("d9", PieceType.KNIGHT, PieceColor.BLACK);
        setupPiece("h9", PieceType.KNIGHT, PieceColor.BLACK);
        setupPiece("e10", PieceType.QUEEN, PieceColor.BLACK);
        setupPiece("g10", PieceType.KING, PieceColor.BLACK);
        setupPiece("f11", PieceType.BISHOP, PieceColor.BLACK);
        setupPiece("f10", PieceType.BISHOP, PieceColor.BLACK);
        setupPiece("f9", PieceType.BISHOP, PieceColor.BLACK);

        setupPiece("b7", PieceType.PAWN, PieceColor.BLACK);
        setupPiece("c7", PieceType.PAWN, PieceColor.BLACK);
        setupPiece("d7", PieceType.PAWN, PieceColor.BLACK);
        setupPiece("e7", PieceType.PAWN, PieceColor.BLACK);
        setupPiece("f7", PieceType.PAWN, PieceColor.BLACK);
        setupPiece("g7", PieceType.PAWN, PieceColor.BLACK);
        setupPiece("h7", PieceType.PAWN, PieceColor.BLACK);
        setupPiece("i7", PieceType.PAWN, PieceColor.BLACK);
        setupPiece("k7", PieceType.PAWN, PieceColor.BLACK);

        positionHistory.add(serializeBoard());
    }

    private void setupPiece(String coord, PieceType type, PieceColor color) {
        int[] qr = HexCoord.parseAlgebraic(coord);
        if (qr != null) {
            setPiece(qr[0], qr[1], new Piece(type, color));
        }
    }

    public static int[] parseAlgebraic(String code) {
        return HexCoord.parseAlgebraic(code);
    }

    public static String toAlgebraic(int q, int r) {
        return HexCoord.toAlgebraic(q, r);
    }

    public boolean isValidMove(int fromQ, int fromR, int toQ, int toR) {
        if (!isValidCoord(fromQ, fromR) || !isValidCoord(toQ, toR)) {
            return false;
        }

        Piece p = getPiece(fromQ, fromR);
        if (p == null || p.getColor() != currentTurn) {
            return false;
        }

        if (fromQ == toQ && fromR == toR) {
            return false;
        }

        Piece target = getPiece(toQ, toR);
        if (target != null && target.getColor() == p.getColor()) {
            return false;
        }

        if (!isValidPieceMoveBasic(fromQ, fromR, toQ, toR)) {
            return false;
        }

        // Simulate move to check if King is placed/left in check
        Piece originalTarget = getPiece(toQ, toR);
        boolean isEnPassant = false;
        if (p.getType() == PieceType.PAWN && originalTarget == null) {
            int[] ep = getEnPassantTarget();
            if (ep != null && ep[0] == toQ && ep[1] == toR) {
                isEnPassant = true;
            }
        }

        setPiece(toQ, toR, p);
        setPiece(fromQ, fromR, null);
        Piece capturedPawn = null;
        if (isEnPassant) {
            capturedPawn = getPiece(lastToQ, lastToR);
            setPiece(lastToQ, lastToR, null);
        }

        boolean inCheck = isInCheck(p.getColor());

        setPiece(fromQ, fromR, p);
        setPiece(toQ, toR, originalTarget);
        if (isEnPassant) {
            setPiece(lastToQ, lastToR, capturedPawn);
        }

        return !inCheck;
    }

    public boolean makeMove(int fromQ, int fromR, int toQ, int toR) {
        if (!isValidMove(fromQ, fromR, toQ, toR)) {
            return false;
        }

        Piece p = getPiece(fromQ, fromR);
        Piece destPiece = getPiece(toQ, toR);
        boolean isCapture = destPiece != null;
        boolean isPawnMove = p.getType() == PieceType.PAWN;

        boolean isEnPassant = false;
        if (isPawnMove && destPiece == null) {
            int[] ep = getEnPassantTarget();
            if (ep != null && ep[0] == toQ && ep[1] == toR) {
                isEnPassant = true;
            }
        }

        if (isCapture || isPawnMove) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }

        setPiece(toQ, toR, p);
        setPiece(fromQ, fromR, null);
        if (isEnPassant) {
            setPiece(lastToQ, lastToR, null);
        }

        // Auto pawn promotion to Queen at the end rank
        if (p.getType() == PieceType.PAWN) {
            int maxRank = 11 - Math.abs(toQ);
            int rank = (toQ < 0) ? (toR + 6 + toQ) : (toR + 6);
            if ((p.isWhite() && rank == maxRank) || (!p.isWhite() && rank == 1)) {
                setPiece(toQ, toR, new Piece(PieceType.QUEEN, p.getColor()));
            }
        }

        lastFromQ = fromQ;
        lastFromR = fromR;
        lastToQ = toQ;
        lastToR = toR;

        currentTurn = (currentTurn == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;

        positionHistory.add(serializeBoard());
        return true;
    }

    public int[] getEnPassantTarget() {
        if (lastFromQ == -999 || lastToQ == -999) {
            return null;
        }
        Piece p = getPiece(lastToQ, lastToR);
        if (p != null && p.getType() == PieceType.PAWN) {
            if (Math.abs(lastToR - lastFromR) == 2 && lastToQ == lastFromQ) {
                return new int[]{lastToQ, (lastFromR + lastToR) / 2};
            }
        }
        return null;
    }

    public boolean isValidPieceMoveBasic(int fromQ, int fromR, int toQ, int toR) {
        Piece p = getPiece(fromQ, fromR);
        if (p == null) {
            return false;
        }
        return ChessRules.isValidPieceMoveBasic(this, p, fromQ, fromR, toQ, toR);
    }

    public boolean isInCheck(PieceColor color) {
        return ChessRules.isInCheck(this, color);
    }

    public boolean hasValidMoves(PieceColor color) {
        return ChessRules.hasValidMoves(this, color);
    }

    public void startFromCurrentPosition() {
        halfMoveClock = 0;
        positionHistory.clear();
        positionHistory.add(serializeBoard());
        lastFromQ = -999;
        lastFromR = -999;
        lastToQ = -999;
        lastToR = -999;
    }

    public String validatePosition(PieceColor startingTurn) {
        return ChessRules.validatePosition(this, startingTurn);
    }

    public String checkDrawCriteria() {
        return ChessRules.checkDrawCriteria(this);
    }

    public Move getBotMove(PieceColor botColor) {
        return ChessBot.getBotMove(this, botColor);
    }

    public String serializeBoard() {
        StringBuilder sb = new StringBuilder();
        sb.append(currentTurn.name()).append(";");
        
        boolean first = true;
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (isValidCoord(q, r)) {
                    Piece p = getPiece(q, r);
                    if (p != null) {
                        String coord = toAlgebraic(q, r);
                        if (!first) {
                            sb.append(",");
                        }
                        sb.append(coord).append(":").append(p.getColor().name()).append(":").append(p.getType().name());
                        first = false;
                    }
                }
            }
        }
        return sb.toString();
    }

    public void deserializeBoard(String serialized) {
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (isValidCoord(q, r)) {
                    setPiece(q, r, null);
                }
            }
        }
        
        lastFromQ = -999;
        lastFromR = -999;
        lastToQ = -999;
        lastToR = -999;
        halfMoveClock = 0;
        positionHistory.clear();

        if (serialized == null || serialized.isEmpty()) {
            return;
        }

        String[] parts = serialized.split(";");
        if (parts.length > 0) {
            currentTurn = PieceColor.valueOf(parts[0]);
        }
        if (parts.length > 1 && !parts[1].isEmpty()) {
            String[] pieces = parts[1].split(",");
            for (String pStr : pieces) {
                String[] tokens = pStr.split(":");
                if (tokens.length == 3) {
                    String coord = tokens[0];
                    PieceColor color = PieceColor.valueOf(tokens[1]);
                    PieceType type = PieceType.valueOf(tokens[2]);
                    setupPiece(coord, type, color);
                }
            }
        }
        positionHistory.add(serializeBoard());
    }
}
