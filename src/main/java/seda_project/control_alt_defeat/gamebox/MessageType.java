package seda_project.control_alt_defeat.gamebox;

// Identifiers for the various network packets sent during a LAN game.
public enum MessageType {
    // Sent by client to ask to join.
    JOIN_REQUEST,

    // Sent by host to approve a join.
    JOIN_ACCEPTED,

    // Sent by host to begin the match.
    GAME_START,

    // Sent by client to report a move.
    CARD_CLICK,

    // Sent by host to synchronize the board.
    STATE_UPDATE,

    // Indicates someone has won.
    GAME_END,

    // Request a match restart.
    RESTART_REQUEST,

    // Restart confirmed.
    RESTART_CONFIRMED,

    // Error.
    ERROR,

    // Heartbeat.
    HEARTBEAT,

    // Chess moves and actions
    CHESS_MOVE,
    CHESS_ACTION
}
