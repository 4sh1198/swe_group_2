package comp20050.qssboard.controller;

import comp20050.qssboard.model.QuaxBoard;
import comp20050.qssboard.model.Octagon;
import comp20050.qssboard.model.Rhombus;
import comp20050.qssboard.model.Tile;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class BoardRenderer implements BoardConfig{

    private final Pane boardPane;
    private final QuaxBoard board;

    // Callback fired when the user clicks any tile polygon
    private final TileClickHandler onTileClicked;

    @FunctionalInterface
    public interface TileClickHandler {
        void onClick(Tile tile, Polygon shape);
    }

    public BoardRenderer(Pane boardPane, QuaxBoard board, TileClickHandler onTileClicked) {
        this.boardPane     = boardPane;
        this.board         = board;
        this.onTileClicked = onTileClicked;
    }

    // Game state that BoardRenderer needs to read for display purposes
    private int       moveCount  = 0;
    private boolean   pieRuleUsed = false;
    private boolean   botGame    = true;

    // Main draw method

    protected void drawBoard() {
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
                double cx = BORDER + (c + 1) * STEP;
                double cy = BORDER + (CELLS - 1 - r) * STEP;

                Polygon rhoShape = makeRhombus(cx, cy);
                rhoShape.setFill(RHO_FILL);
                rhoShape.setStroke(RHO_STROKE);
                rhoShape.setStrokeWidth(1.0);
                rhoShape.setUserData(rho);
                rhoShape.setOnMouseClicked(e -> onTileClicked.onClick(rho, rhoShape));
                boardPane.getChildren().add(rhoShape);
            }
        }

        // 3. Draw octagons on top
        for (int r = 0; r < CELLS; r++) {
            for (int c = 0; c < CELLS; c++) {
                Octagon oct = board.getOctagon(c, r);
                double cx = BORDER + c * STEP + STEP / 2.0;
                double cy = BORDER + (CELLS - 1 - r) * STEP + STEP / 2.0;

                Polygon octShape = makeOctagon(cx, cy);
                octShape.setFill(OCT_FILL);
                octShape.setStroke(OCT_STROKE);
                octShape.setStrokeWidth(1.5);
                octShape.setUserData(oct);
                octShape.setOnMouseClicked(e -> onTileClicked.onClick(oct, octShape));
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
        String[] letters = {"A","B","C","D","E","F","G","H","I","J","K"};

        for (int col = 0; col < CELLS; col++) {
            double x = BORDER + col * STEP + STEP / 2.0;
            addLabel(letters[col], Color.WHITE, x, BORDER / 2 + 5);
            addLabel(letters[col], Color.WHITE, x, TOTAL_SIZE - BORDER / 2 + 5);
        }

        for (int row = 0; row < CELLS; row++) {
            int    number = CELLS - row;
            double y      = BORDER + row * STEP + STEP / 2.0 + 5;
            addLabel(String.valueOf(number), Color.BLACK, BORDER / 2,                    y);
            addLabel(String.valueOf(number), Color.BLACK, TOTAL_SIZE - BORDER / 2, y);
        }
    }

    // Shared helper to avoid duplicating label setup four times
    private void addLabel(String text, Color fill, double x, double y) {
        Text label = new Text(text);
        label.setFill(fill);
        label.setFont(Font.font("Arial", 14));
        label.setX(x - label.getBoundsInLocal().getWidth() / 2);
        label.setY(y);
        boardPane.getChildren().add(label);
    }

    /**
     * Creates a regular octagon centred at (cx, cy) with circumradius RADIUS.
     * The angle offset (2i+1)*π/8 orients it so flat edges are horizontal/vertical.
     */
    protected Polygon makeOctagon(double cx, double cy) {
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
    protected Polygon makeRhombus(double cx, double cy) {
        double rhoRadius = RADIUS * (Math.cos(Math.PI / 8) - Math.sin(Math.PI / 8));
        return new Polygon(
                cx,             cy - rhoRadius,
                cx + rhoRadius, cy,
                cx,             cy + rhoRadius,
                cx - rhoRadius, cy
        );
    }
}
