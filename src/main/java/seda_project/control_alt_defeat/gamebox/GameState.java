package seda_project.control_alt_defeat.gamebox;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Game state snapshot.
public record GameState(List<Card> cards, int player1Score, int player2Score, int activePlayer, GamePhase phase,
        List<Integer> currentAttempt, int matchSize, int deckSize, String statusMessage) implements Serializable {
    public GameState {
        cards = cards.stream().map(Card::copy).toList();
        currentAttempt = List.copyOf(currentAttempt);
    }

    public List<Card> getCards() {
        return cards;
    }

    public int getPlayer1Score() {
        return player1Score;
    }

    public int getPlayer2Score() {
        return player2Score;
    }

    public int getActivePlayer() {
        return activePlayer;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public List<Integer> getCurrentAttempt() {
        return currentAttempt;
    }

    public int getMatchSize() {
        return matchSize;
    }

    public int getDeckSize() {
        return deckSize;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isGameOver() {
        return phase == GamePhase.GAME_OVER;
    }

    public int getWinner() {
        return player1Score > player2Score ? 1 : player2Score > player1Score ? 2 : 0;
    }

    public int getTotalMatched() {
        return (int) cards.stream().filter(Card::isMatched).count();
    }
}
