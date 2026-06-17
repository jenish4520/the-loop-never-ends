package seda_project.control_alt_defeat.chess;

public class Piece {
    private final ChessBoard.PieceType type;
    private final ChessBoard.PieceColor color;

    public Piece(ChessBoard.PieceType type, ChessBoard.PieceColor color) {
        this.type = type;
        this.color = color;
    }

    public ChessBoard.PieceType getType() {
        return type;
    }

    public ChessBoard.PieceColor getColor() {
        return color;
    }

    public boolean isWhite() {
        return color == ChessBoard.PieceColor.WHITE;
    }

    public String getSymbol() {
        return switch (type) {
            case PAWN -> "♟";
            case ROOK -> "♜";
            case KNIGHT -> "♞";
            case BISHOP -> "♝";
            case QUEEN -> "♛";
            case KING -> "♚";
        };
    }
}
