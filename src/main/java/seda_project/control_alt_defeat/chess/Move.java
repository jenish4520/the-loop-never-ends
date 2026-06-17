package seda_project.control_alt_defeat.chess;

public class Move {
    public final int fromQ, fromR, toQ, toR;

    public Move(int fromQ, int fromR, int toQ, int toR) {
        this.fromQ = fromQ;
        this.fromR = fromR;
        this.toQ = toQ;
        this.toR = toR;
    }
}
