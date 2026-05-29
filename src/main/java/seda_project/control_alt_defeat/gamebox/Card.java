package seda_project.control_alt_defeat.gamebox;

import java.io.Serializable;
import java.util.Objects;

// Card data record.
public class Card implements Serializable {
    private final int id;
    private final String symbol;
    private boolean faceUp, matched;
    private int matchedByPlayer;

    public Card(int id, String symbol) {
        this.id = id;
        this.symbol = symbol;
    }

    public Card copy() {
        Card copy = new Card(id, symbol);
        copy.faceUp = faceUp;
        copy.matched = matched;
        copy.matchedByPlayer = matchedByPlayer;
        return copy;
    }

    public int getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isFaceUp() {
        return faceUp;
    }

    public boolean isMatched() {
        return matched;
    }

    public int getMatchedByPlayer() {
        return matchedByPlayer;
    }

    public void setFaceUp(boolean faceUp) {
        this.faceUp = faceUp;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public void setMatchedByPlayer(int playerNum) {
        this.matchedByPlayer = playerNum;
    }

    public boolean isClickable() {
        return !faceUp && !matched;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Card c && id == c.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
