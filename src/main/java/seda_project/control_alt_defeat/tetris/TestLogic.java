package seda_project.control_alt_defeat.tetris;

public class TestLogic {
    public static void main(String[] args) {
        GameLogic logic = new GameLogic("P1", "P2", 1);
        logic.twoBlocksMode = true;
        logic.initModes();

        PlayerState p = logic.p1;
        Tetromino piece2 = p.activePiece2;
        System.out.println("Piece 2 created at x: " + piece2.x + ", y: " + piece2.y);

        p.board.hasSwapPowerup = true;
        p.board.swapX = piece2.x;
        p.board.swapY = piece2.y + 1;

        System.out.println("Swap powerup spawned at x: " + p.board.swapX + ", y: " + p.board.swapY);

        System.out.println("Simulating fall...");
        try {
            java.lang.reflect.Method updateFallingPiece = GameLogic.class.getDeclaredMethod("updateFallingPiece", PlayerState.class, boolean.class, long.class);
            updateFallingPiece.setAccessible(true);
            
            p.lastFallTime2 = 0;
            updateFallingPiece.invoke(logic, p, true, 2000L);

            System.out.println("After fall, piece 2 y: " + p.activePiece2.y);
            System.out.println("Has swap powerup: " + p.board.hasSwapPowerup);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
