package comp20050.qssboard.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class QuaxBoard {
    private static final int    CELLS   = 11;
    private ArrayList<Octagon> ocells; //
    private ArrayList<Rhombus> rcells; //
    private ArrayList<Tile> blackTiles = new ArrayList<>();
    private ArrayList<Tile> whiteTiles = new ArrayList<>();
    private ArrayList<ArrayList<Tile>> blackChains = new ArrayList<>();
    private ArrayList<ArrayList<Tile>> whiteChains = new ArrayList<>();
    private final int sideSize;
    private boolean isBlackTurn = true;
    private boolean[][] visitedOctagons = new boolean[CELLS][CELLS];
    private boolean[][] visitedRhombuses = new boolean[CELLS - 1][CELLS - 1];



    public QuaxBoard(int sideSize) {
        this.sideSize = sideSize;
        this.ocells = new ArrayList<>(); //
        this.rcells = new ArrayList<>(); //

        initializeBoard();
    }

    private void initializeBoard() {
        int radius = 30; // Standard size for the octagons

        // 1. Create Octagons at integer coordinates
        for (int r = 0; r < sideSize; r++) {
            for (int c = 0; c < sideSize; c++) {
                // We pass 'this' so the Octagon can call board methods later
                ocells.add(new Octagon(c, r, radius, this));
            }
        }

        // 2. Create Rhombuses at half-step coordinates
        for (int r = 0; r < sideSize - 1; r++) {
            for (int c = 0; c < sideSize - 1; c++) {
                // Rhombus at (0.5, 0.5) is indexed as (0, 0) logically in rcells
                rcells.add(new Rhombus(c + 0.5, r + 0.5, this));
            }
        }
    }

    // Needed for the neighbor methods to work
    public Octagon getOctagon(int col, int row) {
        if (col < 0 || col >= sideSize || row < 0 || row >= sideSize) return null;
        return ocells.get(row * sideSize + col);
    }

    public Rhombus getRhombus(double col, double row) {
        // Find the rhombus by matching its double coordinates
        for (Rhombus r : rcells) {
            if (r.getCentre()[0] == col && r.getCentre()[1] == row) return r;
        }
        return null;
    }

    public int getSize() { return this.sideSize; }
    public boolean isBlackTurn() { return this.isBlackTurn; }
    public void setBlackTurn(boolean b) { this.isBlackTurn = b; }

    // Helper: Returns all tiles on the bottom edge (row 0 for octagons, row 0.5 for rhombuses)
    public ArrayList<Tile> getBottomEdgeTiles() {
        ArrayList<Tile> edgeTiles = new ArrayList<>();
        // Octagons on bottom edge (row 0)
        for (int c = 0; c < CELLS; c++) {
            Octagon oct = getOctagon(c, 0);
            if (oct.getState() != TileState.EMPTY) {
                edgeTiles.add(oct);
            }
        }
        // Rhombuses on bottom edge (row 0.5)
        for (int c = 0; c < CELLS - 1; c++) {
            Rhombus rho = getRhombus(c + 0.5, 0.5);
            if (rho.getState() != TileState.EMPTY) {
                edgeTiles.add(rho);
            }
        }
        return edgeTiles;
    }

    // Helper: Returns all tiles on the top edge (row 10 for octagons, row 10.5 for rhombuses)
    public ArrayList<Tile> getTopEdgeTiles() {
        ArrayList<Tile> edgeTiles = new ArrayList<>();
        // Octagons on top edge (row 10)
        for (int c = 0; c < CELLS; c++) {
            Octagon oct = getOctagon(c, 10);
            if (oct.getState() != TileState.EMPTY) {
                edgeTiles.add(oct);
            }
        }
        // Rhombuses on top edge (row 10.5)
        for (int c = 0; c < CELLS - 1; c++) {
            Rhombus rho = getRhombus(c + 0.5, 10.5);
            if (rho.getState() != TileState.EMPTY) {
                edgeTiles.add(rho);
            }
        }
        return edgeTiles;
    }

    // Helper: Returns all tiles on the left edge (col 0 for octagons, col 0.5 for rhombuses)
    public ArrayList<Tile> getLeftEdgeTiles() {
        ArrayList<Tile> edgeTiles = new ArrayList<>();
        // Octagons on left edge (col 0)
        for (int r = 0; r < CELLS; r++) {
            Octagon oct = getOctagon(0, r);
            if (oct.getState() != TileState.EMPTY) {
                edgeTiles.add(oct);
            }
        }
        // Rhombuses on left edge (col 0.5)
        for (int r = 0; r < CELLS - 1; r++) {
            Rhombus rho = getRhombus(0.5, r + 0.5);
            if (rho.getState() != TileState.EMPTY) {
                edgeTiles.add(rho);
            }
        }
        return edgeTiles;
    }

    // Helper: Returns all tiles on the right edge (col 10 for octagons, col 10.5 for rhombuses)
    public ArrayList<Tile> getRightEdgeTiles() {
        ArrayList<Tile> edgeTiles = new ArrayList<>();
        // Octagons on right edge (col 10)
        for (int r = 0; r < CELLS; r++) {
            Octagon oct = getOctagon(10, r);
            if (oct.getState() != TileState.EMPTY) {
                edgeTiles.add(oct);
            }
        }
        // Rhombuses on right edge (col 10.5)
        for (int r = 0; r < CELLS - 1; r++) {
            Rhombus rho = getRhombus(10.5, r + 0.5);
            if (rho.getState() != TileState.EMPTY) {
                edgeTiles.add(rho);
            }
        }
        return edgeTiles;
    }

    // Helper: Checks if a tile is on the top/bottom edge (for BLACK)
    public boolean isOnTopOrBottomEdge(Tile tile) {
        if (tile instanceof Octagon) {
            Octagon oct = (Octagon) tile;
            return oct.getRow() == 0 || oct.getRow() == CELLS - 1;
        } else if (tile instanceof Rhombus) {
            Rhombus rho = (Rhombus) tile;
            return rho.getRow() == 0.5 || rho.getRow() == CELLS - 0.5;
        }
        return false;
    }

    // Helper: Checks if a tile is on the left/right edge (for WHITE)
    public boolean isOnLeftOrRightEdge(Tile tile) {
        if (tile instanceof Octagon) {
            Octagon oct = (Octagon) tile;
            return oct.getCol() == 0 || oct.getCol() == CELLS - 1;
        } else if (tile instanceof Rhombus) {
            Rhombus rho = (Rhombus) tile;
            return rho.getCol() == 0.5 || rho.getCol() == CELLS - 0.5;
        }
        return false;
    }

    private void resetVisitedArrays() {
        for (int r = 0; r < CELLS; r++) {
            for (int c = 0; c < CELLS; c++) {
                visitedOctagons[r][c] = false;
            }
        }
        for (int r = 0; r < CELLS - 1; r++) {
            for (int c = 0; c < CELLS - 1; c++) {
                visitedRhombuses[r][c] = false;
            }
        }
    }

    public ArrayList<Tile> getNeighbors(Tile tile) {
        ArrayList<Tile> neighbors = new ArrayList<>();

        if (tile instanceof Octagon) {
            int col = (int) ((Octagon) tile).getCol();
            int row = (int) ((Octagon) tile).getRow();

            // Orthogonal (N/S/E/W) octagon neighbors
            if (row > 0)         neighbors.add(getOctagon(col, row - 1));
            if (row < CELLS - 1) neighbors.add(getOctagon(col, row + 1));
            if (col > 0)         neighbors.add(getOctagon(col - 1, row));
            if (col < CELLS - 1) neighbors.add(getOctagon(col + 1, row));

            // Diagonal (NW/NE/SW/SE) rhombus neighbors
            if (row > 0 && col > 0)                 neighbors.add(getRhombus(col - 0.5, row - 0.5));
            if (row > 0 && col < CELLS - 1)         neighbors.add(getRhombus(col + 0.5, row - 0.5));
            if (row < CELLS - 1 && col > 0)         neighbors.add(getRhombus(col - 0.5, row + 0.5));
            if (row < CELLS - 1 && col < CELLS - 1) neighbors.add(getRhombus(col + 0.5, row + 0.5));

        } else if (tile instanceof Rhombus) {
            // (int) cast floors 0.5→0, 1.5→1, ..., 9.5→9 — the correct array index
            int c = (int) ((Rhombus) tile).getCol();
            int r = (int) ((Rhombus) tile).getRow();

            // All four surrounding octagons always exist — no bounds checks needed
            neighbors.add(getOctagon(c,     r    )); // NW
            neighbors.add(getOctagon(c + 1, r    )); // NE
            neighbors.add(getOctagon(c,     r + 1)); // SW
            neighbors.add(getOctagon(c + 1, r + 1)); // SE
        }

        return neighbors;
    }

    public boolean checkWin(TileState player) {
        boolean[][] visitedOct = new boolean[CELLS][CELLS];          // 11x11
        boolean[][] visitedRho = new boolean[CELLS - 1][CELLS - 1];  // 10x10
        Queue<Tile> queue = new LinkedList<>();

        if (player == TileState.BLACK) {
            // Seed from all BLACK tiles on the bottom row
            for (int c = 0; c < CELLS; c++) {
                Octagon oct = getOctagon(c, 0);
                if (oct.getState() == TileState.BLACK) {
                    visitedOct[0][c] = true;
                    queue.add(oct);
                }
            }

            while (!queue.isEmpty()) {
                Tile current = queue.poll();

                // Win: an octagon on the opposite (top) edge was reached
                if (current instanceof Octagon && ((Octagon) current).getRow() == CELLS - 1) {
                    return true;
                }

                for (Tile neighbor : getNeighbors(current)) {
                    if (neighbor.getState() != TileState.BLACK) continue;

                    if (neighbor instanceof Octagon) {
                        int r = (int) ((Octagon) neighbor).getRow();
                        int c = (int) ((Octagon) neighbor).getCol();
                        if (!visitedOct[r][c]) {
                            visitedOct[r][c] = true;
                            queue.add(neighbor);
                        }
                    } else {
                        int r = (int) ((Rhombus) neighbor).getRow();
                        int c = (int) ((Rhombus) neighbor).getCol();
                        if (!visitedRho[r][c]) {
                            visitedRho[r][c] = true;
                            queue.add(neighbor);
                        }
                    }
                }
            }

        } else if (player == TileState.WHITE) {
            // Seed from all WHITE tiles on the left column
            for (int r = 0; r < CELLS; r++) {
                Octagon oct = getOctagon(0, r);
                if (oct.getState() == TileState.WHITE) {
                    visitedOct[r][0] = true;
                    queue.add(oct);
                }
            }

            while (!queue.isEmpty()) {
                Tile current = queue.poll();

                // Win: an octagon on the opposite (right) edge was reached
                if (current instanceof Octagon && ((Octagon) current).getCol() == CELLS - 1) {
                    return true;
                }

                for (Tile neighbor : getNeighbors(current)) {
                    if (neighbor.getState() != TileState.WHITE) continue;

                    if (neighbor instanceof Octagon) {
                        int r = (int) ((Octagon) neighbor).getRow();
                        int c = (int) ((Octagon) neighbor).getCol();
                        if (!visitedOct[r][c]) {
                            visitedOct[r][c] = true;
                            queue.add(neighbor);
                        }
                    } else {
                        int r = (int) ((Rhombus) neighbor).getRow();
                        int c = (int) ((Rhombus) neighbor).getCol();
                        if (!visitedRho[r][c]) {
                            visitedRho[r][c] = true;
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        return false;
    }

    public QuaxBoard deepCopy() {
        QuaxBoard copy = new QuaxBoard(CELLS);

        for (int r = 0; r < CELLS; r++)
            for (int c = 0; c < CELLS; c++)
                copy.getOctagon(c, r).setState(this.getOctagon(c, r).getState());

        for (int r = 0; r < CELLS - 1; r++)
            for (int c = 0; c < CELLS - 1; c++)
                copy.getRhombus(c + 0.5, r + 0.5)
                        .setState(this.getRhombus(c + 0.5, r + 0.5).getState());

        copy.setBlackTurn(this.isBlackTurn());
        return copy;
    }

    public BitBoard toBitBoard() {
        BitBoard bb = new BitBoard();

        for (int r = 0; r < CELLS; r++)
            for (int c = 0; c < CELLS; c++)
                bb.setOctagon(c, r, toInt(getOctagon(c, r).getState()));

        for (int r = 0; r < CELLS - 1; r++)
            for (int c = 0; c < CELLS - 1; c++)
                bb.setRhombus(c, r, toInt(getRhombus(c + 0.5, r + 0.5).getState()));

        bb.setBlackTurn(isBlackTurn());
        return bb;
    }

    private int toInt(TileState s) {
        return switch (s) {
            case BLACK -> BitBoard.BLACK;
            case WHITE -> BitBoard.WHITE;
            default -> BitBoard.EMPTY;
        };
    }
}