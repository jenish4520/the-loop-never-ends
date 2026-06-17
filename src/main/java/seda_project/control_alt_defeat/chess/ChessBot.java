package seda_project.control_alt_defeat.chess;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChessBot {

    public static Move getBotMove(ChessBoard board, ChessBoard.PieceColor botColor) {
        List<Move> legalMoves = getLegalMoves(board, botColor);
        if (legalMoves.isEmpty()) {
            return null;
        }
        if (legalMoves.size() == 1) {
            return legalMoves.get(0);
        }

        // 1. Look for mate in one
        ChessBoard.PieceColor opponentColor = (botColor == ChessBoard.PieceColor.WHITE) ? ChessBoard.PieceColor.BLACK : ChessBoard.PieceColor.WHITE;
        for (Move move : legalMoves) {
            if (isMoveDeliveringCheckmate(board, move.fromQ, move.fromR, move.toQ, move.toR, botColor)) {
                return move;
            }
        }

        // 2. Filter moves to avoid allowing opponent mate in one
        List<Move> safeMoves = new ArrayList<>();
        for (Move move : legalMoves) {
            Piece p = board.getPiece(move.fromQ, move.fromR);
            Piece originalTarget = board.getPiece(move.toQ, move.toR);
            board.setPiece(move.toQ, move.toR, p);
            board.setPiece(move.fromQ, move.fromR, null);

            boolean opponentHasMate = false;
            List<Move> opponentMoves = getLegalMoves(board, opponentColor);
            for (Move opMove : opponentMoves) {
                if (isMoveDeliveringCheckmate(board, opMove.fromQ, opMove.fromR, opMove.toQ, opMove.toR, opponentColor)) {
                    opponentHasMate = true;
                    break;
                }
            }

            // Revert
            board.setPiece(move.fromQ, move.fromR, p);
            board.setPiece(move.toQ, move.toR, originalTarget);

            if (!opponentHasMate) {
                safeMoves.add(move);
            }
        }

        List<Move> candidates = safeMoves.isEmpty() ? legalMoves : safeMoves;

        Move bestMove = null;
        int bestScore = -9999;
        Random rand = new Random();

        for (Move move : candidates) {
            Piece target = board.getPiece(move.toQ, move.toR);
            int score = 0;
            if (target != null) {
                score = switch (target.getType()) {
                    case PAWN -> 10;
                    case KNIGHT -> 30;
                    case BISHOP -> 30;
                    case ROOK -> 50;
                    case QUEEN -> 90;
                    case KING -> 1000;
                };
            }
            score += rand.nextInt(5);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }

    public static List<Move> getLegalMoves(ChessBoard board, ChessBoard.PieceColor color) {
        List<Move> moves = new ArrayList<>();
        ChessBoard.PieceColor originalTurn = board.getCurrentTurn();
        board.setCurrentTurn(color);

        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (HexCoord.isValid(q, r)) {
                    Piece p = board.getPiece(q, r);
                    if (p != null && p.getColor() == color) {
                        for (int tq = -5; tq <= 5; tq++) {
                            for (int tr = -5; tr <= 5; tr++) {
                                if (HexCoord.isValid(tq, tr)) {
                                    if (board.isValidMove(q, r, tq, tr)) {
                                        moves.add(new Move(q, r, tq, tr));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        board.setCurrentTurn(originalTurn);
        return moves;
    }

    public static boolean isMoveDeliveringCheckmate(ChessBoard board, int fromQ, int fromR, int toQ, int toR, ChessBoard.PieceColor attackerColor) {
        Piece p = board.getPiece(fromQ, fromR);
        Piece originalTarget = board.getPiece(toQ, toR);

        // Make move
        board.setPiece(toQ, toR, p);
        board.setPiece(fromQ, fromR, null);

        // Check opponent's status
        ChessBoard.PieceColor originalTurn = board.getCurrentTurn();
        ChessBoard.PieceColor defenderColor = (attackerColor == ChessBoard.PieceColor.WHITE) ? ChessBoard.PieceColor.BLACK : ChessBoard.PieceColor.WHITE;
        board.setCurrentTurn(defenderColor);

        boolean isMate = ChessRules.isInCheck(board, defenderColor) && !ChessRules.hasValidMoves(board, defenderColor);

        // Revert move and turn
        board.setCurrentTurn(originalTurn);
        board.setPiece(fromQ, fromR, p);
        board.setPiece(toQ, toR, originalTarget);

        return isMate;
    }
}
