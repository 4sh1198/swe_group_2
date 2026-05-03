package comp20050.qssboard.model;

public class Rhombus implements Tile {

    private final double[] centre;      // {col, row} — e.g. {0.5, 0.5}
    private TileState state;
    private final QuaxBoard board;

    public Rhombus(double col, double row, QuaxBoard board) {
        this.centre = new double[]{col, row};
        this.state = TileState.EMPTY;
        this.board = board;
    }

    // Getters / Setters
    public double getCol() {
        return centre[0];
    }

    public double getRow() {
        return centre[1];
    }

    public double[] getCentre() {
        return centre;
    }

    public TileState getState() {
        return state;
    }

    public void setState(TileState s) {
        this.state = s;
    }

    public boolean isEmpty() {
        return state == TileState.EMPTY;
    }

    @Override
    public String toString() {
        return "Rho(" + (centre[0] + 1) + "," + (centre[1] + 1) + ")=" + state;
    }
}
