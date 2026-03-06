package comp20050.qssboard.controller;

import comp20050.qssboard.model.Octagon;
import comp20050.qssboard.model.QuaxBoard;
import comp20050.qssboard.model.Rhombus;
import comp20050.qssboard.model.TileState;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.control.Button;

public class GameController {

    // Board config
    private static final int    CELLS   = 11;
    private static final int    RADIUS  = 28;   // octagon circumradius in pixels
    private static final double BORDER  = 40.0; // border strip width

    // derived geometry
    private static final double STEP       = 2 * RADIUS * Math.cos(Math.PI / 8); // centre-to-centre
    private static final double BOARD_SIZE = CELLS * STEP;                        // full tiled area
    private static final double TOTAL_SIZE = BORDER + BOARD_SIZE + BORDER;

    // Colours
    private static final Color OCT_FILL    = Color.DARKORANGE;
    private static final Color OCT_STROKE  = Color.BLACK;
    private static final Color RHO_FILL    = Color.ORANGE;
    private static final Color RHO_STROKE  = Color.BLACK;
    private static final Color BOARD_BG    = Color.WHITE; // visible in the gaps at edges

    @FXML private Pane boardPane;

    private QuaxBoard board;
    private int moveCount = 0;
    private boolean pieRuleUsed = false;

    @FXML
    public void initialize() {
        board = new QuaxBoard(CELLS);
        drawBoard();
        displayPlayerTurn();
    }

    // Main draw method

    private void drawBoard() {
        boardPane.getChildren().clear();
        boardPane.setPrefSize(TOTAL_SIZE, TOTAL_SIZE);

        // 1. Board background (blue, shows through the gaps at edges)
        Rectangle boardBg = new Rectangle(BORDER, BORDER, BOARD_SIZE, BOARD_SIZE);
        boardBg.setFill(BOARD_BG);
        boardPane.getChildren().add(boardBg);

        // 2. Draw rhombuses first (they sit behind octagons visually)
        for (int r = 0; r < CELLS - 1; r++) {
            for (int c = 0; c < CELLS - 1; c++) {
                Rhombus rho = board.getRhombus(c + 0.5, r + 0.5);

                // Rhombus centre is halfway between four octagon centres
                double cx = BORDER + (c + 1) * STEP;   // = BORDER + c*STEP + STEP
                double cy = BORDER + (CELLS - 1 - r) * STEP;

                Polygon rhoShape = makeRhombus(cx, cy);
                rhoShape.setFill(RHO_FILL);
                rhoShape.setStroke(RHO_STROKE);
                rhoShape.setStrokeWidth(1.0);
                rhoShape.setUserData(rho);
                rhoShape.setOnMouseClicked(e -> onRhombusClicked(rho, rhoShape));
                boardPane.getChildren().add(rhoShape);
            }
        }

        // 3. Draw octagons on top
        for (int r = 0; r < CELLS; r++) {
            for (int c = 0; c < CELLS; c++) {
                Octagon oct = board.getOctagon(c, r);

                // Centre of octagon at grid position (c, r)
                double cx = BORDER + c * STEP + STEP / 2.0;
                double cy = BORDER + (CELLS - 1 - r) * STEP + STEP / 2.0;

                Polygon octShape = makeOctagon(cx, cy);
                octShape.setFill(OCT_FILL);
                octShape.setStroke(OCT_STROKE);
                octShape.setStrokeWidth(1.5);
                octShape.setUserData(oct);
                octShape.setOnMouseClicked(e -> onOctagonClicked(oct,octShape));
                boardPane.getChildren().add(octShape);
            }
        }

        // 4. Border strips (drawn on top so they cover any overshoot)
        drawBorders();

        // 5. Labels
        drawLabels();
    }

    // Border strips

