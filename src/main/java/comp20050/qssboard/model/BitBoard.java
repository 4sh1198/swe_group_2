package comp20050.qssboard.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

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

    // -------------------------------------------------------------------------
    // Octagon access — col 0..10, row 0..10
    // -------------------------------------------------------------------------

    public int getOctagon(int col, int row) {
        return getBits(OCT_OFFSET + (row * 11 + col) * 2);
    }

    public void setOctagon(int col, int row, int state) {
        setBits(OCT_OFFSET + (row * 11 + col) * 2, state);
    }

    // -------------------------------------------------------------------------
    // Rhombus access — col 0..9, row 0..9
    // These are the integer-floored indices of QuaxBoard's half-integer coords.
    // getRhombus(c, r) here corresponds to QuaxBoard.getRhombus(c+0.5, r+0.5).
    // -------------------------------------------------------------------------

    public int getRhombus(int col, int row) {
        return getBits(RHO_OFFSET + (row * 10 + col) * 2);
    }

    public void setRhombus(int col, int row, int state) {
        setBits(RHO_OFFSET + (row * 10 + col) * 2, state);
    }

    // -------------------------------------------------------------------------
    // Turn
    // -------------------------------------------------------------------------

    public boolean isBlackTurn() { return blackTurn; }
    public void setBlackTurn(boolean b) { blackTurn = b; }

    // -------------------------------------------------------------------------
    // Core bit operations
    //
    // All bit indices are (tile_index * 2), so always even.
    // Even offset % 64 is always in 0,2,4,...,62 — never 63.
    // A 2-bit value therefore never straddles a long boundary.
    // -------------------------------------------------------------------------

    private int getBits(int bitIndex) {
        int li  = bitIndex / 64;
        int off = bitIndex % 64;
        return (int)((bits[li] >>> off) & 0b11L);
    }

    private void setBits(int bitIndex, int state) {
        int li  = bitIndex / 64;
        int off = bitIndex % 64;
        bits[li] &= ~(0b11L << off);
        bits[li] |=  ((long) state << off);
    }

    // -------------------------------------------------------------------------
    // Copy — the entire reason this class exists for Monte Carlo
    // -------------------------------------------------------------------------

    public BitBoard copy() {
        BitBoard b = new BitBoard();
        System.arraycopy(this.bits, 0, b.bits, 0, 7);
        b.blackTurn = this.blackTurn;
        return b;
    }

    // -------------------------------------------------------------------------
    // Win check — mirrors QuaxBoard.checkWin exactly:
    //   - Seeds only from octagon edges (row 0 for BLACK, col 0 for WHITE)
    //   - Win condition only on opposite octagon edge (row 10 / col 10)
    //   - Rhombuses are intermediate BFS nodes, never seeds or win conditions
    // -------------------------------------------------------------------------

    public boolean checkWin(int player) {
        boolean[][] visitedOct = new boolean[11][11];
        boolean[][] visitedRho = new boolean[10][10];
        Queue<int[]> queue = new ArrayDeque<>();

        if (player == BLACK) {
            for (int c = 0; c < 11; c++) {
                if (getOctagon(c, 0) == BLACK && !visitedOct[0][c]) {
                    visitedOct[0][c] = true;
                    queue.add(new int[]{0, c, 0});
                }
            }
        } else {
            for (int r = 0; r < 11; r++) {
                if (getOctagon(0, r) == WHITE && !visitedOct[r][0]) {
                    visitedOct[r][0] = true;
                    queue.add(new int[]{0, 0, r});
                }
            }
        }

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int type = current[0], c = current[1], r = current[2];

            // Only octagons trigger the win condition
            if (type == 0) {
                if (player == BLACK && r == 10) return true;
                if (player == WHITE && c == 10) return true;
            }

            for (int[] n : getNeighbours(type, c, r)) {
                int nt = n[0], nc = n[1], nr = n[2];
                int state = (nt == 0) ? getOctagon(nc, nr) : getRhombus(nc, nr);
                if (state != player) continue;

                if (nt == 0) {
                    if (!visitedOct[nr][nc]) {
                        visitedOct[nr][nc] = true;
                        queue.add(n);
                    }
                } else {
                    if (!visitedRho[nr][nc]) {
                        visitedRho[nr][nc] = true;
                        queue.add(n);
                    }
                }
            }
        }

        return false;
    }

    // Mirrors QuaxBoard.getNeighbors() exactly.
    // Encoded as {type, col, row} where type 0=octagon, 1=rhombus.
    private List<int[]> getNeighbours(int type, int c, int r) {
        List<int[]> neighbours = new ArrayList<>();

        if (type == 0) {
            // Octagon at (c, r)

            // 4 orthogonal octagon neighbours (N/S/E/W)
            if (r > 0)  neighbours.add(new int[]{0, c, r - 1});
            if (r < 10) neighbours.add(new int[]{0, c, r + 1});
            if (c > 0)  neighbours.add(new int[]{0, c - 1, r});
            if (c < 10) neighbours.add(new int[]{0, c + 1, r});

            // 4 diagonal rhombus neighbours (NW/NE/SW/SE)
            // QuaxBoard: getRhombus(col-0.5, row-0.5) → BitBoard index (c-1, r-1)
            // QuaxBoard: getRhombus(col+0.5, row-0.5) → BitBoard index (c,   r-1)
            // QuaxBoard: getRhombus(col-0.5, row+0.5) → BitBoard index (c-1, r  )
            // QuaxBoard: getRhombus(col+0.5, row+0.5) → BitBoard index (c,   r  )
            if (r > 0 && c > 0)   neighbours.add(new int[]{1, c - 1, r - 1});
            if (r > 0 && c < 10)  neighbours.add(new int[]{1, c, r - 1});
            if (r < 10 && c > 0)  neighbours.add(new int[]{1, c - 1, r});
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