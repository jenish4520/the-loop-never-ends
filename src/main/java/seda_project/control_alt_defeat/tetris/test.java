package seda_project.control_alt_defeat.tetris;

public class test {
    public static void main(String[] args) {
        GameLogic logic = new GameLogic("P1", "P2");
        PlayerState p = logic.p1;
        p.board.hasSpeedUpPowerup = true;
        // Find the first solid block in the active piece
        int px = 0, py = 0;
        int[][] shape = p.activePiece.getShape();
        for(int r=0; r<shape.length; r++) {
            for(int c=0; c<shape[r].length; c++) {
                if(shape[r][c] == 1) {
                    px = c; py = r; break;
                }
            }
            if(shape[py][px] == 1) break;
        }

        p.board.speedUpX = p.activePiece.x + px;
        p.board.speedUpY = p.activePiece.y + py + 1; // 1 cell below
        System.out.println("has powerup initially: " + p.board.hasSpeedUpPowerup);
        
        // Soft drop
        logic.softDrop(p);
        
        System.out.println("has powerup after soft drop: " + p.board.hasSpeedUpPowerup);
        System.out.println("piece y: " + p.activePiece.y);
    }
}
