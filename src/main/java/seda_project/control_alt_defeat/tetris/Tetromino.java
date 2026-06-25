package seda_project.control_alt_defeat.tetris;

import javafx.scene.paint.Color;
import java.io.Serializable;

public class Tetromino implements Serializable {
    public enum Type {
        I(Color.CYAN),
        O(Color.YELLOW),
        T(Color.PURPLE),
        S(Color.GREEN),
        Z(Color.RED),
        J(Color.BLUE),
        L(Color.ORANGE),
        RADIUS_BOMB(Color.web("#FF4757")),
        COLUMN_BOMB(Color.web("#00D2FF")),
        CUSTOM(Color.WHITE);
        
        public final String colorHex;
        Type(Color color) {
            this.colorHex = String.format("#%02X%02X%02X",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255));
        }
        
        Type(String hex) {
            this.colorHex = hex;
        }
    }

    public Type type;
    public String colorHex;
    // rotation states: 0=spawn, 1=right, 2=flip, 3=left
    public int[][][] shapes; 
    public int state = 0;
    public int x, y;

    public Tetromino(Type type, int[][][] shapes) {
        this.type = type;
        this.colorHex = type.colorHex;
        this.shapes = shapes;
    }
    
    public Tetromino(Type type, String customHex, int[][][] shapes) {
        this.type = type;
        this.colorHex = customHex;
        this.shapes = shapes;
    }

    public Tetromino copy() {
        Tetromino t = new Tetromino(type, colorHex, shapes);
        t.state = this.state;
        t.x = this.x;
        t.y = this.y;
        return t;
    }

    public Tetromino queuedCopy() {
        Tetromino t = copy();
        t.state = 0;
        t.x = 0;
        t.y = 0;
        return t;
    }

    public int[][] getShape() {
        return shapes[state];
    }

    public void rotateCW() {
        if (type == Type.O || isSpecial()) return;
        state = (state + 1) % 4;
    }

    public void rotateCCW() {
        if (type == Type.O || isSpecial()) return;
        state = (state + 3) % 4;
    }

    public boolean isSpecial() {
        return type == Type.RADIUS_BOMB || type == Type.COLUMN_BOMB;
    }

    public static Tetromino create(Type type) {
        int[][][] shapes;
        switch (type) {
            case I:
                shapes = new int[][][]{
                    {{0,0,0,0}, {1,1,1,1}, {0,0,0,0}, {0,0,0,0}},
                    {{0,0,1,0}, {0,0,1,0}, {0,0,1,0}, {0,0,1,0}},
                    {{0,0,0,0}, {0,0,0,0}, {1,1,1,1}, {0,0,0,0}},
                    {{0,1,0,0}, {0,1,0,0}, {0,1,0,0}, {0,1,0,0}}
                };
                break;
            case O:
                shapes = new int[][][]{
                    {{1,1}, {1,1}},
                    {{1,1}, {1,1}},
                    {{1,1}, {1,1}},
                    {{1,1}, {1,1}}
                };
                break;
            case T:
                shapes = new int[][][]{
                    {{0,1,0}, {1,1,1}, {0,0,0}},
                    {{0,1,0}, {0,1,1}, {0,1,0}},
                    {{0,0,0}, {1,1,1}, {0,1,0}},
                    {{0,1,0}, {1,1,0}, {0,1,0}}
                };
                break;
            case S:
                shapes = new int[][][]{
                    {{0,1,1}, {1,1,0}, {0,0,0}},
                    {{0,1,0}, {0,1,1}, {0,0,1}},
                    {{0,0,0}, {0,1,1}, {1,1,0}},
                    {{1,0,0}, {1,1,0}, {0,1,0}}
                };
                break;
            case Z:
                shapes = new int[][][]{
                    {{1,1,0}, {0,1,1}, {0,0,0}},
                    {{0,0,1}, {0,1,1}, {0,1,0}},
                    {{0,0,0}, {1,1,0}, {0,1,1}},
                    {{0,1,0}, {1,1,0}, {1,0,0}}
                };
                break;
            case J:
                shapes = new int[][][]{
                    {{1,0,0}, {1,1,1}, {0,0,0}},
                    {{0,1,1}, {0,1,0}, {0,1,0}},
                    {{0,0,0}, {1,1,1}, {0,0,1}},
                    {{0,1,0}, {0,1,0}, {1,1,0}}
                };
                break;
            case L:
                shapes = new int[][][]{
                    {{0,0,1}, {1,1,1}, {0,0,0}},
                    {{0,1,0}, {0,1,0}, {0,1,1}},
                    {{0,0,0}, {1,1,1}, {1,0,0}},
                    {{1,1,0}, {0,1,0}, {0,1,0}}
                };
                break;
            case RADIUS_BOMB:
            case COLUMN_BOMB:
                shapes = new int[][][]{
                    {{1}},
                    {{1}},
                    {{1}},
                    {{1}}
                };
                break;
            default:
                shapes = new int[][][]{{{1}}};
        }
        return new Tetromino(type, shapes);
    }
}
