package seda_project.control_alt_defeat.tetris;

public class WallKicks {

    // Kicks are stored as [fromState][toState][kickIndex][x, y]
    // states: 0=0, 1=R, 2=2, 3=L
    
    public static final int[][][] JLSTZ_CW = {
        // 0 -> R (0->1)
        {{0,0}, {-1,0}, {-1,1}, {0,-2}, {-1,-2}},
        // R -> 2 (1->2)
        {{0,0}, {1,0}, {1,-1}, {0,2}, {1,2}},
        // 2 -> L (2->3)
        {{0,0}, {1,0}, {1,1}, {0,-2}, {1,-2}},
        // L -> 0 (3->0)
        {{0,0}, {-1,0}, {-1,-1}, {0,2}, {-1,2}}
    };

    public static final int[][][] I_CW = {
        // 0 -> R (0->1)
        {{0,0}, {-2,0}, {1,0}, {-2,-1}, {1,2}},
        // R -> 2 (1->2)
        {{0,0}, {-1,0}, {2,0}, {-1,2}, {2,-1}},
        // 2 -> L (2->3)
        {{0,0}, {2,0}, {-1,0}, {2,1}, {-1,-2}},
        // L -> 0 (3->0)
        {{0,0}, {1,0}, {-2,0}, {1,-2}, {-2,1}}
    };

    public static int[][] getKicks(Tetromino.Type type, int fromState, int toState, boolean isCW) {
        if (type == Tetromino.Type.O) {
            return new int[][]{{0,0}};
        }
        
        int[][][] baseKicks = (type == Tetromino.Type.I) ? I_CW : JLSTZ_CW;
        
        if (isCW) {
            return baseKicks[fromState];
        } else {
            // CCW: reverse the CW offsets in reverse order.
            // A CCW rotation is the reverse of a CW rotation from toState to fromState.
            // Wait, standard SRS:
            // CCW fromState -> toState is the negative of CW toState -> fromState.
            // Let's use the standard "negative of reverse":
            int[][] cwKicks = baseKicks[toState];
            int[][] ccwKicks = new int[5][2];
            for (int i = 0; i < 5; i++) {
                ccwKicks[i][0] = -cwKicks[i][0];
                ccwKicks[i][1] = -cwKicks[i][1];
            }
            return ccwKicks;
        }
    }
}
