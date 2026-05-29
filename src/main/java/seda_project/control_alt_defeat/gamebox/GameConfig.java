package seda_project.control_alt_defeat.gamebox;

import java.io.Serializable;

// Stores setup parameters.
public record GameConfig(int matchSize, int deckSize) implements Serializable {
    public static final int MIN_MATCH_SIZE = 1, MAX_MATCH_SIZE = 45;
    public static final int MIN_DECK_SIZE = 1, MAX_DECK_SIZE = 45;

    public GameConfig {
        validate(matchSize, deckSize);
    }

    public static void validate(int matchSize, int deckSize) {
        if (matchSize < MIN_MATCH_SIZE || matchSize > MAX_MATCH_SIZE)
            throw new IllegalArgumentException("Match size n must be between " + MIN_MATCH_SIZE + " and "
                    + MAX_MATCH_SIZE + ", got: " + matchSize);
        if (deckSize < MIN_DECK_SIZE || deckSize > MAX_DECK_SIZE)
            throw new IllegalArgumentException(
                    "Deck size must be between " + MIN_DECK_SIZE + " and " + MAX_DECK_SIZE + ", got: " + deckSize);
        if (deckSize % matchSize != 0)
            throw new IllegalArgumentException(
                    "Deck size (" + deckSize + ") must be divisible by match size n (" + matchSize + ")");
    }

    public int getUniqueSymbolCount() {
        return deckSize / matchSize;
    }

    public int getMatchSize() {
        return matchSize;
    }

    public int getDeckSize() {
        return deckSize;
    }
}
