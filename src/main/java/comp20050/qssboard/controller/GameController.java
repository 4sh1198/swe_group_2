package comp20050.qssboard.controller;

import comp20050.qssboard.model.*;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Optional;

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
    private static final Color BOARD_BG    = Color.web("#564138"); // visible in the gaps at edges

    @FXML private Pane boardPane;

    private QuaxBoard board;
    private int moveCount = 0;

    private boolean pieRuleUsed = false;
    private boolean gameOver = false;

    private TileState humanColor = TileState.BLACK;
    private TileState botColor = TileState.WHITE;
    private boolean botGame = true;
    private boolean testMode = false;
    private boolean isAnalysing = false;
    private boolean strategyVisible = false;
    private int strategyMove = Integer.MIN_VALUE;
    private double bestMoveWinRate = 0.0;

    public void setTestMode(boolean testMode){
        this.testMode = testMode;
    }

    private MonteCarloSimulator simulator = new MonteCarloSimulator(7500000);

    @FXML
    public void initialize() {
        board = new QuaxBoard(CELLS);

        drawBoard();
        displayPlayerTurn();
        if (botColor == TileState.BLACK) {
            runAnalysis();
        }

        if(!testMode) {
            Platform.runLater(() -> {
                chooseBotColourAtStart();
                updateWindowTitle();
            });
        }

    }

    // ask the player what colour they want to play
    private void chooseBotColourAtStart() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quax");
        alert.setHeaderText("Human V Bot");
        alert.setContentText("Choose your colour:");

        ButtonType blackButton = new ButtonType("BLACK");
        ButtonType whiteButton = new ButtonType("WHITE");

        alert.getButtonTypes().setAll(blackButton, whiteButton);

        Optional<ButtonType> result = alert.showAndWait();

        applyChosenColor(result.orElse(null));


        // Show a confirmation popup so player knows their colour
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Quax");
        info.setHeaderText("Bot player added");
        info.setContentText("You are " + humanColor + ". Bot is " + botColor + ".");
        info.showAndWait();
    }

    void applyChosenColor(ButtonType choice){
        if (choice != null && "White".equals(choice.getText())) {
            humanColor = TileState.WHITE;
            botColor = TileState.BLACK;
        } else {
            humanColor = TileState.BLACK;
            botColor = TileState.WHITE;
        }
    }

    private void updateWindowTitle() {
        if (botGame && boardPane.getScene() != null && boardPane.getScene().getWindow() instanceof Stage stage) {
            stage.setTitle("Quax (Human V Bot)");
        }
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
        // Edge strips (excluding corners)
        Rectangle topStrip    = new Rectangle(BORDER, 0,   TOTAL_SIZE-2*BORDER, BORDER);
        Rectangle bottomStrip = new Rectangle(BORDER, TOTAL_SIZE-BORDER,  TOTAL_SIZE-2*BORDER, BORDER);
        Rectangle leftStrip   = new Rectangle(0, BORDER,   BORDER, TOTAL_SIZE-2*BORDER);
        Rectangle rightStrip  = new Rectangle(TOTAL_SIZE-BORDER, BORDER, BORDER, TOTAL_SIZE-2*BORDER);
        topStrip.setFill(Color.BLACK);    bottomStrip.setFill(Color.BLACK);
        leftStrip.setFill(Color.web("#F5F1E8"));   rightStrip.setFill(Color.web("#F5F1E8"));
        boardPane.getChildren().addAll(topStrip, bottomStrip, leftStrip, rightStrip);

        //Black and white corners for more intuitive UI
        // Top-left corner
        Polygon tlB = new Polygon(0.0, 0.0, BORDER, 0.0, BORDER, BORDER);   tlB.setFill(Color.BLACK);
        Polygon tlW = new Polygon(0.0, 0.0, 0.0, BORDER, BORDER, BORDER);   tlW.setFill(Color.web("#F5F1E8"));
        // Top-right corner
        Polygon trB = new Polygon(TOTAL_SIZE-BORDER, 0.0, TOTAL_SIZE, 0.0, TOTAL_SIZE-BORDER, BORDER); trB.setFill(Color.BLACK);
        Polygon trW = new Polygon(TOTAL_SIZE, 0.0, TOTAL_SIZE, BORDER, TOTAL_SIZE-BORDER, BORDER); trW.setFill(Color.web("#F5F1E8"));
        // Bottom-left corner
        Polygon blB = new Polygon(0.0, TOTAL_SIZE, BORDER, TOTAL_SIZE, BORDER, TOTAL_SIZE-BORDER); blB.setFill(Color.BLACK);
        Polygon blW = new Polygon(0.0, TOTAL_SIZE-BORDER, 0.0, TOTAL_SIZE, BORDER, TOTAL_SIZE-BORDER); blW.setFill(Color.web("#F5F1E8"));
        // Bottom-right corner
        Polygon brB = new Polygon(TOTAL_SIZE-BORDER, TOTAL_SIZE, TOTAL_SIZE, TOTAL_SIZE, TOTAL_SIZE-BORDER, TOTAL_SIZE-BORDER); brB.setFill(Color.BLACK);
        Polygon brW = new Polygon(TOTAL_SIZE, TOTAL_SIZE-BORDER, TOTAL_SIZE, TOTAL_SIZE, TOTAL_SIZE-BORDER, TOTAL_SIZE-BORDER); brW.setFill(Color.web("#F5F1E8"));

        boardPane.getChildren().addAll(tlB,tlW, trB,trW, blB,blW, brB,brW);
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

        if(moveCount == 1 && !pieRuleUsed){
            createPieButton();
        }

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
        stratBtn.setOnAction(e -> showStrategy());
        boardPane.getChildren().add(stratBtn);

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
            // SR4: remove Pie Rule button after it is clicked
            boardPane.getChildren().removeIf(
                    node -> "pieButton".equals(node.getUserData())
            );

            pieRuleUsed = true;
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Quax");
            alert.setHeaderText("Pie Rule:");
            alert.setContentText("Pie Rule used. Player 1 now plays WHITE");
            switchColor();

            displayPlayerTurn();
            alert.showAndWait();
            botColor = TileState.WHITE;
            runAnalysis();
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
        if (gameOver) return;
        if (isAnalysing) return;
        boardPane.getChildren().removeIf(node -> "strategyHighlight".equals(node.getUserData()));

        if (oct.getState() != TileState.EMPTY) {
            throw new IllegalArgumentException("Tile is already occupied by a player");
        }

        // Update model
        TileState newState = board.isBlackTurn() ? TileState.BLACK : TileState.WHITE;
        oct.setState(newState);

        // Update view (colour)
        shape.setFill(newState == TileState.BLACK ? Color.BLACK : Color.WHITE);


        System.out.println("Clicked: " + oct);
        moveCount++;

        if (moveCount > 1) {
            boardPane.getChildren().removeIf(node -> "pieButton".equals(node.getUserData()));
        }

        // Win checking
        if (board.checkWin(newState)) {
            displayWinner(newState);
            return;
        }

        board.setBlackTurn(!board.isBlackTurn());
        displayPlayerTurn();
        runAnalysis();
    }

    private void onRhombusClicked(Rhombus rho, Polygon shape) throws IllegalArgumentException {
        if (gameOver) return;
        if (isAnalysing) return;
        boardPane.getChildren().removeIf(node -> "strategyHighlight".equals(node.getUserData()));

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

        // Win checking
        if (board.checkWin(newState)) {
            displayWinner(newState);
            return;
        }

        board.setBlackTurn(!board.isBlackTurn());
        displayPlayerTurn();
        runAnalysis();
    }

    private void displayWinner(TileState winner) {
        gameOver = true;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Quax");
        alert.setHeaderText("Game Over!");
        alert.setContentText((winner == TileState.BLACK ? "Black wins!" : "White wins!"));
        alert.showAndWait();
    }

    private void runAnalysis() {
        if (gameOver) return;
        isAnalysing = true;

        // Only act if it's bot's turn
        TileState current = board.isBlackTurn() ? TileState.BLACK : TileState.WHITE;
        if (current != botColor) return;

        QuaxBoard snapshot = board.deepCopy();

        Thread t = new Thread(() -> {
            int best = simulator.bestMove(snapshot);
            double rate = simulator.lastBestWinRate;

            Platform.runLater(() -> {
                strategyMove = best;
                bestMoveWinRate = rate;
                playBotMove(best);
                isAnalysing = false;
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

    private void playBotMove(int move) {
        if (gameOver) return;
        boardPane.getChildren().removeIf(node -> "strategyHighlight".equals(node.getUserData()));

        if (move >= 0) {
            int col = move % 11;
            int row = move / 11;

            System.out.printf("BOT PLAYING: Octagon (%c,%d)%n", (char)('A' + col), row + 1);

            Octagon oct = board.getOctagon(col, row);

            if (oct.getState() != TileState.EMPTY) {
                System.out.println("ERROR: Octagon already occupied!");
                return;
            }

            TileState state = botColor;
            oct.setState(state);

            boardPane.getChildren().stream()
                    .filter(n -> n.getUserData() == oct)
                    .findFirst()
                    .ifPresent(n -> ((Polygon) n).setFill(
                            state == TileState.BLACK ? Color.BLACK : Color.WHITE
                    ));

            moveCount++;

            if (board.checkWin(state)) {
                displayWinner(state);
                return;
            }

        } else {
            int ri = -(move + 1);
            int col = ri % 10;
            int row = ri / 10;

            System.out.printf("BOT PLAYING: Rhombus (%d,%d)%n", col + 1, row + 1);

            Rhombus rho = board.getRhombus(col + 0.5, row + 0.5);

            if (rho.getState() != TileState.EMPTY) {
                System.out.println("ERROR: Rhombus already occupied!");
                return;
            }

            TileState state = botColor;
            rho.setState(state);

            boardPane.getChildren().stream()
                    .filter(n -> n.getUserData() == rho)
                    .findFirst()
                    .ifPresent(n -> ((Polygon) n).setFill(
                            state == TileState.BLACK ? Color.BLACK : Color.WHITE
                    ));

            moveCount++;

            if (board.checkWin(state)) {
                displayWinner(state);
                return;
            }
        }

        board.setBlackTurn(!board.isBlackTurn());
        displayPlayerTurn();
    }


    private void showStrategy() {
        if (strategyMove == Integer.MIN_VALUE) return;

        // clear any previous highlight first
        boardPane.getChildren().removeIf(node -> "strategyHighlight".equals(node.getUserData()));

        // restore ALL tiles to their correct colours first
        for (javafx.scene.Node n : boardPane.getChildren()) {
            if (n instanceof Polygon) {
                Object ud = n.getUserData();
                if (ud instanceof Octagon oct) {
                    TileState s = oct.getState();
                    ((Polygon) n).setFill(s == TileState.BLACK ? Color.BLACK :
                            s == TileState.WHITE ? Color.WHITE : OCT_FILL);
                } else if (ud instanceof Rhombus rho) {
                    TileState s = rho.getState();
                    ((Polygon) n).setFill(s == TileState.BLACK ? Color.BLACK :
                            s == TileState.WHITE ? Color.WHITE : RHO_FILL);
                }
            }
        }

        // now highlight just the one best move tile
        if (strategyMove >= 0) {
            int col = strategyMove % 11;
            int row = strategyMove / 11;
            Octagon oct = board.getOctagon(col, row);
            for (javafx.scene.Node n : boardPane.getChildren()) {
                if (n instanceof Polygon && n.getUserData() == oct) {
                    ((Polygon) n).setFill(Color.LIMEGREEN);
                    break;
                }
            }
        } else {
            int ri = -(strategyMove + 1);
            Rhombus rho = board.getRhombus(ri % 10 + 0.5, ri / 10 + 0.5);
            for (javafx.scene.Node n : boardPane.getChildren()) {
                if (n instanceof Polygon && n.getUserData() == rho) {
                    ((Polygon) n).setFill(Color.LIMEGREEN);
                    break;
                }
            }
        }

        // show win rate text
        Text winText = new Text(String.format("Win rate: %.1f%%", bestMoveWinRate * 100));
        winText.setFill(Color.DARKGREEN);
        winText.setFont(Font.font("Arial", 14));
        winText.setX(TOTAL_SIZE / 2 + 80);
        winText.setY(TOTAL_SIZE + 45);
        winText.setUserData("strategyHighlight");
        boardPane.getChildren().add(winText);

        // swap button to Hide Strategy
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
        hideBtn.setOnAction(e -> hideStrategy());
        boardPane.getChildren().add(hideBtn);
    }

    private void hideStrategy() {
        // remove win rate text
        boardPane.getChildren().removeIf(node -> "strategyHighlight".equals(node.getUserData()));

        // restore correct colour of the highlighted tile
        if (strategyMove != Integer.MIN_VALUE) {
            if (strategyMove >= 0) {
                int col = strategyMove % 11;
                int row = strategyMove / 11;
                Octagon oct = board.getOctagon(col, row);
                for (javafx.scene.Node n : boardPane.getChildren()) {
                    if (n instanceof Polygon && n.getUserData() == oct) {
                        TileState s = oct.getState();
                        ((Polygon) n).setFill(s == TileState.BLACK ? Color.BLACK :
                                s == TileState.WHITE ? Color.WHITE : OCT_FILL);
                        break;
                    }
                }
            } else {
                int ri = -(strategyMove + 1);
                Rhombus rho = board.getRhombus(ri % 10 + 0.5, ri / 10 + 0.5);
                for (javafx.scene.Node n : boardPane.getChildren()) {
                    if (n instanceof Polygon && n.getUserData() == rho) {
                        TileState s = rho.getState();
                        ((Polygon) n).setFill(s == TileState.BLACK ? Color.BLACK :
                                s == TileState.WHITE ? Color.WHITE : RHO_FILL);
                        break;
                    }
                }
            }
        }

        // swap button back to Show Strategy
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
        showBtn.setOnAction(e -> showStrategy());
        boardPane.getChildren().add(showBtn);
    }
}
