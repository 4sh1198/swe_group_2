package comp20050.qssboard.controller;

import comp20050.qssboard.model.QuaxBoard;
import comp20050.qssboard.model.TileState;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class BoardUI implements BoardConfig {

    private static final Color STRIP_BG = Color.web("#D0D0D0");
    private static final double STRIP_Y = TOTAL_SIZE;
    private static final double STRIP_H = 80;

    private final Pane boardPane;
    private final QuaxBoard board;
    private final BoardRenderer renderer; // needed for makeOctagon/makeRhombus in displayPlayerTurn
    private final Callbacks callbacks;
    // Mutable state synced from GameController after each move
    private int moveCount = 0;
    private boolean pieRuleUsed = false;
    private boolean botGame = true;
    private boolean testMode =  false;
    public BoardUI(Pane boardPane, QuaxBoard board, BoardRenderer renderer,
                   Callbacks callbacks) {
        this.boardPane = boardPane;
        this.board = board;
        this.renderer = renderer;
        this.callbacks = callbacks;
    }

    // Called by GameController after each move to keep display in sync
    public void setMoveCount(int moveCount) {
        this.moveCount = moveCount;
    }

    public void setPieRuleUsed(boolean used) {
        this.pieRuleUsed = used;
    }

    public void setBotGame(boolean botGame) {
        this.botGame = botGame;
    }

    public void setTestMode(boolean testMode) { this.testMode = testMode; }

    public void updateWindowTitle() {
        if (botGame && boardPane.getScene() != null
                && boardPane.getScene().getWindow() instanceof Stage stage) {
            stage.setTitle("Quax (Human V Bot)");
        }
    }

    // Display Methods
    public void displayPlayerTurn() {
        boardPane.getChildren().removeIf(node -> "turnDisplay".equals(node.getUserData()));
        boardPane.getChildren().removeIf(node -> "strategyButton".equals(node.getUserData()));

        addStripBackground();
        addTurnIndicator();
        addTurnLabel();

        if (moveCount == 1 && !pieRuleUsed) {
            addPieButton();
        }
        addStrategyButton();
    }

    public void displayWinner(TileState winner) {
        if (testMode) return;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Quax");
        alert.setHeaderText("Game Over!");
        alert.setContentText(winner == TileState.BLACK ? "Black wins!" : "White wins!");
        alert.showAndWait();
        // Note: gameOver flag is set in GameController, not here
    }

    // Grey background strip below the board
    private void addStripBackground() {
        Rectangle displayBg = new Rectangle(0, STRIP_Y, TOTAL_SIZE, STRIP_H);
        displayBg.setFill(STRIP_BG);
        displayBg.setUserData("turnDisplay");
        boardPane.getChildren().add(displayBg);
    }

    // UI Helpers

    // Small octagon and rhombus showing the current player's colour
    private void addTurnIndicator() {
        Color playerColor = board.isBlackTurn() ? Color.BLACK : Color.WHITE;
        double cx = BORDER + RADIUS;
        double cy = STRIP_Y + 10 + RADIUS;

        Polygon oct = renderer.makeOctagon(cx, cy);
        oct.setFill(playerColor);
        oct.setStroke(Color.BLACK);
        oct.setStrokeWidth(1.0);
        oct.setUserData("turnDisplay");
        boardPane.getChildren().add(oct);

        Polygon rho = renderer.makeRhombus(cx + 2 * BoardConfig.RADIUS, cy);
        rho.setFill(playerColor);
        rho.setStroke(Color.BLACK);
        rho.setStrokeWidth(1.0);
        rho.setUserData("turnDisplay");
        boardPane.getChildren().add(rho);
    }

    // Adds text that shows current player
    private void addTurnLabel() {
        String label = (board.isBlackTurn() ? "Black" : "White") + " to play";
        double x = BORDER + 4 * RADIUS + 10;
        double y = STRIP_Y + 10 + RADIUS + 5;

        Text text = new Text(label);
        text.setFill(Color.BLACK);
        text.setFont(Font.font("Arial", 16));
        text.setX(x);
        text.setY(y);
        text.setUserData("turnDisplay");
        boardPane.getChildren().add(text);
    }

    // Pie Button Methods
    private void addPieButton() {
        Button button = new Button("Activate Pie Rule");
        button.setLayoutX(BORDER + 4 * RADIUS + 220);
        button.setLayoutY(STRIP_Y + 20);
        button.setFont(Font.font("Arial", 16));
        button.setPrefHeight(40);
        button.setPrefWidth(250);
        button.setStyle("-fx-background-color: dodgerblue; -fx-text-fill: black;");
        button.setUserData("pieButton");
        button.setOnAction(e -> onPieRuleActivated());
        boardPane.getChildren().add(button);
    }

    // Helpers for Pie Button
    private void onPieRuleActivated() {
        boardPane.getChildren().removeIf(node -> "pieButton".equals(node.getUserData()));
        showPieRuleAlert();
        callbacks.onSwitchColor();
        callbacks.onPieRuleUsed();
        displayPlayerTurn();
        callbacks.onRunAnalysis();
    }

    private void showPieRuleAlert() {
        if (testMode) return;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Quax");
        alert.setHeaderText("Pie Rule:");
        alert.setContentText("Pie Rule used. Player 1 now plays WHITE");
        alert.showAndWait();
    }

    // Strategy Button Labels
    private void addStrategyButton() {
        Button btn = new Button("Show\nStrategy");
        btn.setLayoutX(TOTAL_SIZE / 2 - 50);
        btn.setLayoutY(STRIP_Y + 20);
        btn.setFont(Font.font("Arial", 12));
        btn.setPrefWidth(70);
        btn.setPrefHeight(40);
        btn.setWrapText(true);
        btn.setStyle("-fx-background-color: limegreen; -fx-text-fill: black;");
        btn.setUserData("strategyButton");
        btn.setOnAction(e -> callbacks.onShowStrategy());
        boardPane.getChildren().add(btn);
    }

    // Callbacks into GameController so it makes these changes to the actual game
    public interface Callbacks {
        void onShowStrategy();

        void onPieRuleUsed();

        void onSwitchColor();

        void onRunAnalysis();
    }

}
