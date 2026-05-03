package comp20050.qssboard.controller;

import comp20050.qssboard.model.*;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class BotRunner {

    private final Pane boardPane;
    private final MonteCarloSimulator simulator;
    private final Callbacks callbacks;
    private QuaxBoard board;  // Mutable state synced from GameController
    private TileState botColor;
    private boolean testMode = false;
    private boolean gameOver = false;
    public BotRunner(Pane boardPane, MonteCarloSimulator simulator, Callbacks callbacks) {
        this.boardPane = boardPane;
        this.simulator = simulator;
        this.callbacks = callbacks;
    }

    // Called by GameController after each move to keep BotRunner in sync
    public void setBoard(QuaxBoard board) {
        this.board = board;
    }

    public void setBotColor(TileState botColor) {
        this.botColor = botColor;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    // Checks conditions like whether its bot player's turn, whether game is over etc.
    protected void runAnalysis() {
        if (testMode) return;
        if (gameOver) return;

        // Only act if it's the bot's turn
        TileState current = board.isBlackTurn() ? TileState.BLACK : TileState.WHITE;
        if (current != botColor) return;

        QuaxBoard snapshot = board.deepCopy();

        Thread t = new Thread(() -> {
            int best = simulator.bestMove(snapshot);
            double rate = simulator.lastBestWinRate;

            Platform.runLater(() -> {
                // Report result back to GameController before playing the move
                callbacks.onMoveResult(best, rate);
                playBotMove(best);
                callbacks.onRunAnalysisDone();
            });
        });

        t.setDaemon(true);
        t.start();
    }

    // Bot finds empty confirms that tile is empty and plays move
    public void playBotMove(int move) {
        if (gameOver) return;

        // Clear any lingering strategy overlays
        boardPane.getChildren().removeIf(node -> "strategyHighlight".equals(node.getUserData()));

        if (move >= 0) {
            if (octagonMove(move)) return;

        } else {
            if (rhombusMove(move)) return;
        }

        board.setBlackTurn(!board.isBlackTurn());
        callbacks.onDisplayPlayerTurn();
    }

    private boolean rhombusMove(int move) {
        int ri = -(move + 1), col = ri % 10, row = ri / 10;
        System.out.printf("BOT PLAYING: Rhombus (%d,%d)%n", col + 1, row + 1);

        Rhombus rho = board.getRhombus(col + 0.5, row + 0.5);
        if (rho.getState() != TileState.EMPTY) {
            System.out.println("ERROR: Rhombus already occupied!");
            return true;
        }

        rho.setState(botColor);
        updateShape(rho);
        callbacks.onMovePlayed();

        if (board.checkWin(botColor)) {
            callbacks.onWin(botColor);
            return true;
        }
        return false;
    }

    private boolean octagonMove(int move) {
        int col = move % 11, row = move / 11;
        System.out.printf("BOT PLAYING: Octagon (%c,%d)%n", (char) ('A' + col), row + 1);

        Octagon oct = board.getOctagon(col, row);
        if (oct.getState() != TileState.EMPTY) {
            System.out.println("ERROR: Octagon already occupied!");
            return true;
        }

        oct.setState(botColor);
        updateShape(oct);
        callbacks.onMovePlayed();

        if (board.checkWin(botColor)) {
            callbacks.onWin(botColor);
            return true;
        }
        return false;
    }

    // Helper that displays moves
    private void updateShape(Object tile) {
        boardPane.getChildren().stream()
                .filter(n -> n.getUserData() == tile)
                .findFirst()
                .ifPresent(n -> ((Polygon) n).setFill(
                        botColor == TileState.BLACK ? Color.BLACK : Color.WHITE
                ));
    }

    // Callbacks into GameController — BotRunner should not mutate game state itself
    public interface Callbacks {
        void onDisplayPlayerTurn();

        void onRunAnalysisDone();

        void onWin(TileState winner);

        void onMoveResult(int bestMove, double winRate);

        void onMovePlayed();
    }
}
