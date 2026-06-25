package seda_project.control_alt_defeat.gamebox;

// Defines the high-level states the game loop cycles through.
public enum GamePhase {
    // Waiting for the game to officially start.
    LOBBY,

    // Active turn, players are making choices.
    PLAYING,

    // Brief pause to display mismatched results before flipping back.
    RESOLVING,

    // Match concluded, waiting to return to menu or restart.
    GAME_OVER
}
