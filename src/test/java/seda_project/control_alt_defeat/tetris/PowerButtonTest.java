package seda_project.control_alt_defeat.tetris;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class PowerButtonTest {

    private GameLogic createLogic(boolean twoBlocksMode, boolean horizontalMode) {
        GameLogic logic = new GameLogic("Player1", "Player2", 1);
        logic.twoBlocksMode = twoBlocksMode;
        logic.horizontalMode = horizontalMode;
        logic.initModes();
        return logic;
    }

    private void alignPowerupWithPiece(PlayerState p, boolean secondPiece, String powerupType) {
        Tetromino piece = secondPiece ? p.activePiece2 : p.activePiece;
        assertNotNull(piece, "Piece should not be null");

        // Find the first solid block in the active piece
        int px = 0, py = 0;
        int[][] shape = piece.getShape();
        outer:
        for(int r=0; r<shape.length; r++) {
            for(int c=0; c<shape[r].length; c++) {
                if(shape[r][c] != 0) {
                    px = c; py = r; break outer;
                }
            }
        }

        int targetX = piece.x + px;
        int targetY = piece.y + py + 1; // 1 cell below

        switch (powerupType) {
            case "speedUp":
                p.board.hasSpeedUpPowerup = true;
                p.board.speedUpX = targetX;
                p.board.speedUpY = targetY;
                break;
            case "slowSelf":
                p.board.hasSlowSelfPowerup = true;
                p.board.slowSelfX = targetX;
                p.board.slowSelfY = targetY;
                break;
            case "delayOpponentRot":
                p.board.hasDelayOpponentRotPowerup = true;
                p.board.delayOpponentRotX = targetX;
                p.board.delayOpponentRotY = targetY;
                break;
            case "delaySelfRot":
                p.board.hasDelaySelfRotPowerup = true;
                p.board.delaySelfRotX = targetX;
                p.board.delaySelfRotY = targetY;
                break;
            case "slowOpponent":
                p.board.hasSlowOpponentPowerup = true;
                p.board.slowOpponentX = targetX;
                p.board.slowOpponentY = targetY;
                break;
            case "swap":
                p.board.hasSwapPowerup = true;
                p.board.swapX = targetX;
                p.board.swapY = targetY;
                break;
            case "portal":
                p.board.hasPortal = true;
                p.board.portalX = targetX;
                p.board.portalY = targetY;
                break;
        }
    }

    private void runTestsForModes(boolean twoBlocksMode, boolean horizontalMode) {
        testSpeedUp(twoBlocksMode, horizontalMode);
        testSlowSelf(twoBlocksMode, horizontalMode);
        testDelayOpponentRot(twoBlocksMode, horizontalMode);
        testDelaySelfRot(twoBlocksMode, horizontalMode);
        testSlowOpponent(twoBlocksMode, horizontalMode);
        testSwap(twoBlocksMode, horizontalMode);
        testPortal(twoBlocksMode, horizontalMode);
    }

    private void testSpeedUp(boolean twoBlocksMode, boolean horizontalMode) {
        GameLogic logic = createLogic(twoBlocksMode, horizontalMode);
        PlayerState p1 = logic.p1;
        PlayerState p2 = logic.p2;

        alignPowerupWithPiece(p1, false, "speedUp");
        assertTrue(p1.board.hasSpeedUpPowerup);
        
        logic.softDropPiece1(p1);
        
        assertFalse(p1.board.hasSpeedUpPowerup, "SpeedUp powerup should be consumed");
        assertTrue(p2.speedUpEndTime > System.currentTimeMillis(), "Opponent should be sped up");
    }

    private void testSlowSelf(boolean twoBlocksMode, boolean horizontalMode) {
        GameLogic logic = createLogic(twoBlocksMode, horizontalMode);
        PlayerState p1 = logic.p1;

        alignPowerupWithPiece(p1, false, "slowSelf");
        assertTrue(p1.board.hasSlowSelfPowerup);
        
        logic.softDropPiece1(p1);
        
        assertFalse(p1.board.hasSlowSelfPowerup, "SlowSelf powerup should be consumed");
        assertTrue(p1.slowDownEndTime > System.currentTimeMillis(), "Self should be slowed down");
    }

    private void testDelayOpponentRot(boolean twoBlocksMode, boolean horizontalMode) {
        GameLogic logic = createLogic(twoBlocksMode, horizontalMode);
        PlayerState p1 = logic.p1;
        PlayerState p2 = logic.p2;

        alignPowerupWithPiece(p1, false, "delayOpponentRot");
        assertTrue(p1.board.hasDelayOpponentRotPowerup);
        
        logic.softDropPiece1(p1);
        
        assertFalse(p1.board.hasDelayOpponentRotPowerup, "DelayOpponentRot powerup should be consumed");
        assertTrue(p2.rotationDelayEndTime > System.currentTimeMillis(), "Opponent rotation should be delayed");
    }

    private void testDelaySelfRot(boolean twoBlocksMode, boolean horizontalMode) {
        GameLogic logic = createLogic(twoBlocksMode, horizontalMode);
        PlayerState p1 = logic.p1;

        alignPowerupWithPiece(p1, false, "delaySelfRot");
        assertTrue(p1.board.hasDelaySelfRotPowerup);
        
        logic.softDropPiece1(p1);
        
        assertFalse(p1.board.hasDelaySelfRotPowerup, "DelaySelfRot powerup should be consumed");
        assertTrue(p1.rotationDelayEndTime > System.currentTimeMillis(), "Self rotation should be delayed");
    }

    private void testSlowOpponent(boolean twoBlocksMode, boolean horizontalMode) {
        GameLogic logic = createLogic(twoBlocksMode, horizontalMode);
        PlayerState p1 = logic.p1;
        PlayerState p2 = logic.p2;

        alignPowerupWithPiece(p1, false, "slowOpponent");
        assertTrue(p1.board.hasSlowOpponentPowerup);
        
        logic.softDropPiece1(p1);
        
        assertFalse(p1.board.hasSlowOpponentPowerup, "SlowOpponent powerup should be consumed");
        assertTrue(p2.slowDownEndTime > System.currentTimeMillis(), "Opponent should be slowed down");
    }

    private void testSwap(boolean twoBlocksMode, boolean horizontalMode) {
        GameLogic logic = createLogic(twoBlocksMode, horizontalMode);
        PlayerState p1 = logic.p1;
        PlayerState p2 = logic.p2;

        String[][] initialP1Grid = p1.board.grid;
        String[][] initialP2Grid = p2.board.grid;

        alignPowerupWithPiece(p1, false, "swap");
        assertTrue(p1.board.hasSwapPowerup);
        
        logic.softDropPiece1(p1);
        
        assertFalse(p1.board.hasSwapPowerup, "Swap powerup should be consumed");
        assertSame(initialP1Grid, p2.board.grid, "P2 should have P1's old grid");
        assertSame(initialP2Grid, p1.board.grid, "P1 should have P2's old grid");
    }

    private void testPortal(boolean twoBlocksMode, boolean horizontalMode) {
        GameLogic logic = createLogic(twoBlocksMode, horizontalMode);
        PlayerState p1 = logic.p1;
        PlayerState p2 = logic.p2;

        alignPowerupWithPiece(p1, false, "portal");
        assertTrue(p1.board.hasPortal);

        Tetromino originalPiece = p1.activePiece;

        logic.softDropPiece1(p1);

        assertFalse(p1.board.hasPortal, "Portal powerup should be consumed");
        
        // When piece enters portal, it's forwarded to opponent's next queue
        // meaning p2.nextPiece should be of the same type
        assertEquals(originalPiece.type, p2.nextPiece.type, "Opponent's next piece should be the forwarded piece");
    }

    // Now test second piece interactions for Double Block modes
    private void runTestsForSecondPiece(boolean twoBlocksMode, boolean horizontalMode) {
        if (!twoBlocksMode) return;

        GameLogic logic = createLogic(twoBlocksMode, horizontalMode);
        PlayerState p1 = logic.p1;
        
        // Test Swap with second piece
        String[][] initialP2Grid = logic.p2.board.grid;
        alignPowerupWithPiece(p1, true, "swap");
        assertTrue(p1.board.hasSwapPowerup);
        
        logic.softDropPiece2(p1);
        
        assertFalse(p1.board.hasSwapPowerup, "Swap powerup should be consumed by second piece");
        assertSame(initialP2Grid, p1.board.grid, "P1 should have P2's old grid");
    }

    @Test
    @DisplayName("Local Mode (Single Block, Vertical)")
    void testLocalMode() {
        runTestsForModes(false, false);
    }

    @Test
    @DisplayName("LAN Mode (Single Block, Vertical - logic identical to Local)")
    void testLANMode() {
        // LAN Mode logically uses the same backend game state for powerups
        runTestsForModes(false, false);
    }

    @Test
    @DisplayName("Local Double Block Mode")
    void testLocalDoubleBlockMode() {
        runTestsForModes(true, false);
        runTestsForSecondPiece(true, false);
    }

    @Test
    @DisplayName("Horizontal Mode")
    void testHorizontalMode() {
        runTestsForModes(false, true);
    }

    @Test
    @DisplayName("Double Block with Horizontal Mode")
    void testDoubleBlockHorizontalMode() {
        runTestsForModes(true, true);
        runTestsForSecondPiece(true, true);
    }
}
