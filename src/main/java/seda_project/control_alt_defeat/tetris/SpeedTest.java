package seda_project.control_alt_defeat.tetris;

public class SpeedTest {
    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("REAL WORLD TEST: PlayerState Speed Scaling");
        System.out.println("=========================================");
        
        // Create a test player starting at default Speed Level 3
        PlayerState p = new PlayerState(1, "Tester", 3);
        
        for (int i = 0; i <= 20; i++) {
            p.linesCleared = i;
            int delay = p.getFallDelay();
            System.out.printf("Lines Cleared: %2d | Fall Delay: %3d ms%n", i, delay);
        }
        System.out.println("=========================================");
    }
}
