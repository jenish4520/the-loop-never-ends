package seda_project.control_alt_defeat.tetris;

import java.io.Serializable;

public class TetrisMessage implements Serializable {
    public enum Type {
        STATE_UPDATE,
        INPUT_LEFT, INPUT_RIGHT, INPUT_SOFT_DROP, INPUT_HARD_DROP, INPUT_ROTATE_CW, INPUT_ROTATE_CCW,
        RESTART_REQUEST, PLAYER_NAME
    }

    public Type type;
    public PlayerState p1;
    public PlayerState p2;
    public String playerName; // for initial handshake

    public TetrisMessage(Type type) {
        this.type = type;
    }
}
