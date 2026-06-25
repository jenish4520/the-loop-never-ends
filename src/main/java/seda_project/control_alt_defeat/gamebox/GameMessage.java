package seda_project.control_alt_defeat.gamebox;

import java.io.Serializable;

public class GameMessage implements Serializable {
    private final MessageType type;
    private GameState gameState;
    private GameConfig config;
    private int cardIndex = -1;
    private int playerNumber = -1;
    private String message, playerName, p1Name, p2Name;

    // extra fields used only when the message carries a chess move or action
    private int chessFromQ = -999;
    private int chessFromR = -999;
    private int chessToQ = -999;
    private int chessToR = -999;
    private String chessAction;

    public GameMessage(MessageType type) { this.type = type; }

    public static GameMessage joinRequest(String name) { GameMessage m = new GameMessage(MessageType.JOIN_REQUEST); m.playerName = (name == null || name.isBlank()) ? "Player 2" : name; return m; }
    public static GameMessage joinAccepted(int num) { GameMessage m = new GameMessage(MessageType.JOIN_ACCEPTED); m.playerNumber = num; return m; }
    public static GameMessage gameStart(GameState s, GameConfig c, String p1, String p2) { GameMessage m = new GameMessage(MessageType.GAME_START); m.gameState = s; m.config = c; m.p1Name = p1; m.p2Name = p2; return m; }
    public static GameMessage cardClick(int idx) { GameMessage m = new GameMessage(MessageType.CARD_CLICK); m.cardIndex = idx; return m; }
    public static GameMessage stateUpdate(GameState s) { GameMessage m = new GameMessage(MessageType.STATE_UPDATE); m.gameState = s; return m; }
    public static GameMessage gameEnd(GameState s) { GameMessage m = new GameMessage(MessageType.GAME_END); m.gameState = s; return m; }
    public static GameMessage restartRequest() { return new GameMessage(MessageType.RESTART_REQUEST); }
    public static GameMessage restartConfirmed(GameState s, GameConfig c, String p1, String p2) { GameMessage m = new GameMessage(MessageType.RESTART_CONFIRMED); m.gameState = s; m.config = c; m.p1Name = p1; m.p2Name = p2; return m; }
    public static GameMessage error(String err) { GameMessage m = new GameMessage(MessageType.ERROR); m.message = err; return m; }
    public static GameMessage heartbeat() { return new GameMessage(MessageType.HEARTBEAT); }

    // Chess factories
    public static GameMessage chessMove(int fromQ, int fromR, int toQ, int toR) {
        GameMessage m = new GameMessage(MessageType.CHESS_MOVE);
        m.chessFromQ = fromQ;
        m.chessFromR = fromR;
        m.chessToQ = toQ;
        m.chessToR = toR;
        return m;
    }

    public static GameMessage chessAction(String action) {
        GameMessage m = new GameMessage(MessageType.CHESS_ACTION);
        m.chessAction = action;
        return m;
    }

    public MessageType getType() { return type; }
    public GameState getGameState() { return gameState; }
    public GameConfig getConfig() { return config; }
    public int getCardIndex() { return cardIndex; }
    public int getPlayerNumber() { return playerNumber; }
    public String getMessage() { return message; }
    public String getPlayerName() { return playerName; }
    public String getP1Name() { return p1Name; }
    public String getP2Name() { return p2Name; }

    // Chess getters
    public int getChessFromQ() { return chessFromQ; }
    public int getChessFromR() { return chessFromR; }
    public int getChessToQ() { return chessToQ; }
    public int getChessToR() { return chessToR; }
    public String getChessAction() { return chessAction; }
}
