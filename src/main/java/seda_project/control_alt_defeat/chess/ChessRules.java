package seda_project.control_alt_defeat.chess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChessRules {

    public static boolean isValidPieceMoveBasic(ChessBoard board, Piece p, int fromQ, int fromR, int toQ, int toR) {
        int dq = toQ - fromQ;
        int dr = toR - fromR;

        switch (p.getType()) {
            case PAWN -> {
                int forwardR = p.isWhite() ? 1 : -1;
                // Move 1 step forward orthogonally
                if (dq == 0 && dr == forwardR) {
                    return board.getPiece(toQ, toR) == null;
                }
                // Move 2 steps forward if on pawn's starting cell
                if (dq == 0 && dr == 2 * forwardR) {
                    boolean isStart = p.isWhite() ? isWhitePawnStart(fromQ, fromR) : isBlackPawnStart(fromQ, fromR);
                    if (isStart) {
                        return board.getPiece(toQ, toR) == null && board.getPiece(fromQ, fromR + forwardR) == null;
                    }
                }
                // Captures diagonally forward (NW and NE for White, SW and SE for Black)
                boolean isPawnCapture = false;
                if (p.isWhite()) {
                    isPawnCapture = (dq == -1 && dr == 1) || (dq == 1 && dr == 0);
                } else {
                    isPawnCapture = (dq == -1 && dr == 0) || (dq == 1 && dr == -1);
                }
                if (isPawnCapture) {
                    Piece target = board.getPiece(toQ, toR);
                    if (target != null && target.getColor() != p.getColor()) {
                        return true;
                    }
                    int[] ep = board.getEnPassantTarget();
                    if (ep != null && ep[0] == toQ && ep[1] == toR) {
                        return true;
                    }
                    return false;
                }
                return false;
            }
            case ROOK -> {
                if (dq == 0 || dr == 0 || dq + dr == 0) {
                    int sq = Integer.compare(dq, 0);
                    int sr = Integer.compare(dr, 0);
                    return isPathClear(board, fromQ, fromR, toQ, toR, sq, sr);
                }
                return false;
            }
            case BISHOP -> {
                if (dq == dr || dq == -2 * dr || dr == -2 * dq) {
                    int sq, sr;
                    if (dq == dr) {
                        sq = Integer.compare(dq, 0);
                        sr = sq;
                    } else if (dq == -2 * dr) {
                        sr = Integer.compare(dr, 0);
                        sq = -2 * sr;
                    } else {
                        sq = Integer.compare(dq, 0);
                        sr = -2 * sq;
                    }
                    return isPathClear(board, fromQ, fromR, toQ, toR, sq, sr);
                }
                return false;
            }
            case KNIGHT -> {
                // 12 Hex Knight vectors
                return (dq == 1 && dr == 2) || (dq == 2 && dr == 1) || (dq == 3 && dr == -1) || (dq == 3 && dr == -2) ||
                       (dq == 2 && dr == -3) || (dq == 1 && dr == -3) || (dq == -1 && dr == -2) || (dq == -2 && dr == -1) ||
                       (dq == -3 && dr == 1) || (dq == -3 && dr == 2) || (dq == -2 && dr == 3) || (dq == -1 && dr == 3);
            }
            case QUEEN -> {
                // Combines Rook and Bishop directions
                boolean isRookMove = (dq == 0 || dr == 0 || dq + dr == 0);
                boolean isBishopMove = (dq == dr || dq == -2 * dr || dr == -2 * dq);
                if (isRookMove) {
                    int sq = Integer.compare(dq, 0);
                    int sr = Integer.compare(dr, 0);
                    return isPathClear(board, fromQ, fromR, toQ, toR, sq, sr);
                }
                if (isBishopMove) {
                    int sq, sr;
                    if (dq == dr) {
                        sq = Integer.compare(dq, 0);
                        sr = sq;
                    } else if (dq == -2 * dr) {
                        sr = Integer.compare(dr, 0);
                        sq = -2 * sr;
                    } else {
                        sq = Integer.compare(dq, 0);
                        sr = -2 * sq;
                    }
                    return isPathClear(board, fromQ, fromR, toQ, toR, sq, sr);
                }
                return false;
            }
            case KING -> {
                boolean isRookStep = (dq == 0 && Math.abs(dr) == 1) || (dr == 0 && Math.abs(dq) == 1) || (dq + dr == 0 && Math.abs(dq) == 1);
                boolean isBishopStep = (dq == dr && Math.abs(dq) == 1) || (dq == -2 * dr && Math.abs(dr) == 1) || (dr == -2 * dq && Math.abs(dq) == 1);
                return isRookStep || isBishopStep;
            }
        }
        return false;
    }

    private static boolean isPathClear(ChessBoard board, int fromQ, int fromR, int toQ, int toR, int sq, int sr) {
        int q = fromQ + sq;
        int r = fromR + sr;
        while (q != toQ || r != toR) {
            if (board.getPiece(q, r) != null) {
                return false;
            }
            q += sq;
            r += sr;
        }
        return true;
    }

    private static boolean isWhitePawnStart(int q, int r) {
        if (q < -4 || q > 4) return false;
        if (q <= 0) {
            return r == -1;
        } else {
            return r == -(q + 1);
        }
    }

    private static boolean isBlackPawnStart(int q, int r) {
        if (q < -4 || q > 4) return false;
        if (q <= 0) {
            return r == (1 - q);
        } else {
            return r == 1;
        }
    }

    public static boolean isInCheck(ChessBoard board, ChessBoard.PieceColor color) {
        int kingQ = -999;
        int kingR = -999;

        // Find king
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (HexCoord.isValid(q, r)) {
                    Piece p = board.getPiece(q, r);
                    if (p != null && p.getType() == ChessBoard.PieceType.KING && p.getColor() == color) {
                        kingQ = q;
                        kingR = r;
                        break;
                    }
                }
            }
            if (kingQ != -999) break;
        }

        if (kingQ == -999) return false;

        // Check if any opponent piece can attack king
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (HexCoord.isValid(q, r)) {
                    Piece p = board.getPiece(q, r);
                    if (p != null && p.getColor() != color) {
                        if (isValidPieceMoveBasic(board, p, q, r, kingQ, kingR)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasValidMoves(ChessBoard board, ChessBoard.PieceColor color) {
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (HexCoord.isValid(q, r)) {
                    Piece p = board.getPiece(q, r);
                    if (p != null && p.getColor() == color) {
                        for (int tq = -5; tq <= 5; tq++) {
                            for (int tr = -5; tr <= 5; tr++) {
                                if (HexCoord.isValid(tq, tr)) {
                                    if (board.isValidMove(q, r, tq, tr)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public static String validatePosition(ChessBoard board, ChessBoard.PieceColor startingTurn) {
        int whiteKings = 0;
        int blackKings = 0;
        int whitePawns = 0;
        int blackPawns = 0;
        int wkQ = -999, wkR = -999;
        int bkQ = -999, bkR = -999;

        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (HexCoord.isValid(q, r)) {
                    Piece p = board.getPiece(q, r);
                    if (p != null) {
                        if (p.getType() == ChessBoard.PieceType.KING) {
                            if (p.isWhite()) {
                                whiteKings++;
                                wkQ = q;
                                wkR = r;
                            } else {
                                blackKings++;
                                bkQ = q;
                                bkR = r;
                            }
                        } else if (p.getType() == ChessBoard.PieceType.PAWN) {
                            if (p.isWhite()) {
                                whitePawns++;
                            } else {
                                blackPawns++;
                            }
                        }
                    }
                }
            }
        }

        if (whiteKings != 1) {
            return "Position must have exactly one White King (found " + whiteKings + ")";
        }
        if (blackKings != 1) {
            return "Position must have exactly one Black King (found " + blackKings + ")";
        }
        if (whitePawns > 9) {
            return "Position cannot have more than 9 White Pawns (found " + whitePawns + ")";
        }
        if (blackPawns > 9) {
            return "Position cannot have more than 9 Black Pawns (found " + blackPawns + ")";
        }

        // King next to king
        int dq = bkQ - wkQ;
        int dr = bkR - wkR;
        boolean isRookStep = (dq == 0 && Math.abs(dr) == 1) || (dr == 0 && Math.abs(dq) == 1) || (dq + dr == 0 && Math.abs(dq) == 1);
        boolean isBishopStep = (dq == dr && Math.abs(dq) == 1) || (dq == -2 * dr && Math.abs(dr) == 1) || (dr == -2 * dq && Math.abs(dq) == 1);
        if (isRookStep || isBishopStep) {
            return "Kings cannot be placed next to each other";
        }

        // The side whose turn it is NOT cannot be in check
        ChessBoard.PieceColor originalTurn = board.getCurrentTurn();
        board.setCurrentTurn(startingTurn);
        ChessBoard.PieceColor opponentColor = (startingTurn == ChessBoard.PieceColor.WHITE) ? ChessBoard.PieceColor.BLACK : ChessBoard.PieceColor.WHITE;
        if (isInCheck(board, opponentColor)) {
            board.setCurrentTurn(originalTurn);
            return (opponentColor == ChessBoard.PieceColor.WHITE ? "White" : "Black") + " King cannot be in check when it is " + (startingTurn == ChessBoard.PieceColor.WHITE ? "White" : "Black") + " to play";
        }

        board.setCurrentTurn(originalTurn);
        return null;
    }

    public static String checkDrawCriteria(ChessBoard board) {
        if (board.getHalfMoveClock() >= 100) {
            return "50-move rule";
        }
        if (isInsufficientMaterial(board)) {
            return "insufficient material";
        }
        if (isThreefoldRepetition(board)) {
            return "threefold repetition";
        }
        return null;
    }

    public static boolean isInsufficientMaterial(ChessBoard board) {
        List<Piece> whitePieces = new ArrayList<>();
        List<Piece> blackPieces = new ArrayList<>();

        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (HexCoord.isValid(q, r)) {
                    Piece p = board.getPiece(q, r);
                    if (p != null) {
                        if (p.isWhite()) {
                            whitePieces.add(p);
                        } else {
                            blackPieces.add(p);
                        }
                    }
                }
            }
        }

        int wSize = whitePieces.size();
        int bSize = blackPieces.size();

        // King vs King
        if (wSize == 1 && bSize == 1) {
            return true;
        }

        // King + Bishop vs King
        if (wSize == 2 && bSize == 1) {
            Piece other = whitePieces.get(0).getType() == ChessBoard.PieceType.KING ? whitePieces.get(1) : whitePieces.get(0);
            if (other.getType() == ChessBoard.PieceType.BISHOP) return true;
        }
        if (bSize == 2 && wSize == 1) {
            Piece other = blackPieces.get(0).getType() == ChessBoard.PieceType.KING ? blackPieces.get(1) : blackPieces.get(0);
            if (other.getType() == ChessBoard.PieceType.BISHOP) return true;
        }

        // King + Knight vs King
        if (wSize == 2 && bSize == 1) {
            Piece other = whitePieces.get(0).getType() == ChessBoard.PieceType.KING ? whitePieces.get(1) : whitePieces.get(0);
            if (other.getType() == ChessBoard.PieceType.KNIGHT) return true;
        }
        if (bSize == 2 && wSize == 1) {
            Piece other = blackPieces.get(0).getType() == ChessBoard.PieceType.KING ? blackPieces.get(1) : blackPieces.get(0);
            if (other.getType() == ChessBoard.PieceType.KNIGHT) return true;
        }

        return false;
    }

    public static boolean isThreefoldRepetition(ChessBoard board) {
        Map<String, Integer> counts = new HashMap<>();
        for (String pos : board.getPositionHistory()) {
            counts.put(pos, counts.getOrDefault(pos, 0) + 1);
            if (counts.get(pos) >= 3) {
                return true;
            }
        }
        return false;
    }
}
