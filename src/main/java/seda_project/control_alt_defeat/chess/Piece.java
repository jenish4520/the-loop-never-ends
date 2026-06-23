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

    public String getImageName() {
        String colorPrefix = isWhite() ? "w" : "b";
        String typeChar = switch (type) {
            case PAWN -> "P";
            case ROOK -> "R";
            case KNIGHT -> "N";
            case BISHOP -> "B";
            case QUEEN -> "Q";
            case KING -> "K";
        };
        return "merida/" + colorPrefix + typeChar + ".png";
    }
}
