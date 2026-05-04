package comp20050.qssboard.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class QuaxBoard {
    private static final int CELLS = 11;
    private final int sideSize;
    private final ArrayList<Octagon> octagons;
    private final ArrayList<Rhombus> rhombuses;
    private boolean isBlackTurn = true;


    public QuaxBoard(int sideSize) {
        this.sideSize = sideSize;
        this.octagons = new ArrayList<>();
        this.rhombuses = new ArrayList<>();

        initializeBoard();
    }

    private void initializeBoard() {
        int radius = 30; // Standard size for the octagons

        // Create Octagons at integer coordinates
        for (int r = 0; r < sideSize; r++) {
            for (int c = 0; c < sideSize; c++) {
                // We pass 'this' so the Octagon can call board methods later
                octagons.add(new Octagon(c, r, radius, this));
            }
        }

        // Create Rhombuses at half-step coordinates
        for (int r = 0; r < sideSize - 1; r++) {
            for (int c = 0; c < sideSize - 1; c++) {
                // Rhombus at (0.5, 0.5) is indexed as (0, 0) logically in rhombuses
                rhombuses.add(new Rhombus(c + 0.5, r + 0.5, this));
            }
        }
    }


    public Octagon getOctagon(int col, int row) {
        if (col < 0 || col >= sideSize || row < 0 || row >= sideSize) {
            throw new IllegalArgumentException(
                    "Octagon coordinates out of bounds: col=" + col + ", row=" + row +
                            " (board size=" + sideSize + ")"
            );
        }
        return octagons.get(row * sideSize + col);
    }

    public Rhombus getRhombus(double col, double row) {
        for (Rhombus r : rhombuses) {
            if (r.getCentre()[0] == col && r.getCentre()[1] == row) return r;
        }
        throw new IllegalArgumentException(
                "No rhombus found at coordinates: col=" + col + ", row=" + row
        );
    }

    public boolean isBlackTurn() {
        return this.isBlackTurn;
    }

    public void setBlackTurn(boolean b) {
        this.isBlackTurn = b;
    }

    public ArrayList<Tile> getNeighbours(Tile tile) {
        ArrayList<Tile> neighbours = new ArrayList<>();

        if (tile instanceof Octagon) {
            int col = ((Octagon) tile).getCol();
            int row = ((Octagon) tile).getRow();

            // Orthogonal (N/S/E/W) octagon neighbours
            if (row > 0) neighbours.add(getOctagon(col, row - 1));
            if (row < CELLS - 1) neighbours.add(getOctagon(col, row + 1));
            if (col > 0) neighbours.add(getOctagon(col - 1, row));
            if (col < CELLS - 1) neighbours.add(getOctagon(col + 1, row));

            // Diagonal (NW/NE/SW/SE) rhombus neighbours
            if (row > 0 && col > 0) neighbours.add(getRhombus(col - 0.5, row - 0.5));
            if (row > 0 && col < CELLS - 1) neighbours.add(getRhombus(col + 0.5, row - 0.5));
            if (row < CELLS - 1 && col > 0) neighbours.add(getRhombus(col - 0.5, row + 0.5));
            if (row < CELLS - 1 && col < CELLS - 1) neighbours.add(getRhombus(col + 0.5, row + 0.5));

        } else if (tile instanceof Rhombus) {
            // (int) cast floors 0.5→0, 1.5→1, ..., 9.5→9 — the correct array index
            int c = (int) ((Rhombus) tile).getCol();
            int r = (int) ((Rhombus) tile).getRow();

            // Rhombus indices are always 0..9, so surrounding octagons at 0..10 are always valid
            if (c < 0 || c >= sideSize - 1 || r < 0 || r >= sideSize - 1) {
                throw new IllegalStateException(
                        "Rhombus has invalid internal coordinates: col=" + c + ", row=" + r
                );
            }

            // All four surrounding octagons always exist — no bounds checks needed
            neighbours.add(getOctagon(c, r)); // NW
            neighbours.add(getOctagon(c + 1, r)); // NE
            neighbours.add(getOctagon(c, r + 1)); // SW
            neighbours.add(getOctagon(c + 1, r + 1)); // SE
        }

        return neighbours;
    }

    public boolean checkWin(TileState player) {
        boolean[][] visitedOct = new boolean[CELLS][CELLS];
        boolean[][] visitedRho = new boolean[CELLS - 1][CELLS - 1];
        Queue<Tile> queue = new LinkedList<>();

        seedStart(player, visitedOct, queue);

        while (!queue.isEmpty()) {
            Tile current = queue.poll();

            if (checkWinCondition(player, current)) return true;

            for (Tile neighbour : getNeighbours(current)) {
                if (neighbour.getState() != player) continue;

                if (neighbour instanceof Octagon oct) {
                    int r = oct.getRow(), c = oct.getCol();
                    if (!visitedOct[r][c]) {
                        visitedOct[r][c] = true;
                        queue.add(neighbour);
                    }
                } else {
                    Rhombus rho = (Rhombus) neighbour;
                    int r = (int) rho.getRow(), c = (int) rho.getCol();
                    if (!visitedRho[r][c]) {
                        visitedRho[r][c] = true;
                        queue.add(neighbour);
                    }
                }
            }
        }
        return false;
    }

    private boolean checkWinCondition(TileState player, Tile current) {
        if (current instanceof Octagon oct) {
            return (player == TileState.BLACK)
                    ? oct.getRow() == CELLS - 1
                    : oct.getCol() == CELLS - 1;
        }
        return false;
    }

    private void seedStart(TileState player, boolean[][] visitedOct, Queue<Tile> queue) {
        for (int i = 0; i < CELLS; i++) {
            Octagon oct = (player == TileState.BLACK) ? getOctagon(i, 0) : getOctagon(0, i);
            if (oct.getState() == player) {
                int r = oct.getRow(), c = oct.getCol();
                visitedOct[r][c] = true;
                queue.add(oct);
            }
        }
    }

    // Creates a copy of the game for Monte Carlo Simulator to use
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

    // Lets simulator create snapshot of the game
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