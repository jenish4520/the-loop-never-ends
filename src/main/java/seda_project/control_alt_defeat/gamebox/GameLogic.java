package seda_project.control_alt_defeat.gamebox;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Handles all the rules and score-keeping for the memory card game.
public class GameLogic {

    public static final String[] SYMBOL_POOL = {
        "Pikachu", "Charizard", "Bulbasaur", "Squirtle",
        "Jigglypuff", "Eevee", "Snorlax", "Mewtwo",
        "Gengar", "Lucario", "Mew", "Togepi",
        "Psyduck", "Meowth", "Machamp", "Alakazam",
        "Gyarados", "Lapras", "Ditto", "Dragonite",
        "Arcanine", "Vulpix", "Ninetales", "Umbreon",
        "Espeon", "Sylveon", "Leafeon", "Glaceon",
        "Jolteon", "Flareon", "Vaporeon", "Pichu",
        "Raichu", "Charmander", "Charmeleon", "Ivysaur"
    };

    private List<Card> cards;
    private int player1Score;
    private int player2Score;
    private int activePlayer;
    private GamePhase phase;
    private List<Integer> currentAttempt;
    private int matchSize;
    private int deckSize;

    // wipe the old game and set up a fresh shuffled deck
    public synchronized void initializeGame(GameConfig config) {
        this.matchSize = config.getMatchSize();
        this.deckSize = config.getDeckSize();
        this.player1Score = 0;
        this.player2Score = 0;
        this.activePlayer = 1;
        this.phase = GamePhase.PLAYING;
        this.currentAttempt = new ArrayList<>();
        this.cards = generateDeck(config);
    }

    // fill the deck with the right number of copies of each symbol, then shuffle
    private List<Card> generateDeck(GameConfig config) {
        int uniqueSymbols = config.getUniqueSymbolCount();
        if (uniqueSymbols > SYMBOL_POOL.length) {
            throw new IllegalArgumentException(
                "Not enough symbols. Need " + uniqueSymbols + " but have " + SYMBOL_POOL.length);
        }

        List<Card> deck = new ArrayList<>();
        int cardId = 0;
        for (int s = 0; s < uniqueSymbols; s++) {
            for (int k = 0; k < config.getMatchSize(); k++) {
                deck.add(new Card(cardId++, SYMBOL_POOL[s]));
            }
        }
        Collections.shuffle(deck);
        return deck;
    }

    // called whenever a player taps a card
    public synchronized boolean handleCardClick(int cardIndex, int playerNumber) {
        if (phase != GamePhase.PLAYING) return false;
        if (playerNumber != activePlayer) return false;
        if (cardIndex < 0 || cardIndex >= cards.size()) return false;

        Card card = cards.get(cardIndex);
        if (!card.isClickable()) return false;

        card.setFaceUp(true);
        currentAttempt.add(cardIndex);

        if (currentAttempt.size() == matchSize) {
            resolveAttempt();
        }

        return true;
    }

    // called once the player has picked n cards — figure out if they matched
    private void resolveAttempt() {
        String firstSymbol = cards.get(currentAttempt.get(0)).getSymbol();
        boolean allMatch = currentAttempt.stream()
            .allMatch(i -> cards.get(i).getSymbol().equals(firstSymbol));

        if (allMatch) {
            // Match
            for (int idx : currentAttempt) {
                cards.get(idx).setMatched(true);
                cards.get(idx).setMatchedByPlayer(activePlayer);
            }
            if (activePlayer == 1) player1Score++;
            else player2Score++;
            currentAttempt.clear();

            if (cards.stream().allMatch(Card::isMatched)) {
                phase = GamePhase.GAME_OVER;
            }
        } else {
            // Mismatch
            phase = GamePhase.RESOLVING;
        }
    }

    // flip the wrong cards back and hand the turn to the other player
    public synchronized void resolveMismatch() {
        if (phase != GamePhase.RESOLVING) return;

        for (int idx : currentAttempt) {
            cards.get(idx).setFaceUp(false);
        }
        currentAttempt.clear();
        activePlayer = (activePlayer == 1) ? 2 : 1;
        phase = GamePhase.PLAYING;
    }

    // overload for when we don't need a custom status message
    public synchronized GameState getState() {
        return getState("");
    }

    // snapshot the board and attach a message (e.g. "Player 1's turn!")
    public synchronized GameState getState(String statusMessage) {
        return new GameState(
            cards, player1Score, player2Score,
            activePlayer, phase, currentAttempt,
            matchSize, deckSize, statusMessage
        );
    }

    public synchronized GamePhase getPhase() { return phase; }
    public synchronized int getActivePlayer() { return activePlayer; }
    public synchronized int getPlayer1Score() { return player1Score; }
    public synchronized int getPlayer2Score() { return player2Score; }
    public synchronized int getMatchSize() { return matchSize; }
    public synchronized int getDeckSize() { return deckSize; }
    public synchronized List<Integer> getCurrentAttempt() { return new ArrayList<>(currentAttempt); }

    // handy for the status bar — how many cards are already off the board?
    public synchronized int getMatchedCount() {
        return (int) cards.stream().filter(Card::isMatched).count();
    }
}
