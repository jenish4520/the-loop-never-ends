package seda_project.control_alt_defeat.gamebox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {

    @Test
    void testValidConfig() {
        GameConfig config = new GameConfig(2, 16);
        assertEquals(2, config.getMatchSize());
        assertEquals(16, config.getDeckSize());
        assertEquals(8, config.getUniqueSymbolCount());
    }

    @ParameterizedTest
    @CsvSource({"2,4", "2,16", "2,36", "3,12", "3,36", "4,16", "4,36", "5,10", "5,30", "6,12", "6,36"})
    void testValidConfigurations(int matchSize, int deckSize) {
        assertDoesNotThrow(() -> new GameConfig(matchSize, deckSize));
    }

    @Test
    void testMatchSizeTooSmall() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(0, 10));
    }

    @Test
    void testMatchSizeTooLarge() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(46, 46));
    }

    @Test
    void testDeckSizeTooSmall() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(2, 0));
    }

    @Test
    void testDeckSizeTooLarge() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(2, 46));
    }

    @Test
    void testDeckSizeNotDivisible() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(3, 16));
    }

    @Test
    void testUniqueSymbolCount() {
        assertEquals(8, new GameConfig(2, 16).getUniqueSymbolCount());
        assertEquals(4, new GameConfig(3, 12).getUniqueSymbolCount());
        assertEquals(3, new GameConfig(4, 12).getUniqueSymbolCount());
        assertEquals(6, new GameConfig(5, 30).getUniqueSymbolCount());
        assertEquals(6, new GameConfig(6, 36).getUniqueSymbolCount());
    }
}
