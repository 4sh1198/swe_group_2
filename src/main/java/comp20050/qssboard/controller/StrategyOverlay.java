package comp20050.qssboard.controller;

import comp20050.qssboard.model.MonteCarloSimulator;
import comp20050.qssboard.model.QuaxBoard;
import comp20050.qssboard.model.TileState;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.Collections;
import java.util.Map;


public class StrategyOverlay implements BoardConfig {

    // Side panel dimensions
    private static final double PANEL_WIDTH = 260.0;
    private static final double PANEL_MARGIN = 14.0;

    private final Pane boardPane;
    private final QuaxBoard board;
    private final MonteCarloSimulator simulator;
    private final BoardRenderer renderer;

    private boolean isVisible = false;
    private TileState botColor;

    private int strategyMove = Integer.MIN_VALUE;
    private double bestMoveWinRate = 0.0;

    protected boolean isVisible() { return isVisible; }

    protected StrategyOverlay(Pane boardPane, QuaxBoard board,
                              MonteCarloSimulator simulator, BoardRenderer renderer,
                              TileState botColor) {
        this.boardPane = boardPane;
        this.board = board;
        this.simulator = simulator;
        this.renderer = renderer;
        this.botColor = botColor;
    }

    protected void setStrategyMove(int move, double winRate) {
        this.strategyMove = move;
        this.bestMoveWinRate = winRate;
    }

    protected void setBotColor(TileState botColor) {
        this.botColor = botColor;
    }

    protected void show() {
        isVisible = true;
        boardPane.getChildren().removeIf(node -> "strategyHighlight".equals(node.getUserData()));
        boardPane.getChildren().removeIf(node -> "strategyPanel".equals(node.getUserData()));

        // Heatmap overlays on board tiles
        Map<Integer, Double> scores = simulator.getAllWinRates(board, botColor);
        if (scores.isEmpty()) return;

        drawHeatmapOverlays(scores);
        drawSidePanel(scores);
        expandStage(TOTAL_SIZE + 10 + PANEL_WIDTH + 10);

        boardPane.getChildren().removeIf(node -> "strategyButton".equals(node.getUserData()));
        Button hideBtn = makeStrategyButton("Hide\nStrategy");
        hideBtn.setOnAction(e -> hide());
        boardPane.getChildren().add(hideBtn);

    }

    // Highlights tiles from green to red based on best move
    private void drawHeatmapOverlays(Map<Integer, Double> scores) {
        double max = Collections.max(scores.values());
        double min = Collections.min(scores.values());
        double range = max - min;

        for (Map.Entry<Integer, Double> entry : scores.entrySet()) {
            int move = entry.getKey();
            double score = entry.getValue();
            double normalised = (range == 0) ? 0.5 : (score - min) / range;

            Polygon shape = getShapeForMove(move);
            if (shape == null) continue;

            Polygon overlay = new Polygon();
            overlay.getPoints().addAll(shape.getPoints());
            overlay.setFill(interpolateHeatmap(normalised).deriveColor(0, 1, 1, 0.5));
            overlay.setMouseTransparent(true);
            overlay.setUserData("strategyHighlight");
            boardPane.getChildren().add(overlay);

            highlightLastBotMove();
        }
    }

