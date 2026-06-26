    
    Hub --> ExitApp((Close Application))
```
# Retro Game Box — Complete Interview Preparation Guide

---

## 1. Project At a Glance

| Item | Detail |
|------|--------|
| **Project Name** | Retro Game Box (internal: `loop-never-ends`) |
| **Type** | Desktop JavaFX multi-game suite |
| **Language** | Java 21 |
| **Build Tool** | Maven 3.8+ |
| **JavaFX Version** | 21.0.1 |
| **Games Included** | Memory Card Game, Tetris, Hexagonal Chess |
| **Total Java Files** | 37 |
| **Total Lines of Code** | ~6,600 |
| **Network Protocol** | TCP (ObjectInputStream/ObjectOutputStream) + UDP (discovery) |
| **Main Entry Point** | `Launcher.java` → `GameHub.java` |

---

## 2. Package Structure

```
seda_project.control_alt_defeat
├── gamebox/           ← Memory game + shared networking
│   ├── Launcher.java
│   ├── GameHub.java
│   ├── GameBox.java
│   ├── MenuPanel.java
│   ├── GamePanel.java
│   ├── CardComponent.java
│   ├── GameLogic.java
│   ├── GameHost.java
│   ├── GameClient.java
│   ├── GameMessage.java
│   ├── MessageType.java
│   ├── GameState.java
│   ├── GameConfig.java
│   ├── GamePhase.java
│   ├── Card.java
│   └── MyController.java  (unused FXML stub)
├── tetris/            ← Tetris game
│   ├── TetrisApp.java
│   ├── TetrisPanel.java
│   ├── GameLogic.java
│   ├── Board.java
│   ├── Tetromino.java
│   ├── PlayerState.java
│   ├── TetrisHost.java
│   ├── TetrisClient.java
│   ├── TetrisMessage.java
│   ├── WallKicks.java
│   ├── CustomPieceDesigner.java
│   ├── SpeedTest.java
│   ├── TestLogic.java
│   └── test.java
└── chess/             ← Hexagonal Chess game
    ├── ChessApp.java
    ├── ChessBoard.java
    ├── ChessRules.java
    ├── ChessBot.java
    ├── HexCoord.java
    ├── Piece.java
    └── Move.java
```

---

## 3. Complete Application Flow

### 3.1 Startup
```
JVM → Launcher.main()
        └─ Application.launch() [JavaFX bootstrap]
              └─ Launcher.start(primaryStage)
                    ├─ Sets fullscreen mode
                    └─ new GameHub(stage).show()
                          └─ Renders 3 clickable game-selection cards
```

### 3.2 Memory Game Flow
```
GameHub → new GameBox(stage, hub).show()
  └─ GameBox.showMenu()
        └─ new MenuPanel(callbacks...) [shows mode/config screen]
              [User selects LOCAL / HOST / JOIN]
LOCAL path:
  MenuPanel → onLocalGame.accept(matchSize, deckSize)
    └─ GameBox.startLocalGame(matchSize, deckSize)
          ├─ new GameLogic().initializeGame(config)   [shuffle deck]
          ├─ new GamePanel(...)                         [build UI]
          └─ GameBox.updateDisplay(state)              [render board]
HOST path:
  MenuPanel → onHost.accept(matchSize, deckSize)
    └─ GameBox.startHostGame(matchSize, deckSize)
          ├─ new GameHost(onMessage, onLost)
          ├─ Background thread: gameHost.startAndWaitForClient(port)
          │     [blocks until client connects]
          │     [handshake: JOIN_REQUEST → JOIN_ACCEPTED]
          ├─ gameHost.sendMessage(GAME_START + serialized config)
          └─ Both sides render via updateDisplay()
JOIN path:
  MenuPanel → onJoin.accept(name, ip, port)
    └─ GameBox.startJoinGame(name, ip, port)
          ├─ new GameClient(onMessage, onLost)
          ├─ gameClient.connect(ip, port, name)
          │     [sends JOIN_REQUEST, receives JOIN_ACCEPTED]
          └─ Waits for GAME_START from host
Card Click (any mode):
  GamePanel.CardComponent onClick → GameBox.handleCardClick(index)
    ├─ if (isHost || localMode): GameLogic.handleCardClick(index, player)
    │     ├─ Card flips face-up
    │     ├─ If n cards selected → resolveAttempt()
    │     │     ├─ MATCH: mark cards as matched, add score, continue
    │     │     └─ NO MATCH: phase = RESOLVING (delay), then resolveMismatch()
    │     └─ broadcastState() → both clients re-render
    └─ if (isClient): gameClient.sendMessage(CARD_CLICK)
          └─ Host processes, broadcasts STATE_UPDATE
```

### 3.3 Tetris Game Flow
```
GameHub → new TetrisApp(stage, hub).show()
  └─ TetrisApp renders options screen (player names, speed, modes, LAN)
LOCAL path:
  TetrisApp → new GameLogic(p1Name, p2Name, speed).initModes()
    └─ new TetrisPanel(logic, onBack)
          ├─ .setupKeyEvents()   [register keyboard handlers]
          └─ .start()
                └─ Game loop thread (~60fps):
                      ├─ handleInput(now)   [DAS-based movement]
                      ├─ logic.update(now)  [gravity, locking, clears]
                      └─ Platform.runLater(render)
LAN HOST path:
  TetrisApp → new TetrisHost(onMsg, onDisconnect)
    ├─ UDP: broadcasts "TETRIS_HOST:name:port" for auto-discovery
    ├─ TCP: serverSocket.accept() → client connects
    ├─ Host drives game loop, sends TetrisMessage(STATE_UPDATE) every frame
    └─ Client sends INPUT_* messages back
LAN CLIENT path:
  TetrisApp → new TetrisClient(onMsg, onDisconnect)
    ├─ UDP: sends "TETRIS_DISCOVER" broadcast to find hosts
    ├─ TCP: connects to host IP:port
    ├─ Receives STATE_UPDATE → TetrisPanel.updateState(p1, p2)
    └─ Sends INPUT_LEFT/RIGHT/etc. to host
