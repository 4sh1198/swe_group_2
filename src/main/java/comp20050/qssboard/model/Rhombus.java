package comp20050.qssboard.model;

import java.util.ArrayList;

/**
 * A rhombus cell on the Quax board, sitting in the diagonal gap between octagons.
 *
 * Neighbours are tracked via a directions ArrayList (size 8, indexed by
 * Direction.ordinal()). Rhombuses only ever touch octagons — in the four
 * diagonal directions NE, SE, SW, NW. All other slots remain null.
 *
 * The directions list allows callers to iterate over valid neighbour
 * directions without needing to know the internal storage structure.
 */
public class Rhombus {

    private final double[] centre;      // {col, row} — e.g. {0.5, 0.5}
    private TileState state;
    private QuaxBoard board;

    public Rhombus(double col, double row, QuaxBoard board) {
        this.centre = new double[]{col, row};
        this.state  = TileState.EMPTY;
        this.board = board;
    }

    // ── Neighbour access ──────────────────────────────────────────────────

    /**
     * Finds the Octagon neighbor.
     * If Rhombus is at (1.5, 1.5), its NW neighbor is (1, 1).
     */
    public Octagon octnbor(Direction d) {
        double oCol = this.centre[0];
        double oRow = this.centre[1];

        switch (d) {
            case NW -> { oCol -= 0.5; oRow -= 0.5; }
            case NE -> { oCol += 0.5; oRow -= 0.5; }
            case SW -> { oCol -= 0.5; oRow += 0.5; }
            case SE -> { oCol += 0.5; oRow += 0.5; }
            default -> { return null; }
        }
        return board.getOctagon((int)oCol, (int)oRow);
    }


    // ── Coordinate arithmetic ──────────────────────────────────────────────

    public Rhombus add(Rhombus other) {
        return new Rhombus(centre[0] + this.centre[0], centre[1] + this.centre[1], this.board);
    }

    public Rhombus subtract(Rhombus other) {
        return new Rhombus( centre[0] - this.centre[0], centre[1] - this.centre[1], this.board);
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public double   getCol()    { return centre[0]; }
    public double   getRow()    { return centre[1]; }
    public double[] getCentre() { return centre;    }

    public TileState getState()            { return state; }
    public void      setState(TileState s) { this.state = s; }

    public boolean isEmpty() { return state == TileState.EMPTY; }
    public boolean isBlack() { return state == TileState.BLACK; }
    public boolean isWhite() { return state == TileState.WHITE; }

    @Override
    public String toString() {
        return "Rho(" + centre[0] + "," + centre[1] + ")=" + state;
    }
}
