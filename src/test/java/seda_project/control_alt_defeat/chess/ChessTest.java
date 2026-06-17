package seda_project.control_alt_defeat.chess;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import seda_project.control_alt_defeat.chess.ChessBoard.PieceColor;
import seda_project.control_alt_defeat.chess.ChessBoard.PieceType;

public class ChessTest {

    @Test
    public void testInitialSetup() {
        ChessBoard board = new ChessBoard();

        // Check starting turn is White
        assertEquals(PieceColor.WHITE, board.getCurrentTurn());

        // Count total pieces on board
        int whiteCount = 0;
        int blackCount = 0;
        int whiteBishops = 0;
        int blackBishops = 0;
        int whitePawns = 0;
        int blackPawns = 0;

        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (board.isValidCoord(q, r)) {
                    Piece p = board.getPiece(q, r);
                    if (p != null) {
                        if (p.isWhite()) {
                            whiteCount++;
                            if (p.getType() == PieceType.BISHOP) whiteBishops++;
                            if (p.getType() == PieceType.PAWN) whitePawns++;
                        } else {
                            blackCount++;
                            if (p.getType() == PieceType.BISHOP) blackBishops++;
                            if (p.getType() == PieceType.PAWN) blackPawns++;
                        }
                    }
                }
            }
        }

        // Each player must have 18 pieces: 1 King, 1 Queen, 3 Bishops, 2 Knights, 2 Rooks, 9 Pawns
        assertEquals(18, whiteCount);
        assertEquals(18, blackCount);
        assertEquals(3, whiteBishops);
        assertEquals(3, blackBishops);
        assertEquals(9, whitePawns);
        assertEquals(9, blackPawns);

        // Verify key piece starting coordinates
        // White King on g1 (q=1, r=-5)
        Piece whiteKing = board.getPiece(1, -5);
        assertNotNull(whiteKing);
        assertEquals(PieceType.KING, whiteKing.getType());
        assertEquals(PieceColor.WHITE, whiteKing.getColor());

        // Black King on g10 (q=1, r=4)
        Piece blackKing = board.getPiece(1, 4);
        assertNotNull(blackKing);
        assertEquals(PieceType.KING, blackKing.getType());
        assertEquals(PieceColor.BLACK, blackKing.getColor());
    }

    @Test
    public void testPawnMoves() {
        ChessBoard board = new ChessBoard();

        // White pawn starts on f5 (q=0, r=-1)
        Piece p = board.getPiece(0, -1);
        assertNotNull(p);
        assertEquals(PieceType.PAWN, p.getType());

        // Move 1 step forward (f5 -> f6 or q=0, r=-1 -> q=0, r=0) is valid
        assertTrue(board.isValidMove(0, -1, 0, 0));

        // Attempt 2-step move to f7 (q=0, r=1) which is occupied by Black pawn: should be invalid
        assertFalse(board.isValidMove(0, -1, 0, 1));

        // If we clear f7 (q=0, r=1), it should become valid
        board.setPiece(0, 1, null);
        assertTrue(board.isValidMove(0, -1, 0, 1));
    }

    @Test
    public void testRookOrthogonalMoves() {
        ChessBoard board = new ChessBoard();

        // Clear pawn in front of White Rook on c1 (q=-3, r=-2)
        // Pawn in front is on c2 (q=-3, r=-1)
        board.setPiece(-3, -1, null);

        // Move Rook forward (c1 -> c3 or q=-3, r=-2 -> q=-3, r=0)
        assertTrue(board.isValidMove(-3, -2, -3, 0));
        assertTrue(board.makeMove(-3, -2, -3, 0));
        assertEquals(PieceType.ROOK, board.getPiece(-3, 0).getType());
    }

    @Test
    public void testBishopDiagonalMoves() {
        ChessBoard board = new ChessBoard();

        // Bishops are at f1 (0, -5), f2 (0, -4), f3 (0, -3)
        // Clear pawn on d3 (-2, -1)
        board.setPiece(-2, -1, null);

        // Bishop on f2 (0, -4) moves to e3 (-1, -2) which is diagonal (dq=-1, dr=2)
        assertTrue(board.isValidMove(0, -4, -1, -2));
    }

    @Test
    public void testKnightJumps() {
        ChessBoard board = new ChessBoard();

        // Knight is at d1 (q=-2, r=-3)
        // Knight jumps to f4 (q=0, r=-2) -> dq = 2, dr = 1
        assertTrue(board.isValidMove(-2, -3, 0, -2));
    }

    @Test
    public void testCheckAndCheckmateHex() {
        ChessBoard board = new ChessBoard();

        // Clear all pieces
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (board.isValidCoord(q, r)) {
                    board.setPiece(q, r, null);
                }
            }
        }

        // Set up White King in corner a6 (q=-5, r=5)
        board.setPiece(-5, 5, new Piece(PieceType.KING, PieceColor.WHITE));
        
        // Place Black Queen at b6 (q=-4, r=4) and Black Bishop protecting Queen at c8 (q=-3, r=5)
        board.setPiece(-4, 4, new Piece(PieceType.QUEEN, PieceColor.BLACK));
        board.setPiece(-3, 5, new Piece(PieceType.BISHOP, PieceColor.BLACK));

        board.setCurrentTurn(PieceColor.WHITE);

        // King is in check by Queen
        assertTrue(board.isInCheck(PieceColor.WHITE));

        // King cannot move anywhere (Checkmate)
        assertFalse(board.hasValidMoves(PieceColor.WHITE));
    }

    @Test
    public void testBlackPawnInitialSetupAndMoves() {
        ChessBoard board = new ChessBoard();

        // Coordinates of 9 Black pawns on rank 7:
        // b7, c7, d7, e7, f7, g7, h7, i7, k7
        String[] coords = {"b7", "c7", "d7", "e7", "f7", "g7", "h7", "i7", "k7"};
        for (String c : coords) {
            int[] qr = ChessBoard.parseAlgebraic(c);
            assertNotNull(qr);
            Piece p = board.getPiece(qr[0], qr[1]);
            assertNotNull(p, "No piece at " + c);
            assertEquals(PieceType.PAWN, p.getType());
            assertEquals(PieceColor.BLACK, p.getColor());
        }

        // Set turn to BLACK to test Black moves
        board.setCurrentTurn(PieceColor.BLACK);

        // Black pawn starts on f7 (q=0, r=1)
        // 1 step forward (f7 -> f6 or q=0, r=1 -> q=0, r=0) is valid
        assertTrue(board.isValidMove(0, 1, 0, 0));

        // 2 steps forward (f7 -> f5 or q=0, r=1 -> q=0, r=-1)
        // Note: f5 (0, -1) is occupied by White pawn initially, so it should be invalid
        assertFalse(board.isValidMove(0, 1, 0, -1));

        // If we clear f5 (q=0, r=-1), it should be valid
        board.setPiece(0, -1, null);
        assertTrue(board.isValidMove(0, 1, 0, -1));
    }

    @Test
    public void testPositionValidation() {
        ChessBoard board = new ChessBoard();
        
        // Standard setup is valid
        assertNull(board.validatePosition(PieceColor.WHITE));
        assertNull(board.validatePosition(PieceColor.BLACK));

        // Missing King
        board.setPiece(1, -5, null); // Remove White King (g1)
        assertNotNull(board.validatePosition(PieceColor.WHITE));

        // Restore King, place 10 pawns
        board.resetBoard();
        board.setPiece(-5, 0, new Piece(PieceType.PAWN, PieceColor.WHITE)); // Place 10th pawn on a6
        assertNotNull(board.validatePosition(PieceColor.WHITE));

        // Adjacent Kings
        board.resetBoard();
        // Clear board first
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (board.isValidCoord(q, r)) {
                    board.setPiece(q, r, null);
                }
            }
        }
        board.setPiece(0, 0, new Piece(PieceType.KING, PieceColor.WHITE)); // King on f6
        board.setPiece(0, 1, new Piece(PieceType.KING, PieceColor.BLACK)); // King on f7 (adjacent)
        assertNotNull(board.validatePosition(PieceColor.WHITE));

        // Opponent king is in check
        board.resetBoard();
        // Clear board
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (board.isValidCoord(q, r)) {
                    board.setPiece(q, r, null);
                }
            }
        }
        board.setPiece(0, 0, new Piece(PieceType.KING, PieceColor.WHITE)); // f6
        board.setPiece(0, 3, new Piece(PieceType.KING, PieceColor.BLACK)); // f9
        // Place White Queen checking Black King at f9 (f7 is (0, 1))
        board.setPiece(0, 1, new Piece(PieceType.QUEEN, PieceColor.WHITE)); 
        assertNotNull(board.validatePosition(PieceColor.WHITE));
        assertNull(board.validatePosition(PieceColor.BLACK));
    }

    @Test
    public void testBoardSerialization() {
        ChessBoard board = new ChessBoard();
        String serialized = board.serializeBoard();
        
        ChessBoard newBoard = new ChessBoard();
        newBoard.deserializeBoard(serialized);
        
        assertEquals(board.getCurrentTurn(), newBoard.getCurrentTurn());
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (board.isValidCoord(q, r)) {
                    Piece p1 = board.getPiece(q, r);
                    Piece p2 = newBoard.getPiece(q, r);
                    if (p1 == null) {
                        assertNull(p2);
                    } else {
                        assertNotNull(p2);
                        assertEquals(p1.getType(), p2.getType());
                        assertEquals(p1.getColor(), p2.getColor());
                    }
                }
            }
        }
    }

    @Test
    public void testBotLookahead() {
        ChessBoard board = new ChessBoard();
        // Clear board
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (board.isValidCoord(q, r)) {
                    board.setPiece(q, r, null);
                }
            }
        }

        // Setup checkmate scenario: White King in corner a6, Black Bishop protecting b6, Black Queen can move to b6
        board.setPiece(-5, 5, new Piece(PieceType.KING, PieceColor.WHITE)); // White King on a6
        board.setPiece(-3, 5, new Piece(PieceType.BISHOP, PieceColor.BLACK)); // Black Bishop on c8
        board.setPiece(-4, 3, new Piece(PieceType.QUEEN, PieceColor.BLACK)); // Black Queen on b5
        
        board.setCurrentTurn(PieceColor.BLACK);
        
        // Find best move for Black bot
        Move botMove = board.getBotMove(PieceColor.BLACK);
        assertNotNull(botMove);
        // The bot should choose to move Queen from b5 (-4, 3) to b6 (-4, 4) delivering mate in one!
        assertEquals(-4, botMove.fromQ);
        assertEquals(3, botMove.fromR);
        assertEquals(-4, botMove.toQ);
        assertEquals(4, botMove.toR);
    }

    @Test
    public void testDrawCriteriaRules() {
        ChessBoard board = new ChessBoard();

        // 1. Insufficient Material Test
        // Clear all pieces
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (board.isValidCoord(q, r)) {
                    board.setPiece(q, r, null);
                }
            }
        }
        // Place just two Kings
        board.setPiece(0, 0, new Piece(PieceType.KING, PieceColor.WHITE));
        board.setPiece(0, 3, new Piece(PieceType.KING, PieceColor.BLACK));
        assertEquals("insufficient material", board.checkDrawCriteria());

        // Add 1 White Bishop (King + Bishop vs King -> draw)
        board.setPiece(0, 1, new Piece(PieceType.BISHOP, PieceColor.WHITE));
        assertEquals("insufficient material", board.checkDrawCriteria());

        // Add 1 Black Pawn (No longer insufficient material)
        board.setPiece(0, 2, new Piece(PieceType.PAWN, PieceColor.BLACK));
        assertNull(board.checkDrawCriteria());

        // 2. 50-Move Rule Test
        board.resetBoard();
        // Move Rook back and forth 50 times (100 plies)
        // Clear pawn in front of White Rook on c1 (c2 is q=-3, r=-1)
        board.setPiece(-3, -1, null);
        // Clear pawn in front of Black Rook on c8 (c7 is q=-3, r=4)
        board.setPiece(-3, 4, null);

        for (int i = 0; i < 25; i++) {
            board.setCurrentTurn(PieceColor.WHITE);
            assertTrue(board.makeMove(-3, -2, -3, 0)); // c1 to c3
            board.setCurrentTurn(PieceColor.BLACK);
            assertTrue(board.makeMove(-3, 5, -3, 3));  // c8 to c6

            board.setCurrentTurn(PieceColor.WHITE);
            assertTrue(board.makeMove(-3, 0, -3, -2)); // c3 to c1
            board.setCurrentTurn(PieceColor.BLACK);
            assertTrue(board.makeMove(-3, 3, -3, 5));  // c6 to c8
        }
        assertEquals("50-move rule", board.checkDrawCriteria());

        // 3. Threefold Repetition Test
        board.resetBoard();
        // Clear pawn in front of White Rook on c1 (c2 is q=-3, r=-1)
        board.setPiece(-3, -1, null);
        // Clear pawn in front of Black Rook on c8 (c7 is q=-3, r=4)
        board.setPiece(-3, 4, null);
        board.deserializeBoard(board.serializeBoard());

        for (int i = 0; i < 2; i++) {
            board.setCurrentTurn(PieceColor.WHITE);
            assertTrue(board.makeMove(-3, -2, -3, 0)); // c1 to c3
            board.setCurrentTurn(PieceColor.BLACK);
            assertTrue(board.makeMove(-3, 5, -3, 3));  // c8 to c6

            board.setCurrentTurn(PieceColor.WHITE);
            assertTrue(board.makeMove(-3, 0, -3, -2)); // c3 to c1
            board.setCurrentTurn(PieceColor.BLACK);
            assertTrue(board.makeMove(-3, 3, -3, 5));  // c6 to c8
        }
        assertEquals("threefold repetition", board.checkDrawCriteria());
    }

    @Test
    public void testEnPassantCapture() {
        ChessBoard board = new ChessBoard();
        // Clear board
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                if (board.isValidCoord(q, r)) {
                    board.setPiece(q, r, null);
                }
            }
        }
        // Place Kings to make validation happy
        board.setPiece(0, -5, new Piece(PieceType.KING, PieceColor.WHITE)); // f1
        board.setPiece(0, 5, new Piece(PieceType.KING, PieceColor.BLACK)); // f11

        // Place White Pawn on starting position f5 (0, -1)
        board.setPiece(0, -1, new Piece(PieceType.PAWN, PieceColor.WHITE));
        // Place Black Pawn on e6 (-1, 1)
        board.setPiece(-1, 1, new Piece(PieceType.PAWN, PieceColor.BLACK));

        // White makes a double step: f5 to f7 (0, -1 to 0, 1)
        board.setCurrentTurn(PieceColor.WHITE);
        assertTrue(board.makeMove(0, -1, 0, 1));

        // Verify en passant target is set to f6 (0, 0)
        int[] ep = board.getEnPassantTarget();
        assertNotNull(ep);
        assertEquals(0, ep[0]);
        assertEquals(0, ep[1]);

        // Black plays en passant: e6 captures on f6 (-1, 1 to 0, 0)
        board.setCurrentTurn(PieceColor.BLACK);
        assertTrue(board.isValidMove(-1, 1, 0, 0));
        assertTrue(board.makeMove(-1, 1, 0, 0));

        // Verify White Pawn at f7 (0, 1) is captured and set to null
        assertNull(board.getPiece(0, 1));
        // Verify Black Pawn is now at f6 (0, 0)
        Piece capturedAt = board.getPiece(0, 0);
        assertNotNull(capturedAt);
        assertEquals(PieceType.PAWN, capturedAt.getType());
        assertEquals(PieceColor.BLACK, capturedAt.getColor());
    }
}
