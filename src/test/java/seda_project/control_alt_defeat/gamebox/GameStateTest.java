package seda_project.control_alt_defeat.gamebox;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    void testGameStateIsImmutableSnapshot() {
        List<Card> cards = new ArrayList<>();
        cards.add(new Card(0, "A"));
        cards.add(new Card(1, "B"));

        List<Integer> attempt = new ArrayList<>();
        attempt.add(0);

        GameState state = new GameState(cards, 1, 2, 1,
            GamePhase.PLAYING, attempt, 2, 4, "Test");

        // Modify original lists
        cards.get(0).setFaceUp(true);
        attempt.add(1);

        // State should not be affected
        assertFalse(state.getCards().get(0).isFaceUp(), "State cards should be independent copies");
        assertEquals(1, state.getCurrentAttempt().size(), "State attempt list should be independent");
    }

    @Test
    void testGameStateUnmodifiable() {
        GameState state = createTestState(GamePhase.PLAYING);

        assertThrows(UnsupportedOperationException.class,
            () -> state.getCards().add(new Card(99, "X")));
        assertThrows(UnsupportedOperationException.class,
            () -> state.getCurrentAttempt().add(99));
    }

    @Test
    void testWinnerDetermination() {
        // Player 1 wins
        GameState p1Wins = new GameState(
            List.of(), 5, 3, 1, GamePhase.GAME_OVER, List.of(), 2, 16, "");
        assertEquals(1, p1Wins.getWinner());

        // Player 2 wins
        GameState p2Wins = new GameState(
            List.of(), 3, 5, 1, GamePhase.GAME_OVER, List.of(), 2, 16, "");
        assertEquals(2, p2Wins.getWinner());

        // Draw
        GameState draw = new GameState(
            List.of(), 4, 4, 1, GamePhase.GAME_OVER, List.of(), 2, 16, "");
        assertEquals(0, draw.getWinner());
    }

    @Test
    void testIsGameOver() {
        assertFalse(createTestState(GamePhase.PLAYING).isGameOver());
        assertFalse(createTestState(GamePhase.RESOLVING).isGameOver());
        assertFalse(createTestState(GamePhase.LOBBY).isGameOver());
        assertTrue(createTestState(GamePhase.GAME_OVER).isGameOver());
    }

    @Test
    void testTotalMatched() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Card c = new Card(i, "A");
            if (i < 2) c.setMatched(true);
            cards.add(c);
        }

        GameState state = new GameState(cards, 0, 0, 1,
            GamePhase.PLAYING, List.of(), 2, 4, "");
        assertEquals(2, state.getTotalMatched());
    }

    private GameState createTestState(GamePhase phase) {
        return new GameState(List.of(), 0, 0, 1, phase, List.of(), 2, 4, "");
    }
}
