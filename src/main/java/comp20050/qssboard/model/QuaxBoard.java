package comp20050.qssboard.model;

import java.util.ArrayList;

public class QuaxBoard {
    private ArrayList<Octagon> ocells; //
    private ArrayList<Rhombus> rcells; //
    private ArrayList<Tile> blackTiles = new ArrayList<>();
    private ArrayList<Tile> whiteTiles = new ArrayList<>();
    private ArrayList<ArrayList<Tile>> blackChains = new ArrayList<>();
    private ArrayList<ArrayList<Tile>> whiteChains = new ArrayList<>();
    private final int sideSize;
    private boolean isBlackTurn = true;

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
}