    private void drawBorders() {
        // Top & bottom (dark)
        Rectangle topStrip = new Rectangle(0, 0, TOTAL_SIZE, BORDER);
        topStrip.setFill(Color.web("#2A2A2A"));
        boardPane.getChildren().add(topStrip);

        Rectangle bottomStrip = new Rectangle(0, TOTAL_SIZE - BORDER, TOTAL_SIZE, BORDER);
        bottomStrip.setFill(Color.web("#2A2A2A"));
        boardPane.getChildren().add(bottomStrip);

        // Left & right (grey)
        Rectangle leftStrip = new Rectangle(0, 0, BORDER, TOTAL_SIZE);
        leftStrip.setFill(Color.SADDLEBROWN);
        boardPane.getChildren().add(leftStrip);

        Rectangle rightStrip = new Rectangle(TOTAL_SIZE - BORDER, 0, BORDER, TOTAL_SIZE);
        rightStrip.setFill(Color.SADDLEBROWN);
        boardPane.getChildren().add(rightStrip);
    }

    // Column / Row labels

    private void drawLabels() {
        String[] letters = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K"};

        for (int col = 0; col < CELLS; col++) {
            // x = centre of each cell column
            double x = BORDER + col * STEP + STEP / 2.0;

            // Top label
            Text topLabel = new Text(letters[col]);
            topLabel.setFill(Color.WHITE);
            topLabel.setFont(Font.font("Arial", 14));
            topLabel.setX(x - topLabel.getBoundsInLocal().getWidth() / 2);
            topLabel.setY(BORDER / 2 + 5);
            boardPane.getChildren().add(topLabel);

            // Bottom label
            Text bottomLabel = new Text(letters[col]);
            bottomLabel.setFill(Color.WHITE);
            bottomLabel.setFont(Font.font("Arial", 14));
            bottomLabel.setX(x - bottomLabel.getBoundsInLocal().getWidth() / 2);
            bottomLabel.setY(TOTAL_SIZE - BORDER / 2 + 5);
            boardPane.getChildren().add(bottomLabel);
        }

        for (int row = 0; row < CELLS; row++) {
            int number = CELLS - row; // 11 at top, 1 at bottom
            double y = BORDER + row * STEP + STEP / 2.0 + 5;

            // Left label
            Text leftLabel = new Text(String.valueOf(number));
            leftLabel.setFill(Color.BLACK);
            leftLabel.setFont(Font.font("Arial", 14));
            leftLabel.setX(BORDER / 2 - leftLabel.getBoundsInLocal().getWidth() / 2);
            leftLabel.setY(y);
            boardPane.getChildren().add(leftLabel);

            // Right label
            Text rightLabel = new Text(String.valueOf(number));
            rightLabel.setFill(Color.BLACK);
            rightLabel.setFont(Font.font("Arial", 14));
            rightLabel.setX(TOTAL_SIZE - BORDER / 2 - rightLabel.getBoundsInLocal().getWidth() / 2);
            rightLabel.setY(y);
            boardPane.getChildren().add(rightLabel);
        }
    }

    private void displayPlayerTurn(){
        boardPane.getChildren().removeIf(node -> "turnDisplay".equals(node.getUserData()));

        double displayHeight = 100;
        double yBottom = TOTAL_SIZE + 10;
        double xStart = BORDER;

        Rectangle displayBg = new Rectangle(0, yBottom - 10, TOTAL_SIZE, displayHeight);
        displayBg.setFill(Color.web("#D0D0D0"));
        displayBg.setUserData("turnDisplay");
        boardPane.getChildren().add(displayBg);

        Color playerColor = board.isBlackTurn() ? Color.BLACK : Color.WHITE;

        Polygon Oct = makeOctagon(xStart + RADIUS, yBottom + RADIUS);
        Oct.setFill(playerColor);
        Oct.setStroke(Color.BLACK);
        Oct.setStrokeWidth(1.0);
        Oct.setUserData("turnDisplay");
        boardPane.getChildren().add(Oct);

        double rhombusX = xStart + 3 * RADIUS;
        double rhombusY = yBottom + RADIUS;
        Polygon Rho = makeRhombus(rhombusX, rhombusY);
        Rho.setFill(playerColor);
        Rho.setStroke(Color.BLACK);
        Rho.setStrokeWidth(1.0);
        Rho.setUserData("turnDisplay");
        boardPane.getChildren().add(Rho);

        Text turnText = new Text((board.isBlackTurn() ? "Black" : "White") + " to play");
        turnText.setFill(Color.BLACK);
        turnText.setFont(Font.font("Arial", 16));
        turnText.setX(rhombusX + RADIUS + 10);
        turnText.setY(rhombusY + 5);
        turnText.setUserData("turnDisplay");
        boardPane.getChildren().add(turnText);

        if(moveCount == 1){
            createPieButton();
        }

    }