Game Loop (TetrisPanel.start):
  Thread loop every ~16ms:
    ├─ handleInput()     → calls logic.moveLeft/Right/softDrop
    ├─ logic.update()    → calls logic.applyGravity()
    │     ├─ If piece can fall: piece.y++
    │     └─ If piece can't fall: lock() → clearLines() → spawnNext()
    └─ render()          → Canvas drawing (board, ghost, pieces, HUD)
```

### 3.4 Chess Game Flow
```
GameHub → new ChessApp(stage, hub).show()
  └─ ChessApp.showModeScreen()  [4 mode cards]
LOCAL / VS BOT path:
  ChessApp.startLocalGame() / startBotGame()
    └─ new ChessBoard()   [sets up 91 hexagonal cells, places pieces]
    └─ showLocalGameScreen()
          └─ buildBoardPane(false)
                └─ squarePanes[q][r] wired to handleLocalSquareClick()
Click handling (local):
  handleLocalSquareClick(q, r)
    ├─ First click: select piece (if correct color)
    ├─ Second click: if valid move → chessBoard.makeMove(from, to)
    │     ├─ ChessRules.isValidPieceMoveBasic() checks piece movement
    │     ├─ Simulate move → check isInCheck()
    │     └─ If pawn reaches end → showPromotionDialog()
    └─ updateBoardDisplay() re-draws board
