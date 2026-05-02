package comp20050.qssboard.controller;

import comp20050.qssboard.model.MonteCarloSimulator;
import comp20050.qssboard.model.Octagon;
import comp20050.qssboard.model.QuaxBoard;
import comp20050.qssboard.model.Rhombus;
import comp20050.qssboard.model.TileState;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class BotRunner {

    private final Pane boardPane;
    private final MonteCarloSimulator simulator;

    // Callbacks into GameController — BotRunner should not mutate game state itself
    private final Runnable onDisplayPlayerTurn; // called after bot places a move
    private final Runnable onRunAnalysisDone;   // called when analysis finishes so GameController can update isAnalysing
    private final WinCallback onWin;              // called if the bot's move wins the game
    private final MoveResultCallback onMoveResult;       // called with best move + win rate so GameController can store them
    private final Runnable onMovePlayed; // called after each successful bot move

    @FunctionalInterface
    public interface WinCallback {
        void onWin(TileState winner);
    }

    @FunctionalInterface
    public interface MoveResultCallback {
        void onResult(int bestMove, double winRate);
    }

    // Mutable state synced from GameController
    private QuaxBoard board;
    private TileState botColor;
    private boolean   testMode  = false;
    private boolean   gameOver  = false;

    public BotRunner(Pane boardPane, MonteCarloSimulator simulator,
                     Runnable onDisplayPlayerTurn,
                     Runnable onRunAnalysisDone,
                     WinCallback onWin,
                     MoveResultCallback onMoveResult, Runnable onMovePlayed) {
        this.boardPane = boardPane;
        this.simulator = simulator;
        this.onDisplayPlayerTurn = onDisplayPlayerTurn;
        this.onRunAnalysisDone = onRunAnalysisDone;
        this.onWin = onWin;
        this.onMoveResult = onMoveResult;
        this.onMovePlayed = onMovePlayed;
    }

    // Called by GameController after each move to keep BotRunner in sync
    public void setBoard(QuaxBoard board)       { this.board    = board;    }
    public void setBotColor(TileState botColor) { this.botColor = botColor; }
    public void setTestMode(boolean testMode)   { this.testMode = testMode; }
    public void setGameOver(boolean gameOver)   { this.gameOver = gameOver; }

    protected void runAnalysis() {
        if (testMode) return;
        if (gameOver) return;

        // Only act if it's the bot's turn
        TileState current = board.isBlackTurn() ? TileState.BLACK : TileState.WHITE;
        if (current != botColor) return;

        QuaxBoard snapshot = board.deepCopy();

        Thread t = new Thread(() -> {
            int best    = simulator.bestMove(snapshot);
            double rate = simulator.lastBestWinRate;

            Platform.runLater(() -> {
                // Report result back to GameController before playing the move
                onMoveResult.onResult(best, rate);
                playBotMove(best);
                onRunAnalysisDone.run();
            });
        });

        t.setDaemon(true);
        t.start();
    }

    private void highlightBestMove(int move) {
        // decode and highlight the suggested tile in the UI
        if (move >= 0) {
            int col = move % 11, row = move / 11;
            System.out.println("Suggested octagon: " + col + "," + row);
        } else {
            int ri = -(move + 1);
            System.out.println("Suggested rhombus: " + ri % 10 + "," + ri / 10);
        }
    }

    public void playBotMove(int move) {
        if (gameOver) return;

        // Clear any lingering strategy overlays
        boardPane.getChildren().removeIf(node -> "strategyHighlight".equals(node.getUserData()));

        if (move >= 0) {
            int col = move % 11, row = move / 11;
            System.out.printf("BOT PLAYING: Octagon (%c,%d)%n", (char)('A' + col), row + 1);

            Octagon oct = board.getOctagon(col, row);
            if (oct.getState() != TileState.EMPTY) {
                System.out.println("ERROR: Octagon already occupied!");
                return;
            }

            oct.setState(botColor);
            updateShape(oct);
            onMovePlayed.run();

            if (board.checkWin(botColor)) { onWin.onWin(botColor); return; }

        } else {
            int ri = -(move + 1), col = ri % 10, row = ri / 10;
            System.out.printf("BOT PLAYING: Rhombus (%d,%d)%n", col + 1, row + 1);

            Rhombus rho = board.getRhombus(col + 0.5, row + 0.5);
            if (rho.getState() != TileState.EMPTY) {
                System.out.println("ERROR: Rhombus already occupied!");
                return;
            }

            rho.setState(botColor);
            updateShape(rho);
            onMovePlayed.run();

            if (board.checkWin(botColor)) { onWin.onWin(botColor); return; }
        }

        board.setBlackTurn(!board.isBlackTurn());
        onDisplayPlayerTurn.run();
    }

    private void updateShape(Object tile) {
        boardPane.getChildren().stream()
                .filter(n -> n.getUserData() == tile)
                .findFirst()
                .ifPresent(n -> ((Polygon) n).setFill(
                        botColor == TileState.BLACK ? Color.BLACK : Color.WHITE
                ));
    }
}
