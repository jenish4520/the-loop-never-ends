package seda_project.control_alt_defeat.gamebox;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameMessageTest {

    @Test
    void testJoinRequest() {
        GameMessage msg = GameMessage.joinRequest("TestPlayer");
        assertEquals(MessageType.JOIN_REQUEST, msg.getType());
        assertEquals("TestPlayer", msg.getPlayerName());
    }

    @Test
    void testJoinAccepted() {
        GameMessage msg = GameMessage.joinAccepted(2);
        assertEquals(MessageType.JOIN_ACCEPTED, msg.getType());
        assertEquals(2, msg.getPlayerNumber());
    }

    @Test
    void testCardClick() {
        GameMessage msg = GameMessage.cardClick(5);
        assertEquals(MessageType.CARD_CLICK, msg.getType());
        assertEquals(5, msg.getCardIndex());
    }

    @Test
    void testStateUpdate() {
        GameState state = createTestState();
        GameMessage msg = GameMessage.stateUpdate(state);
        assertEquals(MessageType.STATE_UPDATE, msg.getType());
        assertNotNull(msg.getGameState());
    }

    @Test
    void testGameStart() {
        GameState state = createTestState();
        GameConfig config = new GameConfig(2, 16);
        GameMessage msg = GameMessage.gameStart(state, config, "Alice", "Bob");
        assertEquals(MessageType.GAME_START, msg.getType());
        assertNotNull(msg.getGameState());
        assertNotNull(msg.getConfig());
        assertEquals("Alice", msg.getP1Name());
        assertEquals("Bob", msg.getP2Name());
    }

    @Test
    void testError() {
        GameMessage msg = GameMessage.error("Test error");
        assertEquals(MessageType.ERROR, msg.getType());
        assertEquals("Test error", msg.getMessage());
    }

    @Test
    void testHeartbeat() {
        GameMessage msg = GameMessage.heartbeat();
        assertEquals(MessageType.HEARTBEAT, msg.getType());
    }

    @Test
    void testRestartRequest() {
        GameMessage msg = GameMessage.restartRequest();
        assertEquals(MessageType.RESTART_REQUEST, msg.getType());
    }

    @Test
    void testGameEnd() {
        GameState state = createTestState();
        GameMessage msg = GameMessage.gameEnd(state);
        assertEquals(MessageType.GAME_END, msg.getType());
        assertNotNull(msg.getGameState());
    }

    @Test
    void testSerialization() throws Exception {
        GameState state = createTestState();
        GameMessage original = GameMessage.gameStart(state, new GameConfig(2, 16), "P1", "P2");

        // Serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.flush();

        // Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        GameMessage deserialized = (GameMessage) ois.readObject();

        assertEquals(original.getType(), deserialized.getType());
        assertNotNull(deserialized.getGameState());
        assertNotNull(deserialized.getConfig());
        assertEquals(original.getGameState().getDeckSize(), deserialized.getGameState().getDeckSize());
    }

    @Test
    void testAllMessageTypesSerialization() throws Exception {
        List<GameMessage> messages = List.of(
            GameMessage.joinRequest("TestPlayer"),
            GameMessage.joinAccepted(2),
            GameMessage.cardClick(5),
            GameMessage.stateUpdate(createTestState()),
            GameMessage.gameStart(createTestState(), new GameConfig(2, 16), "P1", "P2"),
            GameMessage.gameEnd(createTestState()),
            GameMessage.restartRequest(),
            GameMessage.restartConfirmed(createTestState(), new GameConfig(2, 16), "P1", "P2"),
            GameMessage.error("test"),
            GameMessage.heartbeat()
        );

        for (GameMessage msg : messages) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(msg);
            oos.flush();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            GameMessage deserialized = (GameMessage) ois.readObject();

            assertEquals(msg.getType(), deserialized.getType(),
                "Serialization failed for " + msg.getType());
        }
    }

    private GameState createTestState() {
        List<Card> cards = List.of(
            new Card(0, "A"), new Card(1, "A"),
            new Card(2, "B"), new Card(3, "B")
        );
        return new GameState(cards, 0, 0, 1,
            GamePhase.PLAYING, List.of(), 2, 4, "test");
    }
}
