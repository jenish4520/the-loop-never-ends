package seda_project.control_alt_defeat.gamebox;

// Message types.
public enum MessageType {
    // Join request.
    JOIN_REQUEST,

    // Join accepted.
    JOIN_ACCEPTED,

    // Game started.
    GAME_START,

    // Card clicked.
    CARD_CLICK,

    // State updated.
    STATE_UPDATE,

    // Game ended.
    GAME_END,

    // Restart requested.
    RESTART_REQUEST,

    // Restart confirmed.
    RESTART_CONFIRMED,

    // Error.
    ERROR,

    // Heartbeat.
    HEARTBEAT
}