    private void createPieButton() {
        double pieWidth = 250;
        double pieHeight = 40;

        double pieX = BORDER + (3 * RADIUS) + 250;
        double pieY = TOTAL_SIZE + 20;

        Button button = new Button("Activate Pie Rule");
        button.setLayoutX(pieX);
        button.setLayoutY(pieY);
        button.setFont(Font.font("Arial", 16));

        button.setPrefHeight(pieHeight);
        button.setPrefWidth(pieWidth);

        button.setStyle("-fx-background-color: dodgerblue; -fx-text-fill: black;");
        button.setUserData("pieButton");


        button.setOnAction(e -> {

            pieRuleUsed = true;
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Quax");
            alert.setHeaderText("Pie Rule:");
            alert.setContentText("Pie Rule used. Player 1 now plays WHITE");
            switchColor();
            // SR4: remove Pie Rule button after it is clicked
            boardPane.getChildren().removeIf(
                    node -> "pieButton".equals(node.getUserData())
            );
            displayPlayerTurn();
            alert.showAndWait();
        });
        boardPane.getChildren().add(button);
    }

    public void switchColor(){
        board.setBlackTurn(false);
    }
    // Shape factories

    /**
     * Creates a regular octagon centred at (cx, cy) with circumradius RADIUS.
     * The angle offset (2i+1)*π/8 orients it so flat edges are horizontal/vertical.
     */
    private Polygon makeOctagon(double cx, double cy) {
        Polygon polygon = new Polygon();
        for (int i = 0; i < 8; i++) {
            double angle = (2 * i + 1) * Math.PI / 8;
            polygon.getPoints().addAll(
                cx + RADIUS * Math.cos(angle),
                cy + RADIUS * Math.sin(angle)
            );
        }
        return polygon;
    }

    /**
     * Creates a rhombus (diamond) centred at (cx, cy) that exactly fills
     * the diagonal gap between four octagons.
     */
    private Polygon makeRhombus(double cx, double cy) {
        double rhoRadius = RADIUS * (Math.cos(Math.PI / 8) - Math.sin(Math.PI / 8));
        return new Polygon(
            cx,             cy - rhoRadius,  // top
            cx + rhoRadius, cy,              // right
            cx,             cy + rhoRadius,  // bottom
            cx - rhoRadius, cy               // left
        );
    }

    // Event handlers

    private void onOctagonClicked(Octagon oct, Polygon shape) throws IllegalArgumentException {
        if (oct.getState() != TileState.EMPTY) {
            throw new IllegalArgumentException("Tile is already occupied by a player");
        }

        // Update model
        TileState newState = board.isBlackTurn() ? TileState.BLACK : TileState.WHITE;
        oct.setState(newState);

        // Update view (colour)
        shape.setFill(newState == TileState.BLACK ? Color.BLACK : Color.WHITE);

        // Update chains

        System.out.println("Clicked: " + oct);
        moveCount++;

        if (moveCount > 1) {
            boardPane.getChildren().removeIf(node -> "pieButton".equals(node.getUserData()));
        }

        board.setBlackTurn(!board.isBlackTurn());
        displayPlayerTurn();
    }

    private void onRhombusClicked(Rhombus rho, Polygon shape) throws IllegalArgumentException {
        if (rho.getState() != TileState.EMPTY) {
            throw new IllegalArgumentException("Tile is already occupied by a player");
        }

        TileState newState = board.isBlackTurn() ? TileState.BLACK : TileState.WHITE;
        rho.setState(newState);

        shape.setFill(newState == TileState.BLACK ? Color.BLACK : Color.WHITE);

        System.out.println("Clicked: " + rho);
        moveCount++;

        if (moveCount > 1) {
            boardPane.getChildren().removeIf(node -> "pieButton".equals(node.getUserData()));
        }

        board.setBlackTurn(!board.isBlackTurn());
        displayPlayerTurn();
    }
}
