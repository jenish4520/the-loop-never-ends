package seda_project.control_alt_defeat.gamebox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void testCardCreation() {
        Card card = new Card(0, "\u2605");
        assertEquals(0, card.getId());
        assertEquals("\u2605", card.getSymbol());
        assertFalse(card.isFaceUp());
        assertFalse(card.isMatched());
    }

    @Test
    void testCardIsClickable() {
        Card card = new Card(0, "\u2605");
        assertTrue(card.isClickable(), "New card should be clickable");

        card.setFaceUp(true);
        assertFalse(card.isClickable(), "Face-up card should not be clickable");

        card.setFaceUp(false);
        card.setMatched(true);
        assertFalse(card.isClickable(), "Matched card should not be clickable");
    }

    @Test
    void testCardCopy() {
        Card original = new Card(5, "\u2665");
        original.setFaceUp(true);
        original.setMatched(true);

        Card copy = original.copy();
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getSymbol(), copy.getSymbol());
        assertEquals(original.isFaceUp(), copy.isFaceUp());
        assertEquals(original.isMatched(), copy.isMatched());

        // Modify copy, original should not change
        copy.setFaceUp(false);
        assertTrue(original.isFaceUp());
    }

    @Test
    void testCardEquality() {
        Card card1 = new Card(0, "\u2605");
        Card card2 = new Card(0, "\u2665");
        Card card3 = new Card(1, "\u2605");

        assertEquals(card1, card2, "Cards with same ID should be equal");
        assertNotEquals(card1, card3, "Cards with different IDs should not be equal");
    }

    @Test
    void testCardStateTransitions() {
        Card card = new Card(0, "\u2605");

        // Initial state: face-down, not matched
        assertFalse(card.isFaceUp());
        assertFalse(card.isMatched());
        assertTrue(card.isClickable());

        // Flip face up
        card.setFaceUp(true);
        assertTrue(card.isFaceUp());
        assertFalse(card.isClickable());

        // Flip back down
        card.setFaceUp(false);
        assertTrue(card.isClickable());

        // Mark as matched
        card.setFaceUp(true);
        card.setMatched(true);
        assertFalse(card.isClickable());
    }
}
