package comp20050.qssboard.controller;

import comp20050.qssboard.model.MonteCarloSimulator;
import comp20050.qssboard.model.QuaxBoard;
import comp20050.qssboard.model.TileState;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.Collections;
import java.util.Map;


public class StrategyOverlay implements BoardConfig{

    private final Pane boardPane;
    private final QuaxBoard board;
    private final MonteCarloSimulator simulator;
    private final BoardRenderer renderer; // needed for getShapeForMove polygon lookup

    private TileState botColor;

    // Set by GameController after each bot analysis run
    private int    strategyMove    = Integer.MIN_VALUE;
    private double bestMoveWinRate = 0.0;

    protected StrategyOverlay(Pane boardPane, QuaxBoard board,
                           MonteCarloSimulator simulator, BoardRenderer renderer,
                           TileState botColor) {
        this.boardPane  = boardPane;
        this.board      = board;
        this.simulator  = simulator;
        this.renderer   = renderer;
        this.botColor    = botColor;
    }

    // Called by GameController once the bot has finished analysis
    protected void setStrategyMove(int move, double winRate) {
        this.strategyMove    = move;
        this.bestMoveWinRate = winRate;
    }

    protected void setBotColor(TileState botColor) {
        this.botColor = botColor;
    }


    protected void show() {
        // Remove any existing overlays from a previous strategy view
        boardPane.getChildren().removeIf(node -> "strategyHighlight".equals(node.getUserData()));

        // Score every empty tile by how well-connected it is to friendly pieces
        Map<Integer, Double> scores = simulator.getAllWinRates(board, botColor);
        if (scores.isEmpty()) return;

        double max   = Collections.max(scores.values());
        double min   = Collections.min(scores.values());
        double range = max - min;

        for (Map.Entry<Integer, Double> entry : scores.entrySet()) {
            int    move  = entry.getKey();
            double score = entry.getValue();

            double normalised = (range == 0) ? 0.5 : (score - min) / range;
            Color colour = interpolateHeatmap(normalised);

            Polygon shape = getShapeForMove(move);
            if (shape == null) continue;

            Polygon overlay = new Polygon();
            overlay.getPoints().addAll(shape.getPoints());
            overlay.setFill(colour.deriveColor(0, 1, 1, 0.5));
            overlay.setMouseTransparent(true);
            overlay.setUserData("strategyHighlight");

            // Mark the bot's actual MCTS-chosen move with a yellow border
            if (move == strategyMove) {
                overlay.setStroke(Color.YELLOW);
                overlay.setStrokeWidth(2.5);
            }

            boardPane.getChildren().add(overlay);
        }

        // Show the bot's win rate for its chosen move as a label below the board
        if (strategyMove != Integer.MIN_VALUE) {
            Polygon shape = getShapeForMove(strategyMove);
            if (shape != null) {
                Polygon marker = new Polygon();
                marker.getPoints().addAll(shape.getPoints());
                marker.setFill(Color.TRANSPARENT);
                marker.setStroke(Color.YELLOW);
                marker.setStrokeWidth(2.5);
                marker.setMouseTransparent(true);
                marker.setUserData("strategyHighlight");
                boardPane.getChildren().add(marker);
            }
            Text winText = new Text(String.format("Win rate: %.1f%%", bestMoveWinRate * 100));
            winText.setFill(Color.DARKGREEN);
            winText.setFont(Font.font("Arial", 14));
            winText.setX(TOTAL_SIZE / 2 + 80);
            winText.setY(TOTAL_SIZE + 45);
            winText.setUserData("strategyHighlight");
            boardPane.getChildren().add(winText);
        }

        // Swap to a Hide Strategy button
        boardPane.getChildren().removeIf(node -> "strategyButton".equals(node.getUserData()));
        Button hideBtn = new Button("Hide\nStrategy");
        hideBtn.setLayoutX(TOTAL_SIZE / 2 - 50);
        hideBtn.setLayoutY(TOTAL_SIZE + 20);
        hideBtn.setFont(Font.font("Arial", 12));
        hideBtn.setPrefWidth(70);
        hideBtn.setPrefHeight(40);
        hideBtn.setWrapText(true);
        hideBtn.setStyle("-fx-background-color: limegreen; -fx-text-fill: black;");
        hideBtn.setUserData("strategyButton");
        hideBtn.setOnAction(e -> hide());
        boardPane.getChildren().add(hideBtn);
    }

    protected void hide() {
        // Remove all overlays and the win rate label in one pass —
        // the real tile polygons underneath are unaffected since we never mutated them
        boardPane.getChildren().removeIf(node -> "strategyHighlight".equals(node.getUserData()));

        // Swap back to a Show Strategy button
        boardPane.getChildren().removeIf(node -> "strategyButton".equals(node.getUserData()));
        Button showBtn = new Button("Show\nStrategy");
        showBtn.setLayoutX(TOTAL_SIZE / 2 - 50);
        showBtn.setLayoutY(TOTAL_SIZE + 20);
        showBtn.setFont(Font.font("Arial", 12));
        showBtn.setPrefWidth(70);
        showBtn.setPrefHeight(40);
        showBtn.setWrapText(true);
        showBtn.setStyle("-fx-background-color: limegreen; -fx-text-fill: black;");
        showBtn.setUserData("strategyButton");
        showBtn.setOnAction(e -> show());
        boardPane.getChildren().add(showBtn);
    }

    //Helpers

    // Walks the boardPane to find the polygon whose userData matches the tile
    // for a given encoded move index
    private Polygon getShapeForMove(int move) {
        Object target;
        if (move >= 0) {
            target = board.getOctagon(move % 11, move / 11);
        } else {
            int ri = -(move + 1);
            target = board.getRhombus(ri % 10 + 0.5, ri / 10 + 0.5);
        }
        return boardPane.getChildren().stream()
                .filter(n -> n.getUserData() == target)
                .filter(n -> n instanceof Polygon)
                .map(n -> (Polygon) n)
                .findFirst()
                .orElse(null);
    }

    // Red (bad) → yellow → green (good)
    private Color interpolateHeatmap(double t) {
        if (t < 0.5) return Color.RED.interpolate(Color.YELLOW, t * 2);
        else         return Color.YELLOW.interpolate(Color.GREEN, (t - 0.5) * 2);
    }
}
