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

public class BoardUI implements BoardConfig{

    private final Pane       boardPane;
    private final QuaxBoard  board;
    private final BoardRenderer renderer; // needed for makeOctagon/makeRhombus in displayPlayerTurn

    // Callbacks into GameController so it makes these changes to the actual game
    private final Runnable onShowStrategy; // called when Show Strategy button is pressed
    private final Runnable onPieRuleUsed;  // called when Pie Rule button is activated
    private final Runnable onSwitchColor;  // called to flip the active player colour
    private final Runnable onRunAnalysis;  // called to trigger bot analysis after pie rule

    // Mutable state synced from GameController after each move
    private int     moveCount   = 0;
    private boolean pieRuleUsed = false;
    private boolean botGame     = true;

    public BoardUI(Pane boardPane, QuaxBoard board, BoardRenderer renderer,
                   Runnable onShowStrategy, Runnable onPieRuleUsed,
                   Runnable onSwitchColor,  Runnable onRunAnalysis) {
        this.boardPane      = boardPane;
        this.board          = board;
        this.renderer       = renderer;
        this.onShowStrategy = onShowStrategy;
        this.onPieRuleUsed  = onPieRuleUsed;
        this.onSwitchColor  = onSwitchColor;
        this.onRunAnalysis  = onRunAnalysis;
    }

    // Called by GameController after each move to keep display in sync
    public void setMoveCount(int moveCount)   { this.moveCount   = moveCount; }
    public void setPieRuleUsed(boolean used)  { this.pieRuleUsed = used;      }
    public void setBotGame(boolean botGame)   { this.botGame     = botGame;   }


    public void updateWindowTitle() {
        if (botGame && boardPane.getScene() != null
                && boardPane.getScene().getWindow() instanceof Stage stage) {
            stage.setTitle("Quax (Human V Bot)");
        }
    }


    public void displayPlayerTurn() {
        boardPane.getChildren().removeIf(node -> "turnDisplay".equals(node.getUserData()));

        double yBottom = TOTAL_SIZE + 10;
        double xStart  = BORDER;

        // Grey background strip below the board
        Rectangle displayBg = new Rectangle(0, yBottom - 10, TOTAL_SIZE, 100);
        displayBg.setFill(Color.web("#D0D0D0"));
        displayBg.setUserData("turnDisplay");
        boardPane.getChildren().add(displayBg);

        // Small octagon and rhombus showing the current player's colour
        Color playerColor = board.isBlackTurn() ? Color.BLACK : Color.WHITE;

        Polygon oct = renderer.makeOctagon(xStart + RADIUS, yBottom + RADIUS);
        oct.setFill(playerColor);
        oct.setStroke(Color.BLACK);
        oct.setStrokeWidth(1.0);
        oct.setUserData("turnDisplay");
        boardPane.getChildren().add(oct);

        double rhombusX = xStart + 3 * RADIUS;
        double rhombusY = yBottom + RADIUS;
        Polygon rho = renderer.makeRhombus(rhombusX, rhombusY);
        rho.setFill(playerColor);
        rho.setStroke(Color.BLACK);
        rho.setStrokeWidth(1.0);
        rho.setUserData("turnDisplay");
        boardPane.getChildren().add(rho);

        Text turnText = new Text((board.isBlackTurn() ? "Black" : "White") + " to play");
        turnText.setFill(Color.BLACK);
        turnText.setFont(Font.font("Arial", 16));
        turnText.setX(rhombusX + RADIUS + 10);
        turnText.setY(rhombusY + 5);
        turnText.setUserData("turnDisplay");
        boardPane.getChildren().add(turnText);

        // Pie rule button only appears after move 1 and before it has been used
        if (moveCount == 1 && !pieRuleUsed) {
            createPieButton();
        }

        // Strategy button always resets to "Show" when the turn changes
        boardPane.getChildren().removeIf(node -> "strategyButton".equals(node.getUserData()));
        Button stratBtn = new Button("Show\nStrategy");
        stratBtn.setLayoutX(TOTAL_SIZE / 2 - 50);
        stratBtn.setLayoutY(TOTAL_SIZE + 20);
        stratBtn.setFont(Font.font("Arial", 12));
        stratBtn.setPrefWidth(70);
        stratBtn.setPrefHeight(40);
        stratBtn.setWrapText(true);
        stratBtn.setStyle("-fx-background-color: limegreen; -fx-text-fill: black;");
        stratBtn.setUserData("strategyButton");
        stratBtn.setOnAction(e -> onShowStrategy.run());
        boardPane.getChildren().add(stratBtn);
    }

    private void createPieButton() {
        Button button = new Button("Activate Pie Rule");
        button.setLayoutX(BORDER + (3 * RADIUS) + 250);
        button.setLayoutY(TOTAL_SIZE + 20);
        button.setFont(Font.font("Arial", 16));
        button.setPrefHeight(40);
        button.setPrefWidth(250);
        button.setStyle("-fx-background-color: dodgerblue; -fx-text-fill: black;");
        button.setUserData("pieButton");
        button.setOnAction(e -> {
            boardPane.getChildren().removeIf(node -> "pieButton".equals(node.getUserData()));

            // Show confirmation alert before delegating state changes to GameController
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Quax");
            alert.setHeaderText("Pie Rule:");
            alert.setContentText("Pie Rule used. Player 1 now plays WHITE");
            alert.showAndWait();

            // Let GameController handle the actual state mutation
            onSwitchColor.run();
            onPieRuleUsed.run();
            displayPlayerTurn();
            onRunAnalysis.run();
        });
        boardPane.getChildren().add(button);
    }

    public void displayWinner(TileState winner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Quax");
        alert.setHeaderText("Game Over!");
        alert.setContentText(winner == TileState.BLACK ? "Black wins!" : "White wins!");
        alert.showAndWait();
        // Note: gameOver flag is set in GameController, not here
    }
}
