package seda_project.control_alt_defeat.gamebox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GameLogicTest {

    private GameLogic logic;

    @BeforeEach
    void setUp() {
        logic = new GameLogic();
    }

    // ==================== Initialization Tests ====================

    @Test
    void testInitializeGame_createsCorrectNumberOfCards() {
        logic.initializeGame(new GameConfig(2, 16));
        GameState state = logic.getState();
        assertEquals(16, state.getCards().size());
    }

    @Test
    void testInitializeGame_allCardsFaceDown() {
        logic.initializeGame(new GameConfig(2, 16));
        GameState state = logic.getState();
        assertTrue(state.getCards().stream().noneMatch(Card::isFaceUp));
        assertTrue(state.getCards().stream().noneMatch(Card::isMatched));
    }

    @Test
    void testInitializeGame_scoresStartAtZero() {
        logic.initializeGame(new GameConfig(2, 16));
        GameState state = logic.getState();
        assertEquals(0, state.getPlayer1Score());
        assertEquals(0, state.getPlayer2Score());
    }

    @Test
    void testInitializeGame_player1StartsFirst() {
        logic.initializeGame(new GameConfig(2, 16));
        assertEquals(1, logic.getActivePlayer());
    }

    @Test
    void testInitializeGame_phaseIsPlaying() {
        logic.initializeGame(new GameConfig(2, 16));
        assertEquals(GamePhase.PLAYING, logic.getPhase());
    }

    @ParameterizedTest
    @CsvSource({"2,16", "3,12", "4,16", "5,10", "6,12"})
    void testInitializeGame_eachSymbolAppearsExactlyNTimes(int matchSize, int deckSize) {
        logic.initializeGame(new GameConfig(matchSize, deckSize));
        GameState state = logic.getState();

        Map<String, Long> counts = state.getCards().stream()
            .collect(Collectors.groupingBy(Card::getSymbol, Collectors.counting()));

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            assertEquals(matchSize, entry.getValue().intValue(),
                "Symbol '" + entry.getKey() + "' should appear exactly " + matchSize + " times");
        }
    }

    @Test
    void testInitializeGame_cardsAreShuffled() {
        // Run multiple times; at least one should differ from sorted order
        Set<String> orderings = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            logic.initializeGame(new GameConfig(2, 16));
            String order = logic.getState().getCards().stream()
                .map(Card::getSymbol)
                .collect(Collectors.joining());
            orderings.add(order);
        }
        assertTrue(orderings.size() > 1, "Cards should be shuffled (got same order every time)");
    }

    // ==================== Card Click Tests ====================

    @Test
    void testHandleCardClick_validClick() {
        logic.initializeGame(new GameConfig(2, 16));
        boolean result = logic.handleCardClick(0, 1);
        assertTrue(result, "First click by active player should succeed");
    }

    @Test
    void testHandleCardClick_cardFlipsFaceUp() {
        logic.initializeGame(new GameConfig(2, 16));
        logic.handleCardClick(0, 1);
        GameState state = logic.getState();
        assertTrue(state.getCards().get(0).isFaceUp());
    }

    @Test
    void testHandleCardClick_rejectOutOfTurn() {
        logic.initializeGame(new GameConfig(2, 16));
        // Player 2 tries to click when it's player 1's turn
        boolean result = logic.handleCardClick(0, 2);
        assertFalse(result, "Out-of-turn click should be rejected");
    }

    @Test
    void testHandleCardClick_rejectFaceUpCard() {
        logic.initializeGame(new GameConfig(2, 16));
        logic.handleCardClick(0, 1);
        boolean result = logic.handleCardClick(0, 1);
        assertFalse(result, "Cannot click already face-up card");
    }

    @Test
    void testHandleCardClick_rejectInvalidIndex() {
        logic.initializeGame(new GameConfig(2, 16));
        assertFalse(logic.handleCardClick(-1, 1));
        assertFalse(logic.handleCardClick(16, 1));
    }

    @Test
    void testHandleCardClick_rejectDuringResolving() {
        logic.initializeGame(new GameConfig(2, 16));
        // Force a mismatch
        forceMismatch(1);
        assertEquals(GamePhase.RESOLVING, logic.getPhase());
        assertFalse(logic.handleCardClick(5, 1), "Cannot click during RESOLVING phase");
    }

    // ==================== Match Resolution Tests ====================

    @Test
    void testMatch_scoreIncreases() {
        logic.initializeGame(new GameConfig(2, 16));
        forceMatch(1);
        assertEquals(1, logic.getPlayer1Score());
    }

    @Test
    void testMatch_playerKeepsTurn() {
        logic.initializeGame(new GameConfig(2, 16));
        forceMatch(1);
        assertEquals(1, logic.getActivePlayer(), "Player should keep turn after match");
    }

    @Test
    void testMatch_cardsMarkedMatched() {
        logic.initializeGame(new GameConfig(2, 16));
        List<Integer> matchedIndices = findMatchingPair();
        for (int idx : matchedIndices) {
            logic.handleCardClick(idx, 1);
        }
        GameState state = logic.getState();
        for (int idx : matchedIndices) {
            assertTrue(state.getCards().get(idx).isMatched());
        }
    }

    @Test
    void testMismatch_phaseBecomesResolving() {
        logic.initializeGame(new GameConfig(2, 16));
        forceMismatch(1);
        assertEquals(GamePhase.RESOLVING, logic.getPhase());
    }

    @Test
    void testMismatch_cardsStayFaceUpDuringResolving() {
        logic.initializeGame(new GameConfig(2, 16));
        List<Integer> mismatchIndices = findMismatchPair();
        for (int idx : mismatchIndices) {
            logic.handleCardClick(idx, 1);
        }
        GameState state = logic.getState();
        for (int idx : mismatchIndices) {
            assertTrue(state.getCards().get(idx).isFaceUp(),
                "Cards should stay face-up during RESOLVING");
        }
    }

    @Test
    void testResolveMismatch_cardsFlipBack() {
        logic.initializeGame(new GameConfig(2, 16));
        List<Integer> mismatchIndices = findMismatchPair();
        for (int idx : mismatchIndices) {
            logic.handleCardClick(idx, 1);
        }

        logic.resolveMismatch();
        GameState state = logic.getState();
        for (int idx : mismatchIndices) {
            assertFalse(state.getCards().get(idx).isFaceUp(),
                "Cards should be face-down after mismatch resolution");
        }
    }

    @Test
    void testResolveMismatch_turnSwitches() {
        logic.initializeGame(new GameConfig(2, 16));
        assertEquals(1, logic.getActivePlayer());
        forceMismatch(1);
        logic.resolveMismatch();
        assertEquals(2, logic.getActivePlayer(), "Turn should switch after mismatch");
    }

    @Test
    void testResolveMismatch_scoreUnchanged() {
        logic.initializeGame(new GameConfig(2, 16));
        forceMismatch(1);
        logic.resolveMismatch();
        assertEquals(0, logic.getPlayer1Score());
    }

    // ==================== Game Over Tests ====================

    @Test
    void testGameOver_whenAllCardsMatched() {
        logic.initializeGame(new GameConfig(2, 4));
        // 4 cards, 2 pairs — match them all
        forceAllMatches();
        assertEquals(GamePhase.GAME_OVER, logic.getPhase());
    }

    @Test
    void testGameOver_winnerDetermined() {
        logic.initializeGame(new GameConfig(2, 4));
        forceAllMatches();
        GameState state = logic.getState();
        assertTrue(state.isGameOver());
        // Winner is the one with more points
        int winner = state.getWinner();
        assertTrue(winner >= 0 && winner <= 2);
    }

    // ==================== n-of-a-kind Tests ====================

    @ParameterizedTest
    @CsvSource({"3,12", "4,16", "5,10"})
    void testNOfAKind_matchRequiresNCards(int matchSize, int deckSize) {
        logic.initializeGame(new GameConfig(matchSize, deckSize));

        // Open matchSize-1 cards of the same symbol — should not resolve yet
        GameState state = logic.getState();
        String targetSymbol = state.getCards().get(0).getSymbol();
        List<Integer> sameSymbol = new ArrayList<>();
        for (int i = 0; i < state.getCards().size(); i++) {
            if (state.getCards().get(i).getSymbol().equals(targetSymbol)) {
                sameSymbol.add(i);
            }
        }

        // Click matchSize-1 cards
        for (int i = 0; i < matchSize - 1; i++) {
            logic.handleCardClick(sameSymbol.get(i), 1);
        }
        assertEquals(GamePhase.PLAYING, logic.getPhase(),
            "Should still be PLAYING with " + (matchSize - 1) + " cards open");

        // Click the last one
        logic.handleCardClick(sameSymbol.get(matchSize - 1), 1);
        // Should now be matched (or resolving if mismatch, but these are same symbol)
        GameState afterMatch = logic.getState();
        for (int i = 0; i < matchSize; i++) {
            assertTrue(afterMatch.getCards().get(sameSymbol.get(i)).isMatched());
        }
    }

    // ==================== Restart Tests ====================

    @Test
    void testRestart_resetsState() {
        GameConfig config = new GameConfig(2, 16);
        logic.initializeGame(config);

        // Play some turns
        forceMatch(1);
        assertTrue(logic.getPlayer1Score() > 0);

        // Restart
        logic.initializeGame(config);
        assertEquals(0, logic.getPlayer1Score());
        assertEquals(0, logic.getPlayer2Score());
        assertEquals(1, logic.getActivePlayer());
        assertEquals(GamePhase.PLAYING, logic.getPhase());
        assertTrue(logic.getState().getCards().stream().noneMatch(Card::isFaceUp));
    }

    // ==================== Matched Card Rejection Tests ====================

    @Test
    void testRejectClickOnMatchedCard() {
        logic.initializeGame(new GameConfig(2, 16));
        List<Integer> matched = findMatchingPair();
        for (int idx : matched) {
            logic.handleCardClick(idx, 1);
        }
        // Cards are now matched — clicking them should fail
        assertFalse(logic.handleCardClick(matched.get(0), 1));
    }

    // ==================== Helper Methods ====================

    /** Finds and clicks a matching pair for the given player. */
    private void forceMatch(int player) {
        List<Integer> pair = findMatchingPair();
        for (int idx : pair) {
            logic.handleCardClick(idx, player);
        }
    }

    /** Finds and clicks a mismatching pair for the given player. */
    private void forceMismatch(int player) {
        List<Integer> pair = findMismatchPair();
        for (int idx : pair) {
            logic.handleCardClick(idx, player);
        }
    }

    /** Finds indices of n cards with the same symbol (a matching group). */
    private List<Integer> findMatchingPair() {
        GameState state = logic.getState();
        int matchSize = state.getMatchSize();
        Map<String, List<Integer>> symbolIndices = new HashMap<>();

        for (int i = 0; i < state.getCards().size(); i++) {
            Card c = state.getCards().get(i);
            if (!c.isMatched() && !c.isFaceUp()) {
                symbolIndices.computeIfAbsent(c.getSymbol(), k -> new ArrayList<>()).add(i);
            }
        }

        for (List<Integer> indices : symbolIndices.values()) {
            if (indices.size() >= matchSize) {
                return indices.subList(0, matchSize);
            }
        }
        throw new IllegalStateException("No matching group available");
    }

    /** Finds indices of n cards with different symbols (a mismatch). */
    private List<Integer> findMismatchPair() {
        GameState state = logic.getState();
        int matchSize = state.getMatchSize();
        List<Integer> result = new ArrayList<>();
        Set<String> usedSymbols = new HashSet<>();

        for (int i = 0; i < state.getCards().size() && result.size() < matchSize; i++) {
            Card c = state.getCards().get(i);
            if (!c.isMatched() && !c.isFaceUp() && !usedSymbols.contains(c.getSymbol())) {
                result.add(i);
                usedSymbols.add(c.getSymbol());
            }
        }

        if (result.size() < matchSize) {
            // Not enough distinct symbols — just pick any unmatched cards
            for (int i = 0; i < state.getCards().size() && result.size() < matchSize; i++) {
                Card c = state.getCards().get(i);
                if (!c.isMatched() && !c.isFaceUp() && !result.contains(i)) {
                    result.add(i);
                }
            }
        }

        return result;
    }

    /** Matches all cards in the game to trigger game over. */
    private void forceAllMatches() {
        int player = 1;
        while (logic.getPhase() == GamePhase.PLAYING) {
            try {
                List<Integer> pair = findMatchingPair();
                for (int idx : pair) {
                    logic.handleCardClick(idx, player);
                }
            } catch (IllegalStateException e) {
                break;
            }
        }
    }
}
