package comp20050.qssboard.model;


public class Octagon implements Tile {

    private final int[] centre;          // {col, row}
    private final int radius;
    private final int[] directions;  // the 8 compass directions
    private final int[] cellTypes;
    private final QuaxBoard board;
    private TileState state;


    public Octagon(int col, int row, int radius, QuaxBoard board) {
        this.centre = new int[]{col, row};
        this.state = TileState.EMPTY;
        this.radius = radius;
        this.board = board;

        int numDirections = Direction.values().length; // 8

        directions = new int[numDirections];
        cellTypes = new int[numDirections];
    }

    // Getters / Setters
    public int getCol() {
        return centre[0];
    }

    public int getRow() {
        return centre[1];
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
        return "Oct(" + (char) ('A' + centre[0]) + (centre[1] + 1) + ")=" + state;
    }
}