Bot move (after player's move):
  maybeTriggerBot()
    └─ bgExecutor.submit(() → {
          Thread.sleep(350)   [dramatic pause]
          ChessBot.getBotMove(board, BLACK)
            ├─ 1. Check for mate-in-one  → play it
            ├─ 2. Filter moves that allow opponent mate-in-one
            └─ 3. Score remaining moves by piece value → pick best
          chessBoard.makeMove(botMove)
          updateBoardDisplay()
       })
LAN HOST path:
  ChessApp.showHostScreen()
    └─ new GameHost(onNetMsg, onLost)
    └─ bgExecutor: gameHost.startAndWaitForClient(5555)
          [client connects → send "START:" + serialized board]
    └─ showLanGameScreen("Host — Playing as White")
LAN CLIENT path:
  Receives CHESS_ACTION("START:...") → deserialize board
    └─ showLanGameScreen("Client — Playing as Black")
Move broadcast:
  handleLanSquareClick → chessBoard.makeMove()
    └─ sendNetworkMessage(GameMessage.chessMove(fromQ,fromR,toQ,toR))
         └─ Remote side receives CHESS_MOVE → applies move → redraws
```

---

## 4. File-by-File Reference

---

### 📁 gamebox package

---

#### `Launcher.java` — 27 lines
**Role:** JavaFX application entry point.

| Method | What It Does |
|--------|-------------|
| `start(Stage)` | Sets fullscreen mode, creates `GameHub`, calls `hub.show()` |
| `main(String[])` | Calls `Application.launch()` to bootstrap JavaFX runtime |

**Key fact:** JavaFX requires a subclass of `Application`; `main()` just delegates to `launch()` which calls `start()` on the JavaFX thread.

---

#### `GameHub.java` — 123 lines
**Role:** Main menu launcher. Shows three clickable game cards; instantiates and starts each game on click.

| Method | What It Does |
|--------|-------------|
| `GameHub(Stage)` | Stores stage reference, sets title and maximized mode |
| `show()` | Builds the 3-card selection screen (Memory, Tetris, Chess) and sets scene |
| `buildModeCard(icon, title, desc, accent, onClick)` | Creates a styled hover-animated card that runs a callback on click |
| `toHexString(Color)` | Converts JavaFX `Color` to `#RRGGBB` CSS string |

**Dependencies it creates:** `GameBox`, `TetrisApp`, `ChessApp`

---

#### `GameBox.java` — 395 lines
**Role:** Memory game controller. Coordinates between `MenuPanel`, `GamePanel`, `GameLogic`, `GameHost`, and `GameClient`.

| Method | What It Does |
|--------|-------------|
| `GameBox(Stage, GameHub)` | Constructor; wires callbacks for the window-close event |
| `show()` | Shows the `MenuPanel` for mode selection |
| `showMenu()` | Rebuilds and shows the menu (used for "back" navigation) |
| `startLocalGame(matchSize, deckSize)` | Initializes `GameLogic`, creates `GamePanel` for 2-player local mode |
| `startHostGame(matchSize, deckSize)` | Creates `GameHost`, waits in background for client, starts game when connected |
| `startJoinGame(name, ip, port)` | Creates `GameClient`, connects to host, waits for `GAME_START` |
| `onHostReceivedMessage(GameMessage)` | Processes messages received on the host side (`CARD_CLICK`, `RESTART_REQUEST`) |
| `handleRemoteCardClick(cardIndex)` | Validates a card flip from the remote client and runs it through `GameLogic` |
| `broadcastState(GameState)` | Updates the local `GamePanel` and (if hosting) sends `STATE_UPDATE` over the wire |
| `handleCardClick(cardIndex)` | Central card-click router: local or client mode |
| `startMismatchTimer()` | Waits ~1.2s then calls `GameLogic.resolveMismatch()` and redraws |
| `restartGame()` | Re-initializes `GameLogic`, optionally sends `RESTART_CONFIRMED` over network |
| `sendMessage(GameMessage)` | Sends via `gameHost` or `gameClient` whichever is active |

---

#### `MenuPanel.java` — 543 lines
**Role:** The pre-game UI for the Memory game. Three screens: mode select, config, and join.

| Method | What It Does |
|--------|-------------|
| `MenuPanel(onLocal, onHost, onJoin, onBack)` | Builds all three sub-screens, shows mode screen initially |
| `getPlayer1Name()` | Returns typed player 1 name |
| `getPlayer2Name()` | Returns typed player 2 name |
| `getJoinPlayerName()` | Returns typed join player name |
| `getPort()` | Returns port number from the port field |
| `showScreen(VBox)` | Swaps the visible child in the `StackPane` |
| `buildModeScreen()` | Builds the 3-mode selection cards (LOCAL, HOST, JOIN) |
| `buildModeCard(...)` | Creates a styled clickable card with hover animation |
| `buildConfigScreen()` | Builds the game config screen (player names, match size, deck size) |
| `buildJoinScreen()` | Builds the "Join a Game" screen with IP/port/name fields |
| `showConfigScreen()` | Switches to config screen; hides P2 name field if not in local mode |
| `updateConfigUI()` | Refreshes match-size label and suggestion deck size buttons |
| `getSuggestedDeckSizes(n)` | Computes 3 valid deck size suggestions for a given match size `n` |
| `selectSuggestion(idx)` | Highlights the chosen deck size suggestion button |
| `applyCustomDeck()` | Validates and applies a manually typed deck size |
| `handleStart()` | Validates config then calls `onLocalGame` or `onHost` callback |
| `styleSelected(Button)` | Applies purple "selected" style to a suggestion button |
| `styleUnselected(Button)` | Applies neutral unselected style to a button |
| `makeLabel(...)` | Factory for styled `Label` nodes |
| `makeBackButton()` | Creates a "← Back" button that returns to the mode screen |
| `makeBigButton(text, color)` | Creates a large styled action button |
| `makeRoundButton(text, color)` | Creates a small round `+`/`-` stepper button |
| `setStatus(text, color)` | Updates the status label on the config screen (thread-safe) |
| `toHexString(Color)` | Converts JavaFX `Color` to CSS hex string |

---

#### `GamePanel.java` — 292 lines
**Role:** In-game UI for the Memory game. Shows the card grid, scores, turn indicator, and end-game overlay.

| Method | What It Does |
|--------|-------------|
| `GamePanel(localPlayer, localMode, p1, p2, onCardClick, onRestart, onBack)` | Constructor; builds sidebar, card grid, and overlay |
| `buildSidebar()` | Builds the left sidebar with scores, turn indicator, and buttons |
| `buildCardGrid(state)` | Builds the `GridPane` of `CardComponent` objects |
| `updateState(GameState)` | Full re-render: updates scores, turn label, card grid, and overlay |
| `updateScores(GameState)` | Refreshes both player score labels |
| `updateTurnLabel(GameState)` | Updates "Player X's Turn" indicator |
| `updateCardGrid(GameState)` | Calls `CardComponent.updateState()` for each card |
| `showOverlay(GameState)` | Shows the win/draw result overlay at game over |
| `removeOverlay()` | Removes the overlay (for restart) |

---

#### `CardComponent.java` — 233 lines
**Role:** JavaFX visual for a single memory card. Handles flip animation and visual states (hidden, face-up, matched).

| Method | What It Does |
|--------|-------------|
| `CardComponent(Card, index, onClick)` | Builds the card node, wires the click handler |
| `updateState(Card)` | Applies face-up / face-down / matched visual based on card state |
| `animateFlip(faceUp)` | Plays a scale-X animation to simulate a 3D card flip |
| `buildFront(Card)` | Creates the face-up card display with symbol |
| `buildBack()` | Creates the face-down card display |

---

#### `GameLogic.java` (gamebox) — 145 lines
**Role:** Sole rules engine for the Memory game. Manages the deck, turns, match resolution, and scores.

| Method | What It Does |
|--------|-------------|
| `initializeGame(GameConfig)` | Resets and builds a shuffled deck; sets match size |
| `generateDeck(GameConfig)` | Fills a list with `matchSize` copies of each symbol, then shuffles |
| `handleCardClick(cardIndex, playerNumber)` | Validates the click, flips the card, calls `resolveAttempt()` when full set selected |
| `resolveAttempt()` | Checks if all selected cards match; assigns scores or triggers mismatch |
| `resolveMismatch()` | Flips non-matched cards back; advances turn |
| `getState()` | Overload — calls `getState("")` |
| `getState(statusMessage)` | Snapshots current board into a `GameState` record |
| `getMatchedCount()` | Returns count of cards marked as matched |
| `isGameOver()` | True when all cards are matched |
| Various getters | `getDeckSize()`, `getActivePlayer()`, `getCurrentAttempt()`, etc. |

---

#### `GameHost.java` — 165 lines
**Role:** TCP server that accepts exactly one client and manages the session.

| Method | What It Does |
|--------|-------------|
| `GameHost(messageHandler, onConnectionLost)` | Stores callbacks |
| `startAndWaitForClient(port)` | Opens `ServerSocket`, calls `accept()` (blocks), performs handshake |
| `sendMessage(GameMessage)` | Serializes and writes a message to the client's output stream |
| `close()` | Closes the server socket and client socket cleanly |
| `startReaderThread()` | Background thread that reads `GameMessage` objects in a loop |
| `startHeartbeat()` | Scheduled heartbeat pings every 3 seconds to detect disconnections |
| `getClientPlayerName()` | Returns the name the client provided during handshake |
| `getHostAddress()` | Returns the local machine's LAN IP address |
| `isConnected()` | Returns the `running` flag |

---

#### `GameClient.java` — 124 lines
**Role:** TCP client that connects to a host and exchanges messages.

| Method | What It Does |
|--------|-------------|
| `GameClient(messageHandler, onConnectionLost)` | Stores callbacks |
| `connect(hostAddress, port, playerName)` | Opens `Socket`, does handshake (send JOIN_REQUEST, read JOIN_ACCEPTED), starts reader + heartbeat |
| `sendMessage(GameMessage)` | Serializes and sends a message to the server |
| `close()` | Closes the socket |
| `startReaderThread()` | Background thread that reads `GameMessage` objects in a loop |
| `startHeartbeat()` | Sends `HEARTBEAT` every 3 seconds |
| `getPlayerNumber()` | Returns the assigned player number (2 for clients) |

---

#### `GameMessage.java` — 65 lines
**Role:** Serializable DTO for all network messages (memory game + chess).

| Method | What It Does |
|--------|-------------|
| `joinRequest(playerName)` | Static factory: builds a JOIN_REQUEST message |
| `joinAccepted(playerNumber)` | Static factory: builds a JOIN_ACCEPTED message |
| `gameStart(config, p1Name, p2Name)` | Static factory: builds a GAME_START message with config |
| `stateUpdate(state)` | Static factory: builds a STATE_UPDATE message |
| `cardClick(index)` | Static factory: builds a CARD_CLICK message |
| `chessMove(fq,fr,tq,tr)` | Static factory: builds a CHESS_MOVE message |
| `chessAction(action)` | Static factory: builds a CHESS_ACTION message (START, RESIGN, RESTART) |
| `heartbeat()` | Static factory: builds a HEARTBEAT message |
| All `get*()` methods | Field accessors for each message property |

---

#### `GameState.java` — 63 lines
**Role:** Immutable snapshot of the memory game board. Sent over the wire and used to refresh the UI.

Fields: `cards`, `player1Score`, `player2Score`, `activePlayer`, `phase`, `currentAttempt`, `matchSize`, `deckSize`, `statusMessage`

**Type:** Java `record` — all fields are final, set at construction, accessible via auto-generated accessors.

---

#### `GameConfig.java` — 37 lines
**Role:** Immutable configuration record for the memory game.

| Method | What It Does |
|--------|-------------|
| `GameConfig(matchSize, deckSize)` | Constructor |
| `validate(matchSize, deckSize)` | Static validator — throws `IllegalArgumentException` if params are out of range or not divisible |
| `getMatchSize()`, `getDeckSize()`, `getUniqueSymbolCount()` | Accessors |

---

#### `Card.java` — 71 lines
**Role:** Data model for a single memory card.

| Method | What It Does |
|--------|-------------|
| `Card(id, symbol)` | Constructor |
| `flip()` | Toggles `faceUp` |
| `setFaceUp(boolean)` | Directly sets face state |
| `match(playerNumber)` | Marks card as matched by a player |
| `reset()` | Flips face down (for mismatch) |
| `isFaceUp()`, `isMatched()`, `getSymbol()`, `getId()`, `getMatchedBy()` | Getters |

---

#### `MessageType.java` — 38 lines
**Role:** Enum of all network message discriminators.

Values: `JOIN_REQUEST`, `JOIN_ACCEPTED`, `GAME_START`, `CARD_CLICK`, `STATE_UPDATE`, `GAME_END`, `RESTART_REQUEST`, `RESTART_CONFIRMED`, `ERROR`, `HEARTBEAT`, `CHESS_MOVE`, `CHESS_ACTION`

---

#### `GamePhase.java` — 16 lines
**Role:** Enum of memory game state-machine phases.

Values: `LOBBY`, `PLAYING`, `RESOLVING`, `GAME_OVER`

---

#### `MyController.java` — 17 lines
**Role:** Stub FXML controller linked to `MyView.fxml`. Not used in the actual game flow. Likely an early prototype.

---

### 📁 tetris package

---

#### `TetrisApp.java` — 503 lines
**Role:** Tetris entry point. Manages all UI screens (options, host, join) and LAN lifecycle.

| Method | What It Does |
|--------|-------------|
| `TetrisApp(Stage, GameHub)` | Stores refs; registers window-close handler |
| `show()` | Shows the options/mode selection screen |
| `closeNetwork()` | Shuts down `activeHost` or `activeClient` |
| `showOptionsScreen()` | Builds the main Tetris menu with player names, speed slider, game-mode toggles |
| `startLocalGame()` | Builds `GameLogic` and `TetrisPanel`, starts the game loop |
| `showHostScreen()` | Starts `TetrisHost`, shows "waiting" UI, auto-detects LAN |
| `showJoinScreen()` | Starts `TetrisClient`, shows LAN host discovery list + manual IP option |
| `styleButton(Button)` | Applies consistent dark-theme button style |
| `buildSliderRow(...)` | Helper for building a labeled slider control |

---

#### `TetrisPanel.java` — 971 lines
**Role:** The core Tetris game view. Owns the game loop thread, input handler, and Canvas renderer.

| Method | What It Does |
|--------|-------------|
| `TetrisPanel(GameLogic, onBack)` | Builds canvas, back/restart buttons, registers key listeners |
| `setNetworkMode(isClient, outMessage)` | Marks this panel as a LAN client and wires output callback |
| `setLanHostMode()` | Marks this panel as a LAN host |
| `updateState(p1, p2)` | Replaces player states (used when receiving STATE_UPDATE from host) |
| `start()` | Launches the game loop thread (~16ms/frame) |
| `stop()` | Sets `running = false` to end the loop |
| `handleInput(now)` | Reads active keys; applies DAS (Delayed Auto-Shift) for smooth movement |
| `setupKeyEvents()` | Registers `onKeyPressed` for single-press actions (rotate, hard drop) |
| `showDisconnectOverlay(onQuit)` | Draws "CONNECTION LOST" overlay over the canvas |
| `render()` | Draws everything: background, both boards, pieces, ghosts, power-ups, HUD, overlays |
| `drawPlayerState(gc, p, offX, offY, horiz)` | Draws one player's complete board + stats + next piece |
| `drawShape(gc, piece, x, y, ghost)` | Draws a tetromino shape at pixel coordinates |
| `drawGhostShape(gc, piece, offX, offY, p, horiz)` | Draws the ghost (landing preview) in translucent form |
| `drawCell(gc, offX, offY, x, y, color, p, horiz)` | Draws a single locked cell with border effect |
| `pwPos(offX, offY, px, py, p, horiz)` | Converts board cell coordinates to canvas pixel coordinates for power-ups |
| `boardPixelWidth(p, horiz)` | Returns pixel width of a player's board |
| `boardPixelHeight(p, horiz)` | Returns pixel height of a player's board |

**Key design: DAS (Delayed Auto-Shift)**
When a movement key is held, the first press moves immediately. After a 170ms delay, it auto-repeats every 50ms. This is tracked per-player, per-piece with `lastDAS_*`, `lastRepeat_*`, `initialDAS_*` fields.

---

#### `GameLogic.java` (tetris) — 707 lines
**Role:** The complete Tetris rules engine: gravity, locking, line clears, scoring, power-ups, ghost, two-blocks mode.

| Method | What It Does |
|--------|-------------|
| `GameLogic(p1Name, p2Name, speedLevel)` | Creates two `PlayerState` objects, spawns first piece for each |
| `initModes()` | Initializes horizontal or two-blocks mode: sets `visibleRows`, spawns extra pieces |
| `getSpeedLevel()` | Returns the speed level setting |
| `update(now)` | Master tick: applies gravity and locking for both players |
| `applyGravity(p, now)` | Moves piece down if fall delay elapsed; triggers lock if at bottom |
| `lockAndSpawn(p)` | Locks piece, clears lines, checks power-ups, spawns next piece |
| `clearLines(p, lockedPiece)` | Calls `Board.getFullRows()`, triggers flash, removes rows, updates score |
| `calculateScore(linesCleared, backToBack)` | Returns points based on single/double/triple/tetris + back-to-back bonus |
| `checkAndSpawnPowerup(p, linesCleared)` | Randomly places a power-up on the board after a line clear |
| `applyPowerup(p, type, x, y)` | Executes a collected power-up (speed up, slow, swap, etc.) |
| `moveLeft(p)` | Moves active piece one cell left (if valid) |
| `moveRight(p)` | Moves active piece one cell right (if valid) |
| `softDrop(p)` | Moves piece down one cell immediately |
| `hardDrop(p)` | Instantly drops piece to the ghost position and locks |
| `rotateCW(p)` | Rotates active piece clockwise with SRS wall-kick attempts |
| `rotateCCW(p)` | Rotates active piece counter-clockwise with SRS wall-kicks |
| `getGhost(p)` | Returns a copy of the active piece at its lowest valid position |
| `isValidWithPeer(p, t, isSecond)` | Checks if a piece placement is valid AND doesn't overlap the other piece (two-blocks mode) |
| `movePiece(p, isSecond, dx)` | Shared left/right move logic for two-blocks mode |
| `moveLeft/Right/rotateCW/hardDrop Piece1/Piece2(p)` | Per-piece variants for two-blocks mode |
| `getGhost2(p)` | Ghost for the second piece in two-blocks mode |

---

#### `Board.java` — 181 lines
**Role:** The Tetris grid: a 24×10 array of colour strings. Manages line clears, power-up positions, and board transformations.

| Method | What It Does |
|--------|-------------|
| `Board()` | Initializes the grid with all `null` |
| `getTopRow()` | Returns `HEIGHT - visibleRows` (the first visible row index) |
| `isValid(Tetromino)` | Checks that every filled cell of the piece is within bounds and not on a filled cell |
| `lock(Tetromino)` | Stamps the piece's colour hex into the grid |
| `getFullRows()` | Returns indices of all completely filled rows |
| `clearRows(int[], isP2)` | Removes completed rows and shifts remaining rows down |
| `grow(lines)` | Increases `visibleRows` (board gets taller — power-up effect) |
| `shrink(lines)` | Decreases `visibleRows` (board gets shorter) |
| `clearRadius(cx, cy, radius)` | Clears all cells within a circular radius (bomb power-up) |
| `clearBelow(x, y)` | Clears an entire column below a point (column bomb) |
| `clearHiddenItems()` | Removes power-ups that are now in the hidden zone after a shrink |

---

#### `Tetromino.java` — 158 lines
**Role:** Represents a single Tetris piece: its type, colour, 4 rotation states, and current position.

| Method | What It Does |
|--------|-------------|
| `Tetromino(type, shapes)` | Standard constructor |
| `Tetromino(type, customHex, shapes)` | Constructor for custom-coloured pieces |
| `copy()` | Deep copies the piece including position and rotation state |
| `queuedCopy()` | Copy without position (used for the "next piece" preview reset) |
| `getShape()` | Returns the current rotation's 2D array |
| `rotateCW()` | Advances `state` by 1 (mod 4); skips O and special pieces |
| `rotateCCW()` | Decrements `state` by 1 (mod 4) |
| `isSpecial()` | Returns true for bomb types |
| `create(Type)` | Static factory that returns a piece with all 4 hardcoded rotation shapes |

**Rotation States:** 0 = spawn, 1 = right (CW), 2 = flip (180°), 3 = left (CCW)

---

#### `PlayerState.java` — 133 lines
**Role:** All per-player Tetris state: active piece, next piece, bag, board, score, timers.

| Method | What It Does |
|--------|-------------|
| `PlayerState(id, name)` | Delegates to full constructor with default speed |
| `PlayerState(id, name, speedLevel)` | Creates board, fills bag, sets first nextPiece |
| `refillBag()` | Adds all 7 standard pieces + 2 bombs + custom pieces, then shuffles |
| `pullFromBag()` | Removes and returns the first piece from the bag; refills if empty |
| `spawnNext()` | Promotes `nextPiece` to `activePiece`, draws the next from bag, centers it |
| `spawnNext2()` | Same for the second piece (two-blocks mode) — offset to avoid collision |
| `getFallDelay()` | Computes current gravity delay in ms considering speed level, lines cleared, and active power-ups |

---

#### `WallKicks.java` — 54 lines
**Role:** SRS (Super Rotation System) wall-kick offset tables.

| Method | What It Does |
|--------|-------------|
| `getKicks(type, fromState, toState, isCW)` | Returns the 5-element kick offset array for a given rotation direction |

**Data:** Contains `JLSTZ_CW[4][5][2]` and `I_CW[4][5][2]` static offset tables matching the Tetris guideline.

---

#### `TetrisHost.java` — 96 lines
**Role:** Tetris LAN server. Combines UDP discovery broadcasting with a TCP game session.

| Method | What It Does |
|--------|-------------|
| `TetrisHost(onMessage, onDisconnect)` | Stores callbacks |
| `start(port, hostName)` | Opens UDP socket (port 28081) for LAN discovery, then opens TCP server on the given port, accepts one client |
| `send(Object)` | Serializes and sends any object to the client |
| `close()` | Closes UDP and TCP sockets |
| `fireDisconnect()` | Thread-safe one-shot disconnection callback |

**LAN Discovery:** The UDP thread responds to `"TETRIS_DISCOVER"` broadcasts with `"TETRIS_HOST:name:port"`.

---

#### `TetrisClient.java` — 55 lines
**Role:** Tetris LAN client. Connects to a host via TCP.

| Method | What It Does |
|--------|-------------|
| `TetrisClient(onMessage, onDisconnect)` | Stores callbacks |
| `connect(ip, port)` | Opens socket, creates streams, starts reader thread |
| `send(Object)` | Serializes and sends to the host |
| `close()` | Closes the socket |

---

#### `TetrisMessage.java` — 20 lines
**Role:** Serializable DTO for all Tetris network messages.

Fields: `type`, `p1` (PlayerState), `p2` (PlayerState), `playerName`

Type values: `STATE_UPDATE`, `INPUT_LEFT`, `INPUT_RIGHT`, `INPUT_SOFT_DROP`, `INPUT_HARD_DROP`, `INPUT_ROTATE_CW`, `INPUT_ROTATE_CCW`, `RESTART_REQUEST`, `PLAYER_NAME`

---

#### `CustomPieceDesigner.java` — 260 lines
**Role:** UI tool for designing, saving, and loading custom Tetris pieces. Pieces are persisted to `custom_pieces.txt`.

| Method | What It Does |
|--------|-------------|
| `loadPieces()` | Reads `custom_pieces.txt`, parses hex colour + 25-bit binary shape, generates 4 rotations |
| `savePieces()` | Writes all custom pieces to `custom_pieces.txt` |
| `show(Stage, onBack)` | Builds the designer UI: 5×5 grid of toggle buttons on the right, saved pieces list on the left |
| `validateConnected(state)` | BFS check to ensure the drawn shape is a single connected component |

---

#### `SpeedTest.java`, `TestLogic.java`, `test.java` — 19/33/30 lines
**Role:** Development scratch files used during testing. Not part of the production game flow.

---

### 📁 chess package

---

#### `ChessApp.java` — 1047 lines
**Role:** The entire Chess game controller: all UI screens, board rendering, LAN networking, bot integration.

| Method | What It Does |
|--------|-------------|
| `ChessApp(Stage, GameHub)` | Constructor |
| `show()` | Entry point — shows mode selection |
| `showModeScreen()` | Builds 4 mode cards (Local, Host, Join, vs Bot) |
| `buildModeCard(...)` | Styled card factory with hover animation |
| `startLocalGame()` | Initializes `ChessBoard`, shows local game screen |
| `startBotGame()` | Same as local but sets `botMode = true` |
| `maybeTriggerBot()` | If bot mode and it's Black's turn, submits a background task to compute and apply the bot's move |
| `showPromotionDialog(color, isLan)` | Overlays the board with 4 promotion choice cards |
| `showHostScreen()` | Creates `GameHost`, starts waiting for client in background, shows waiting UI |
| `showJoinScreen()` | Creates `GameClient`, shows IP/connect UI |
| `handleNetworkMessage(GameMessage)` | Routes `CHESS_MOVE`, `CHESS_ACTION` (START, RESIGN, RESTART) to the right handler |
| `handleConnectionLost()` | Shows an alert dialog and returns to mode screen |
| `showLanGameScreen(roleText)` | Builds the LAN game UI with sidebar (role, turn, resign button) |
| `handleLanSquareClick(q, r)` | Validates and processes a click during LAN play (turn-locking enforced) |
| `sendNetworkMessage(GameMessage)` | Routes through host or client depending on which is active |
| `closeLanConnection()` | Closes host/client and resets `lanMode` |
| `showLocalGameScreen()` | Builds local game UI |
| `buildBoardPane(isLan)` | Constructs the hex board: 91 `StackPane` cells with piece images/shapes, wired to click handlers |
| `updateBoardDisplay()` | Re-draws all 91 board cells, updates turn label, status, checks win/draw conditions |
| `handleLocalSquareClick(q, r)` | Selection + move logic for local/bot play |
| `restartLocalGame()` | Resets `ChessBoard`, re-renders |
| `lbl(text, size, weight, color)` | Label factory helper |
| `styleButton(btn, color)` | Applies consistent sidebar button style |
| `toHex(Color)` | Converts JavaFX Color to CSS hex |

---

#### `ChessBoard.java` — 380 lines
**Role:** The game state for Hexagonal Chess. Owns the 11×11 piece array, handles move validation and execution, serialization.

| Method | What It Does |
|--------|-------------|
| `ChessBoard()` | Calls `resetBoard()` |
| `isValidCoord(q, r)` | Delegates to `HexCoord.isValid()` |
| `getPiece(q, r)` / `setPiece(q, r, p)` | Array access with `+5` offset translation |
| `getCurrentTurn()` / `setCurrentTurn(color)` | Turn management |
| `getHalfMoveClock()` | Returns the 50-move rule counter |
| `getPositionHistory()` | Returns list of serialized positions for 3-fold repetition |
| `getLastFrom/ToQ/R()` | Last move coordinates (needed for en-passant) |
| `setLastMove(...)` | Stores last move |
| `hasPendingPromotion()` / `completePromotion(type)` | Promotion state management |
| `resetBoard()` | Places all 34 pieces in starting position |
| `setupPiece(coord, type, color)` | Parses algebraic notation and places a piece |
| `isValidMove(fromQ, fromR, toQ, toR)` | Full move legality check including check-simulation |
| `makeMove(fromQ, fromR, toQ, toR)` | Executes a legal move; handles en-passant, promotion detection, turn switch |
| `getEnPassantTarget()` | Returns en-passant square based on last pawn double-move |
| `isValidPieceMoveBasic(...)` | Delegates to `ChessRules` |
| `isInCheck(color)` | Delegates to `ChessRules` |
| `hasValidMoves(color)` | Delegates to `ChessRules` |
| `serializeBoard()` | Returns `"TURN;coord:COLOR:TYPE,..."` string |
| `deserializeBoard(serialized)` | Parses the serialized string and re-places all pieces |
| `checkDrawCriteria()` | Delegates to `ChessRules.checkDrawCriteria()` |
| `getBotMove(color)` | Delegates to `ChessBot.getBotMove()` |

---

#### `ChessRules.java` — 346 lines
**Role:** Stateless rules engine for hexagonal chess. Handles move validation, check detection, draw conditions.

| Method | What It Does |
|--------|-------------|
| `isValidPieceMoveBasic(board, p, fq, fr, tq, tr)` | Switch on piece type: validates direction and path clearance |
| `isPathClear(board, fq, fr, tq, tr, sq, sr)` | Walks from source to destination checking for blocking pieces |
| `isWhitePawnStart(q, r)` / `isBlackPawnStart(q, r)` | Determines if a pawn is on its starting row (for 2-step move) |
| `isInCheck(board, color)` | Finds the king, then checks if any opponent piece can attack it |
| `hasValidMoves(board, color)` | Tries all possible moves for every piece of the color; returns true if any are legal |
| `validatePosition(board, startingTurn)` | Validates king counts, pawn counts, kings-adjacent, and check legality |
| `checkDrawCriteria(board)` | Checks 50-move rule, insufficient material, and threefold repetition |
| `isInsufficientMaterial(board)` | Returns true for K vs K, K+B vs K, K+N vs K |
| `isThreefoldRepetition(board)` | Counts position occurrences in history; true if any appears 3+ times |

---

#### `ChessBot.java` — 132 lines
**Role:** Simple chess AI. Uses a 3-stage heuristic: find checkmate, avoid losing, maximize piece capture.

| Method | What It Does |
|--------|-------------|
| `getBotMove(board, botColor)` | Main entry: runs 3 stages and returns the best `Move` |
| `getLegalMoves(board, color)` | Generates all legal moves for a given color |
| `isMoveDeliveringCheckmate(board, fq, fr, tq, tr, attacker)` | Tests if a move results in checkmate for the opponent |

**Algorithm:**
1. If any move is checkmate → take it immediately
2. Filter out moves that leave the opponent with a checkmate reply
3. Score remaining moves by captured piece value (Pawn=10, Knight/Bishop=30, Rook=50, Queen=90) + random tie-breaker

---

#### `HexCoord.java` — 47 lines
**Role:** Utility for hexagonal coordinate validation and algebraic notation conversion.

| Method | What It Does |
|--------|-------------|
| `isValid(q, r)` | Returns true if `(q,r)` is within the hexagonal board (axial coords, -5 to +5 with |q+r|≤5) |
| `toAlgebraic(q, r)` | Converts axial `(q, r)` to chess notation like `"f5"` |
| `parseAlgebraic(code)` | Parses `"f5"` back to `int[]{q, r}` |

**Coordinate System:** Axial hex coordinates where `q` = file (a-l, skipping j), `r` = rank computed from file and row offset.

---

#### `Piece.java` — 47 lines
**Role:** Immutable chess piece model: type and color with display helpers.

| Method | What It Does |
|--------|-------------|
| `Piece(type, color)` | Constructor |
| `getType()` / `getColor()` | Accessors |
| `isWhite()` | Returns `color == WHITE` |
| `getSymbol()` | Returns Unicode chess symbol (♟, ♜, etc.) |
| `getImageName()` | Returns `"merida/wQ.png"` style filename for the piece image resource |

---

#### `Move.java` — 12 lines
**Role:** Immutable value object for a chess move (from/to in axial coordinates).

Fields: `fromQ`, `fromR`, `toQ`, `toR` (all `final int`)

---

## 5. Key Data Flow: Memory Game Card Click (LAN)

```
CLIENT side:
  User clicks card → CardComponent.onClick → GameBox.handleCardClick(i)
    → isClient==true → gameClient.sendMessage(GameMessage.cardClick(i))
      → ObjectOutputStream.writeObject(msg)
        → [TCP socket] →
HOST side:
  ObjectInputStream.readObject() → messageHandler.accept(msg)
    → Platform.runLater → onHostReceivedMessage(msg)
      → handleRemoteCardClick(msg.getCardIndex())
        → gameLogic.handleCardClick(index, 2)  [player 2 = client]
          → resolveAttempt() or just flip
        → broadcastState(gameLogic.getState())
          → gamePanel.updateState(state)         [host updates locally]
          → gameHost.sendMessage(STATE_UPDATE)
            → [TCP socket] →
CLIENT side (again):
  → messageHandler → onClientReceivedMessage → gamePanel.updateState(state)
```

---

## 6. Key Data Flow: Tetris LAN Game Loop

```
HOST (every ~16ms):
  TetrisPanel.start() loop:
    handleInput() → logic.moveLeft/Right/softDrop(p1)
    logic.update() → applyGravity(p1), applyGravity(p2)
    outMessage callback → TetrisHost.send(TetrisMessage(STATE_UPDATE, p1, p2))
CLIENT (event-driven):
  onKeyPressed → TetrisPanel → outMessage(INPUT_LEFT)
    → TetrisClient.send(TetrisMessage(INPUT_LEFT))
      → [TCP] → TetrisHost receives
        → onMessage → TetrisApp handler
          → logic.moveLeft(logic.p2)   [client controls p2]
HOST receives STATE_UPDATE loop from its own logic,
sends p1+p2 state to client every frame.
CLIENT receives STATE_UPDATE:
  → TetrisPanel.updateState(p1, p2)
  → render() draws the received state
```

---

## 7. Likely Interview Questions & Answers

**Q: What is the entry point of the application?**
A: `Launcher.java`. It extends `Application` (JavaFX), and `main()` calls `Application.launch()` which invokes `start(Stage)` on the JavaFX thread. `start()` creates a `GameHub` and calls `show()`.

**Q: How does LAN multiplayer work in the memory game?**
A: The host creates a `GameHost` which opens a `ServerSocket` on port 5555. It waits for a `JOIN_REQUEST`, responds with `JOIN_ACCEPTED`, then both sides use `ObjectInputStream/ObjectOutputStream` over TCP. All game logic runs on the host; the client only sends `CARD_CLICK` messages. The host processes the click, updates the game state, and broadcasts `STATE_UPDATE` to both sides.

**Q: How does the Tetris game loop work?**
A: `TetrisPanel.start()` launches a daemon thread that runs every 16ms (~60fps). Each tick: (1) `handleInput(now)` processes held keys with DAS timing; (2) `logic.update(now)` applies gravity and locking; (3) `Platform.runLater(render)` queues a canvas redraw on the JavaFX thread. JavaFX is not thread-safe, so all UI updates must go through `Platform.runLater`.

**Q: What is DAS in Tetris?**
A: Delayed Auto-Shift. When a directional key is held, the piece moves immediately on first press, waits 170ms, then repeats every 50ms. This prevents unintentional rapid movement while still allowing fast sliding. Each player has separate `lastDAS`, `lastRepeat`, and `initialDAS` state variables.

**Q: How does chess move validation work?**
A: In two stages. First, `ChessRules.isValidPieceMoveBasic()` checks whether the move matches the piece's movement pattern (e.g., rook moves along a rank/file/diagonal with a clear path). Second, `ChessBoard.isValidMove()` simulates the move on the board and calls `ChessRules.isInCheck()` to ensure the king is not left in check. If both pass, the move is legal.

**Q: How does the chess bot work?**
A: It's a greedy heuristic AI. Step 1: check if any move delivers immediate checkmate. Step 2: filter out moves that give the opponent a checkmate response. Step 3: among remaining moves, score each by the value of the captured piece (pawn=10, rook=50, queen=90) plus a small random value to break ties. Pick the highest-scoring move.

**Q: How does the hexagonal chess board work geometrically?**
A: It uses axial hex coordinates (q, r) where both range from -5 to +5 with the constraint |q+r| ≤ 5. This produces 91 valid hexagonal cells. The `HexCoord` class converts between these coordinates and algebraic notation like "f5". The board is stored as an 11×11 Java array with a +5 offset to handle negative coordinates.

**Q: What serialization mechanism is used for networking?**
A: Java object serialization via `ObjectInputStream` / `ObjectOutputStream` over raw TCP sockets. Both `GameMessage` and `TetrisMessage` implement `Serializable`. For chess, the board is serialized to a text format (`"TURN;coord:COLOR:TYPE,..."`) that is embedded as a string in a `GameMessage`.

**Q: What is the SRS wall-kick system in Tetris?**
A: Super Rotation System. When a piece rotation would put it in an invalid position, the game tries up to 5 offset positions (kicks) defined in `WallKicks.java`. The kick tables are different for the I piece vs all others (JLSTZ). CCW kicks are computed as the negative of the CW kicks from the destination state.

**Q: How are power-ups implemented in Tetris?**
A: After a line clear, `GameLogic.checkAndSpawnPowerup()` has a random chance of placing a power-up token at a random position on the board (tracked in `Board`). When the active piece locks on top of a power-up cell, `applyPowerup()` is triggered. Effects include: speed up opponent, slow self, swap boards, delay opponent rotation, radius bomb clear, column clear, and portal (random column teleport).

**Q: Why does `Piece` depend on `ChessBoard`'s inner enums?**
A: A design flaw — `PieceType` and `PieceColor` are defined as enums inside `ChessBoard`, but `Piece` uses them. This creates an upward dependency from a leaf class to a container class. The fix would be to promote them to top-level enums.

**Q: What does `Platform.runLater()` do and why is it needed?**
A: JavaFX has a single UI thread (the "JavaFX Application Thread"). All UI modifications must happen on this thread. `Platform.runLater(Runnable)` queues the runnable to execute on the JavaFX thread. Without it, updating a Label from a background networking thread would cause `IllegalStateException: Not on FX application thread`.

---

## 8. Lines of Code Summary

| File | Package | LOC |
|------|---------|-----|
| ChessApp.java | chess | 1047 |
| TetrisPanel.java | tetris | 971 |
| MenuPanel.java | gamebox | 543 |
| TetrisApp.java | tetris | 503 |
| GameLogic.java | tetris | 707 |
| ChessBoard.java | chess | 380 |
| ChessRules.java | chess | 346 |
| GameBox.java | gamebox | 395 |
| GamePanel.java | gamebox | 292 |
| CustomPieceDesigner.java | tetris | 260 |
| CardComponent.java | gamebox | 233 |
| TetrisHost.java | tetris | 96 |
| GameHost.java | gamebox | 165 |
| Board.java | tetris | 181 |
| Tetromino.java | tetris | 158 |
| GameHub.java | gamebox | 123 |
| GameClient.java | gamebox | 124 |
| PlayerState.java | tetris | 133 |
| ChessBot.java | chess | 132 |
| GameLogic.java | gamebox | 145 |
| GameState.java | gamebox | 63 |
| GameMessage.java | gamebox | 65 |
| GameConfig.java | gamebox | 37 |
| WallKicks.java | tetris | 54 |
| TetrisClient.java | tetris | 55 |
| HexCoord.java | chess | 47 |
| Piece.java | chess | 47 |
| Card.java | gamebox | 71 |
| MessageType.java | gamebox | 38 |
| Launcher.java | gamebox | 27 |
| TetrisMessage.java | tetris | 20 |
| GamePhase.java | gamebox | 16 |
| Move.java | chess | 12 |
| MyController.java | gamebox | 17 |
| **TOTAL** | | **~6,600** |
