package comp20050.qssboard.model;

import java.util.ArrayList;

/**
 * An octagon cell on the Quax board.
 *
 * Neighbours:
 *   Orthogonal (N/S/E/W)   → other Octagons  via onbor()
 *   Diagonal   (NE/SE/SW/NW) → Rhombuses      via rnbor()
 *
 * Centre is stored as int[2] = {col, row}, matching the diagram.
 * add/subtract do vector arithmetic on centres for neighbour navigation.
 */
public class Octagon implements Tile {

    private final int[] centre;          // {col, row}
    private final int radius;
    private TileState state;
    private QuaxBoard board;

    private final int[] directions;  // the 8 compass directions
    private final int[]  cellTypes;


    public Octagon(int col, int row, int radius, QuaxBoard board) {
        this.centre = new int[]{col, row};
        this.state  = TileState.EMPTY;
        this.radius = radius;
        this.board = board;

        int numDirections = Direction.values().length; // 8

        directions    = new int[numDirections];
        cellTypes     = new int[numDirections];
    }

    // ── Neighbour access ─────────────────────────────────────────────────

    /**
     * Returns the octagon neighbour in direction d (N/S/E/W).
     * Returns null if this cell is on the board edge in that direction.
     */
    public Octagon onbor(Direction d) {
        double oCol = this.centre[0];
        double oRow = this.centre[1];

        switch (d) {
            case N -> { oRow++; }
            case E -> { oCol++; }
            case W -> { oCol--; }
            case S -> { oRow--; }
            default -> { return null; }
        }
        return board.getOctagon((int)oCol, (int)oRow);
    }

    /**
     * Returns the rhombus neighbour in direction d (NE/SE/SW/NW).
     * Returns null if this cell is on the board edge in that direction.
     */
    public Rhombus rnbor(Direction d) {

        double oCol = this.centre[0];
        double oRow = this.centre[1];

        switch (d) {
            case NW -> { oCol -= 0.5; oRow += 0.5; }
            case NE -> { oCol += 0.5; oRow += 0.5; }
            case SW -> { oCol -= 0.5; oRow -= 0.5; }
            case SE -> { oCol += 0.5; oRow -= 0.5; }
            default -> { return null; }
        }
        return board.getRhombus((int)oCol, (int)oRow);
    }

    // ── Coordinate arithmetic ─────────────────────────────────────────────

    public Octagon add(Octagon other) {
        return new Octagon(centre[0] + other.centre[0], centre[1] + other.centre[1], this.radius, this.board);
    }

    public Octagon subtract(Octagon other) {
        return new Octagon(centre[0] - other.centre[0], centre[1] - other.centre[1], this.radius,  this.board);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public int   getCol()    { return centre[0]; }
    public int   getRow()    { return centre[1]; }

    public TileState getState()            { return state; }
    public void      setState(TileState s) { this.state = s; }

    public boolean isEmpty() { return state == TileState.EMPTY; }

    @Override
    public String toString() {
        return "Oct(" + (char)('A' + centre[0]) + (centre[1] + 1) + ")=" + state;
    }
}
