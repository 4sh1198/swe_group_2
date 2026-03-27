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
            Octagon oct = (Octagon) tile;
            int col = (int) oct.getCol();
            int row = (int) oct.getRow();

            // Mark current octagon as visited
            visitedOctagons[row][col] = true;

            // Check orthogonal neighbors (N/S/E/W)
            if (row > 0 && !visitedOctagons[row - 1][col]) {
                Octagon north = getOctagon(col, row - 1);
                if (north.getState() != TileState.EMPTY && north.getState() == tile.getState()) {
                    neighbors.add(north);
                }
            }
            if (row < CELLS - 1 && !visitedOctagons[row + 1][col]) {
                Octagon south = getOctagon(col, row + 1);
                if (south.getState() != TileState.EMPTY && south.getState() == tile.getState()) {
                    neighbors.add(south);
                }
            }
            if (col > 0 && !visitedOctagons[row][col - 1]) {
                Octagon west = getOctagon(col - 1, row);
                if (west.getState() != TileState.EMPTY && west.getState() == tile.getState()) {
                    neighbors.add(west);
                }
            }
            if (col < CELLS - 1 && !visitedOctagons[row][col + 1]) {
                Octagon east = getOctagon(col + 1, row);
                if (east.getState() != TileState.EMPTY && east.getState() == tile.getState()) {
                    neighbors.add(east);
                }
            }

            // Check diagonal neighbors (NE/SE/SW/NW)
            if (row > 0 && col > 0 && !visitedRhombuses[row - 1][col - 1]) {
                Rhombus nw = getRhombus(col - 0.5, row - 0.5);
                if (nw.getState() != TileState.EMPTY && nw.getState() == tile.getState()) {
                    neighbors.add(nw);
                }
            }
            if (row > 0 && col < CELLS - 1 && !visitedRhombuses[row - 1][col]) {
                Rhombus ne = getRhombus(col + 0.5, row - 0.5);
                if (ne.getState() != TileState.EMPTY && ne.getState() == tile.getState()) {
                    neighbors.add(ne);
                }
            }
            if (row < CELLS - 1 && col > 0 && !visitedRhombuses[row][col - 1]) {
                Rhombus sw = getRhombus(col - 0.5, row + 0.5);
                if (sw.getState() != TileState.EMPTY && sw.getState() == tile.getState()) {
                    neighbors.add(sw);
                }
            }
            if (row < CELLS - 1 && col < CELLS - 1 && !visitedRhombuses[row][col]) {
                Rhombus se = getRhombus(col + 0.5, row + 0.5);
                if (se.getState() != TileState.EMPTY && se.getState() == tile.getState()) {
                    neighbors.add(se);
                }
            }
        }
        else if (tile instanceof Rhombus) {
            Rhombus rho = (Rhombus) tile;
            int col = (int) (rho.getCol() * 2); // Convert 0.5 → 1, 1.5 → 3, etc.
            int row = (int) (rho.getRow() * 2);

            // Mark current rhombus as visited
            visitedRhombuses[row / 2][col / 2] = true;

            // Check diagonal neighbors (NE/SE/SW/NW)
            if (row > 1 && col > 1 && !visitedOctagons[row / 2 - 1][col / 2 - 1]) {
                Octagon nw = getOctagon(col / 2 - 1, row / 2 - 1);
                if (nw.getState() != TileState.EMPTY && nw.getState() == tile.getState()) {
                    neighbors.add(nw);
                }
            }
            if (row > 1 && col < CELLS * 2 - 1 && !visitedOctagons[row / 2 - 1][col / 2]) {
                Octagon ne = getOctagon(col / 2, row / 2 - 1);
                if (ne.getState() != TileState.EMPTY && ne.getState() == tile.getState()) {
                    neighbors.add(ne);
                }
            }
            if (row < CELLS * 2 - 1 && col > 1 && !visitedOctagons[row / 2][col / 2 - 1]) {
                Octagon sw = getOctagon(col / 2 - 1, row / 2);
                if (sw.getState() != TileState.EMPTY && sw.getState() == tile.getState()) {
                    neighbors.add(sw);
                }
            }
            if (row < CELLS * 2 - 1 && col < CELLS * 2 - 1 && !visitedOctagons[row / 2][col / 2]) {
                Octagon se = getOctagon(col / 2, row / 2);
                if (se.getState() != TileState.EMPTY && se.getState() == tile.getState()) {
                    neighbors.add(se);
                }
            }
        }

        return neighbors;
    }

    public boolean checkWin(TileState player) {
        // Reset visited arrays before each check
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

        if (player == TileState.BLACK) {
            // Check if BLACK has connected top and bottom edges
            Queue<Tile> queue = new LinkedList<>();

            // Start BFS from all bottom edge tiles of the player
            for (int c = 0; c < CELLS; c++) {
                Octagon oct = getOctagon(c, 0);
                if (oct.getState() == TileState.BLACK) {
                    queue.add(oct);
                    visitedOctagons[0][c] = true;
                }
            }
            for (int c = 0; c < CELLS - 1; c++) {
                Rhombus rho = getRhombus(c + 0.5, 0.5);
                if (rho.getState() == TileState.BLACK) {
                    queue.add(rho);
                    visitedRhombuses[0][c] = true;
                }
            }

            // If no starting tiles, can't win
            if (queue.isEmpty()) {
                return false;
            }

            // Perform BFS
            while (!queue.isEmpty()) {
                Tile current = queue.poll();
                ArrayList<Tile> neighbors = getNeighbors(current);

                for (Tile neighbor : neighbors) {
                    // Check if neighbor is on the top edge
                    if (neighbor instanceof Octagon) {
                        Octagon oct = (Octagon) neighbor;
                        if (oct.getRow() == CELLS - 1) {
                            return true;
                        }
                    } else if (neighbor instanceof Rhombus) {
                        Rhombus rho = (Rhombus) neighbor;
                        if (rho.getRow() == CELLS - 0.5) {
                            return true;
                        }
                    }
                    queue.add(neighbor);
                }
            }

            return false;
        }
        else if (player == TileState.WHITE) {
            // Check if WHITE has connected left and right edges
            Queue<Tile> queue = new LinkedList<>();

            // Start BFS from all left edge tiles of the player
            for (int r = 0; r < CELLS; r++) {
                Octagon oct = getOctagon(0, r);
                if (oct.getState() == TileState.WHITE) {
                    queue.add(oct);
                    visitedOctagons[r][0] = true;
                }
            }
            for (int r = 0; r < CELLS - 1; r++) {
                Rhombus rho = getRhombus(0.5, r + 0.5);
                if (rho.getState() == TileState.WHITE) {
                    queue.add(rho);
                    visitedRhombuses[r][0] = true;
                }
            }

            // If no starting tiles, can't win
            if (queue.isEmpty()) {
                return false;
            }

            // Perform BFS
            while (!queue.isEmpty()) {
                Tile current = queue.poll();
                ArrayList<Tile> neighbors = getNeighbors(current);

                for (Tile neighbor : neighbors) {
                    // Check if neighbor is on the right edge
                    if (neighbor instanceof Octagon) {
                        Octagon oct = (Octagon) neighbor;
                        if (oct.getCol() == CELLS - 1) {
                            return true;
                        }
                    } else if (neighbor instanceof Rhombus) {
                        Rhombus rho = (Rhombus) neighbor;
                        if (rho.getCol() == CELLS - 0.5) {
                            return true;
                        }
                    }
                    queue.add(neighbor);
                }
            }

            return false;
        }

        return false;
    }
}