    private void highlightLastBotMove() {
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
        }
    }

    // Carves out side panel for displaying strategy
    private void drawSidePanel(Map<Integer, Double> scores) {
        double panelX = TOTAL_SIZE;

        Rectangle panelBg = new Rectangle(panelX, 0, PANEL_WIDTH + 50, TOTAL_SIZE + 100);
        panelBg.setFill(Color.web("#D0D0D0"));
        panelBg.setUserData("strategyPanel");
        boardPane.getChildren().add(panelBg);

        double y = PANEL_MARGIN + 150;
        y = addStrategyDescription(panelX, y);
        y += 10;
        y = addWinRateSection(panelX, y);
        y += 10;
        // Heatmap
        y = addText("Heatmap Key For BOT's Next Move:", panelX + PANEL_MARGIN, y, 16, Color.BLACK);
        y = addText("GREEN TO RED => BEST TO WORST", panelX + PANEL_MARGIN, y, 14, BOARD_BG);
    }

    // Draw Panel Helpers
    private double addStrategyDescription(double panelX, double y) {
        y = addText("Algorithm: MCTS", panelX + PANEL_MARGIN, y, 30, Color.BLACK);
        y += 2;
        y = addWrappedText(
                "Monte Carlo Tree Search simulates thousands of random " +
                        "game continuations from each empty tile and scores " +
                        "each continuation by its win frequency." +
                        "The bot chooses the move with the best win rate" +
                        "and plays it.",
                panelX + PANEL_MARGIN, y, 18, BOARD_BG);
        return y;
    }

    private double addWinRateSection(double panelX, double y) {
        y = addText("Best Move Win Rate:", panelX + PANEL_MARGIN, y, 18, Color.BLACK);
        y += 2;
        if (strategyMove != Integer.MIN_VALUE) {
            String rateStr = String.format("%.1f%%", bestMoveWinRate * 100);
            Color rateColor = bestMoveWinRate >= 0.5 ? Color.DARKGREEN : Color.web("#8B0000");
            y = addText(rateStr, panelX + PANEL_MARGIN, y, 22, rateColor);
            y = addWrappedText(
                    "Win rate for the bot's chosen move (yellow border on board).",
                    panelX + PANEL_MARGIN, y, 16, BOARD_BG);
        } else {
            y = addWrappedText("Analysis not yet available.", panelX + PANEL_MARGIN, y, 18, BOARD_BG);
        }
        return y;
    }

    // Switching to hide strategy panel
    protected void hide() {
        isVisible = false;
        boardPane.getChildren().removeIf(node -> "strategyHighlight".equals(node.getUserData()));
        boardPane.getChildren().removeIf(node -> "strategyPanel".equals(node.getUserData()));

        expandStage(TOTAL_SIZE);

        boardPane.getChildren().removeIf(node -> "strategyButton".equals(node.getUserData()));
        Button showBtn = makeStrategyButton("Show\nStrategy");
        showBtn.setOnAction(e -> show());
        boardPane.getChildren().add(showBtn);
    }

    // Helpers Methods

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

    private Color interpolateHeatmap(double t) {
        if (t < 0.5) return Color.RED.interpolate(Color.YELLOW, t * 2);
        else return Color.YELLOW.interpolate(Color.GREEN, (t - 0.5) * 2);
    }

    private void expandStage(double newWidth) {
        if (boardPane.getScene() != null &&
                boardPane.getScene().getWindow() instanceof Stage stage) {
            stage.setWidth(newWidth + 16);
        }
    }

    // Makes buttons
    private Button makeStrategyButton(String label) {
        Button btn = new Button(label);
        btn.setLayoutX(TOTAL_SIZE / 2 - 50);
        btn.setLayoutY(TOTAL_SIZE + 20);
        btn.setFont(Font.font("Arial", 12));
        btn.setPrefWidth(70);
        btn.setPrefHeight(40);
        btn.setWrapText(true);
        btn.setStyle("-fx-background-color: limegreen; -fx-text-fill: black;");
        btn.setUserData("strategyButton");
        return btn;
    }

    // Adds a single line of text and returns the next Y position.
    private double addText(String content, double x, double y, double size, Color fill) {
        Text t = new Text(content);
        t.setX(x);
        t.setY(y);
        t.setFont(Font.font("Arial", size));
        t.setFill(fill);
        t.setUserData("strategyPanel");
        boardPane.getChildren().add(t);

        int lineCount = content.split("\n", -1).length;
        return y + size * 1.6 * lineCount;
    }

    // Helps display long lines of text without overflowing off the panel
    private double addWrappedText(String content, double x, double y, double fontSize, Color fill) {
        int charsPerLine = (int) ((PANEL_WIDTH - 2 * PANEL_MARGIN) / (fontSize * 0.40));
        String[] words = content.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (line.length() + word.length() + 1 > charsPerLine && !line.isEmpty()) {
                y = addText(line.toString().trim(), x, y, fontSize, fill);
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        if (!line.isEmpty()) {
            y = addText(line.toString().trim(), x, y, fontSize, fill);
        }
        return y + 4;
    }
}
