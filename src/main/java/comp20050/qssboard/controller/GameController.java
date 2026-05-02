package comp20050.qssboard.controller;

import comp20050.qssboard.model.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import java.util.Optional;

public class GameController implements BoardConfig{

    @FXML private Pane boardPane;

    private QuaxBoard board;
    private int moveCount = 0;
    private boolean pieRuleUsed = false;
    private boolean gameOver = false;
    private boolean botGame = true;
    private boolean testMode = false;
    private boolean isAnalysing = false;

    private TileState humanColor = TileState.BLACK;
    private TileState botColor = TileState.WHITE;

    private int strategyMove = Integer.MIN_VALUE;
    private double bestMoveWinRate = 0.0;

    public void setTestMode(boolean testMode){
        this.testMode = testMode;
    }

    private MonteCarloSimulator simulator = new MonteCarloSimulator(2000000);

    // Components
    private BoardRenderer   renderer;
    private BoardUI         boardUI;
    private StrategyOverlay strategyOverlay;
    private BotRunner       botRunner;

    @FXML
    public void initialize() {
        board = new QuaxBoard(CELLS);

        // Wire up components with callbacks so they can reach back into
        // GameController without holding a direct reference to it
        renderer = new BoardRenderer(boardPane, board, this::onTileClicked);

        boardUI = new BoardUI(
                boardPane, board, renderer,
                this::showStrategy,   // onShowStrategy
                this::onPieRuleUsed,  // onPieRuleUsed
                this::switchColor,    // onSwitchColor
                this::runAnalysis     // onRunAnalysis
        );

        strategyOverlay = new StrategyOverlay(boardPane, board, simulator, renderer, botColor);

        botRunner = new BotRunner(
                boardPane, simulator,
                boardUI::displayPlayerTurn,          // onDisplayPlayerTurn
                () -> isAnalysing = false,           // onRunAnalysisDone
                winner -> {                          // onWin
                    gameOver = true;
                    botRunner.setGameOver(true);
                    boardUI.displayWinner(winner);
                },
                (best, rate) -> {                    // onMoveResult
                    strategyMove    = best;
                    bestMoveWinRate = rate;
                    strategyOverlay.setStrategyMove(best, rate);
                },
                () -> { moveCount++; syncComponentState(); } // onMovePlayed
        );
        botRunner.setBoard(board);
        botRunner.setBotColor(botColor);
        botRunner.setTestMode(testMode);

        renderer.drawBoard();
        boardUI.displayPlayerTurn();

        if (botColor == TileState.BLACK) runAnalysis();

        if (!testMode) {
            Platform.runLater(() -> {
                chooseBotColourAtStart();
                boardUI.updateWindowTitle();
            });
        }
    }

    // ask the player what colour they want to play
    private void chooseBotColourAtStart() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quax");
        alert.setHeaderText("Human V Bot");
        alert.setContentText("Choose your colour:");
        alert.getButtonTypes().setAll(new ButtonType("BLACK"), new ButtonType("WHITE"));

        applyChosenColor(alert.showAndWait().orElse(null));

        if (botColor == TileState.BLACK) runAnalysis();

        //show a confirmation popup so user knows what color to play
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Quax");
        info.setHeaderText("Bot player added");
        info.setContentText("You are " + humanColor + ". Bot is " + botColor + ".");
        info.showAndWait();
    }

    void applyChosenColor(ButtonType choice) {
        if (choice != null && choice.getText().equalsIgnoreCase("white")) {
            humanColor = TileState.WHITE;
            botColor   = TileState.BLACK;
        } else {
            humanColor = TileState.BLACK;
            botColor   = TileState.WHITE;
        }
        // Keep BotRunner in sync after colour is decided
        botRunner.setBotColor(botColor);
        strategyOverlay.setBotColor(botColor);
    }

    public void switchColor(){
        board.setBlackTurn(false);
    }

    private void onPieRuleUsed() {
        pieRuleUsed = true;
        botColor    = TileState.WHITE;
        botRunner.setBotColor(botColor);
        syncComponentState();
    }


    // Event handler

    private void onTileClicked(Tile tile, Polygon shape) {
        if (gameOver)    return;
        if (isAnalysing) return;

        // Clear any strategy overlays left from the previous turn
        strategyOverlay.hide();

        if (tile.getState() != TileState.EMPTY) {
            throw new IllegalArgumentException("Tile is already occupied by a player");
        }

        TileState newState = board.isBlackTurn() ? TileState.BLACK : TileState.WHITE;
        tile.setState(newState);
        shape.setFill(newState == TileState.BLACK ? Color.BLACK : Color.WHITE);

        System.out.println("Clicked: " + tile);
        moveCount++;
        syncComponentState();

        if (moveCount > 1) {
            boardPane.getChildren().removeIf(node -> "pieButton".equals(node.getUserData()));
        }

        if (board.checkWin(newState)) {
            gameOver = true;
            boardUI.displayWinner(newState);
            return;
        }

        board.setBlackTurn(!board.isBlackTurn());
        boardUI.displayPlayerTurn();
        runAnalysis();
    }

    // Handled by BotRunner
    private void runAnalysis() {
        if (testMode) return;
        if (gameOver) return;
        isAnalysing = true;
        botRunner.setBoard(board);
        botRunner.runAnalysis();
    }

    // Showing strategy is handled by StrategyOverlay
    private void showStrategy() {
        strategyOverlay.show();
    }

    // Keeps all components in sync after any state change
    private void syncComponentState() {
        boardUI.setMoveCount(moveCount);
        boardUI.setPieRuleUsed(pieRuleUsed);
        boardUI.setBotGame(botGame);
    }
}
