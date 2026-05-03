package comp20050.qssboard.model;

import java.util.ArrayList;
import java.util.List;

public class BitBoard {

    public static final int EMPTY = 0b00;
    public static final int BLACK = 0b01;
    public static final int WHITE = 0b10;

    // Octagons: bits 0–241   (121 tiles × 2 bits)
    // Rhombuses: bits 242–441 (100 tiles × 2 bits)
    // Total: 442 bits → 7 longs (448 bits, 6 spare)
    private static final int OCT_OFFSET = 0;
    private static final int RHO_OFFSET = 242;

    private final long[] bits = new long[7];
    private boolean blackTurn;

    // Octagon access — col 0..10, row 0..10
    public int getOctagon(int col, int row) {
        return getBits(OCT_OFFSET + (row * 11 + col) * 2);
    }

    public void setOctagon(int col, int row, int state) {
        setBits(OCT_OFFSET + (row * 11 + col) * 2, state);
    }

    // Rhombus access — col 0..9, row 0..9
    // These are the integer-floored indices of QuaxBoard's half-integer coords.
    // getRhombus(c, r) here corresponds to QuaxBoard.getRhombus(c+0.5, r+0.5).
    public int getRhombus(int col, int row) {
        return getBits(RHO_OFFSET + (row * 10 + col) * 2);
    }

    public void setRhombus(int col, int row, int state) {
        setBits(RHO_OFFSET + (row * 10 + col) * 2, state);
    }

    // Turn
    public boolean isBlackTurn() {
        return blackTurn;
    }

    public void setBlackTurn(boolean b) {
        blackTurn = b;
    }

    // Core bit operations
    // All bit indices are (tile_index * 2), so always even.
    // Even offset % 64 is always in 0,2,4,...,62 — never 63.
    // A 2-bit value therefore never straddles a long boundary.
    private int getBits(int bitIndex) {
        int li = bitIndex / 64;
        int off = bitIndex % 64;
        return (int) ((bits[li] >>> off) & 0b11L);
    }

    private void setBits(int bitIndex, int state) {
        int li = bitIndex / 64;
        int off = bitIndex % 64;
        bits[li] &= ~(0b11L << off);
        bits[li] |= ((long) state << off);
    }

    // Copy copies the board: important for Monte Carlo space optimisation
    public BitBoard copy() {
        BitBoard b = new BitBoard();
        System.arraycopy(this.bits, 0, b.bits, 0, 7);
        b.blackTurn = this.blackTurn;
        return b;
    }

    // Mirrors QuaxBoard.getNeighbors() exactly. Encoded as {type, col, row} where type 0=octagon, 1=rhombus.
    protected List<int[]> getNeighbours(int type, int c, int r) {
        List<int[]> neighbours = new ArrayList<>();

        if (type == 0) {
            // Octagon at (c, r)

            // 4 orthogonal octagon neighbours (N/S/E/W)
            if (r > 0) neighbours.add(new int[]{0, c, r - 1});
            if (r < 10) neighbours.add(new int[]{0, c, r + 1});
            if (c > 0) neighbours.add(new int[]{0, c - 1, r});
            if (c < 10) neighbours.add(new int[]{0, c + 1, r});

            // 4 diagonal rhombus neighbours (NW/NE/SW/SE)
            // QuaxBoard: getRhombus(col-0.5, row-0.5) → BitBoard index (c-1, r-1)
            // QuaxBoard: getRhombus(col+0.5, row-0.5) → BitBoard index (c,   r-1)
            // QuaxBoard: getRhombus(col-0.5, row+0.5) → BitBoard index (c-1, r  )
            // QuaxBoard: getRhombus(col+0.5, row+0.5) → BitBoard index (c,   r  )
            if (r > 0 && c > 0) neighbours.add(new int[]{1, c - 1, r - 1});
            if (r > 0 && c < 10) neighbours.add(new int[]{1, c, r - 1});
            if (r < 10 && c > 0) neighbours.add(new int[]{1, c - 1, r});
            if (r < 10 && c < 10) neighbours.add(new int[]{1, c, r});

        } else {
            // Rhombus at index (c, r) — half-coords (c+0.5, r+0.5)
            // 4 surrounding octagons — bounds always valid since c in 0..9, r in 0..9
            // QuaxBoard: getOctagon(c, r), getOctagon(c+1, r), getOctagon(c, r+1), getOctagon(c+1, r+1)
            neighbours.add(new int[]{0, c, r});
            neighbours.add(new int[]{0, c + 1, r});
            neighbours.add(new int[]{0, c, r + 1});
            neighbours.add(new int[]{0, c + 1, r + 1});
        }

        return neighbours;
    }
}
