package comp20050.qssboard.model;

import java.util.*;
import java.util.concurrent.*;

public class MonteCarloSimulator {

    private final int simulations;
    public double lastBestWinRate = 0.0;

    // Index layout for Union-Find
    // Octagons:  r*11 + c         →   0..120
    // Rhombuses: 121 + r*10 + c   → 121..220
    // Virtual:   BLACK_START=221, BLACK_END=222, WHITE_START=223, WHITE_END=224
    private static final int OCT_OFFSET   = 0;
    private static final int RHO_OFFSET   = 121;
    private static final int BLACK_START  = 221;
    private static final int BLACK_END    = 222;
    private static final int WHITE_START  = 223;
    private static final int WHITE_END    = 224;
    private static final int UF_SIZE      = 225;

    public MonteCarloSimulator(int simulations) {
        this.simulations = simulations;
    }

    // Union-Find
    // It's faster than BFS, so used here to simulate millions of games in good time

    static class UnionFind {
        private final int[] parent, rank;

        UnionFind(int size) {
            parent = new int[size];
            rank   = new int[size];
            for (int i = 0; i < size; i++) parent[i] = i;
        }

        int find(int x) {
            if (parent[x] != x) parent[x] = find(parent[x]); // path compression
            return parent[x];
        }

        void union(int a, int b) {
            a = find(a); b = find(b);
            if (a == b) return;
            if (rank[a] < rank[b]) {
                int t = a;
                a = b;
                b = t;
            }
            parent[b] = a;
            if (rank[a] == rank[b]) rank[a]++;
        }

        boolean connected(int a, int b) {
            return find(a) == find(b);
        }
    }

    // Build a Union-Find from the current board state

    private UnionFind[] buildUF(BitBoard bb) {
        UnionFind ufBlack = new UnionFind(UF_SIZE);
        UnionFind ufWhite = new UnionFind(UF_SIZE);

        for (int r = 0; r < 11; r++) {
            for (int c = 0; c < 11; c++) {
                int state = bb.getOctagon(c, r);
                if (state == BitBoard.BLACK) unionOct(bb, ufBlack, c, r, BitBoard.BLACK);
                if (state == BitBoard.WHITE) unionOct(bb, ufWhite, c, r, BitBoard.WHITE);
            }
        }
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                int state = bb.getRhombus(c, r);
                if (state == BitBoard.BLACK) unionRho(bb, ufBlack, c, r, BitBoard.BLACK);
                if (state == BitBoard.WHITE) unionRho(bb, ufWhite, c, r, BitBoard.WHITE);
            }
        }

        return new UnionFind[]{ufBlack, ufWhite};
    }

    // Union helpers
    private void unionOct(BitBoard bb, UnionFind uf, int c, int r, int player) {
        int id = OCT_OFFSET + r * 11 + c;

        // Connect to virtual edge nodes
        if (player == BitBoard.BLACK) {
            if (r == 0)  uf.union(id, BLACK_START);
            if (r == 10) uf.union(id, BLACK_END);
        } else {
            if (c == 0)  uf.union(id, WHITE_START);
            if (c == 10) uf.union(id, WHITE_END);
        }

        // Union with same-player occupied neighbours
        for (int[] n : bb.getNeighbours(0, c, r)) {
            int nState = (n[0] == 0) ? bb.getOctagon(n[1], n[2]) : bb.getRhombus(n[1], n[2]);
            if (nState != player) continue;
            int nId = (n[0] == 0)
                    ? OCT_OFFSET + n[2] * 11 + n[1]
                    : RHO_OFFSET + n[2] * 10 + n[1];
            uf.union(id, nId);
        }
    }

    private void unionRho(BitBoard bb, UnionFind uf, int c, int r, int player) {
        int id = RHO_OFFSET + r * 10 + c;

        for (int[] n : bb.getNeighbours(1, c, r)) {
            int nState = (n[0] == 0) ? bb.getOctagon(n[1], n[2]) : bb.getRhombus(n[1], n[2]);
            if (nState != player) continue;
            int nId = (n[0] == 0)
                    ? OCT_OFFSET + n[2] * 11 + n[1]
                    : RHO_OFFSET + n[2] * 10 + n[1];
            uf.union(id, nId);
        }
    }

    /**
     * Returns the index of the best move found.
     * Positive = octagon (decode: col = idx%11, row = idx/11)
     * Negative = rhombus (decode: ri = -(idx+1), col = ri%10, row = ri/10)
     */
    public int bestMove(QuaxBoard board) {
        int currentPlayer = board.isBlackTurn() ? BitBoard.BLACK : BitBoard.WHITE;
        BitBoard snapshot = board.toBitBoard();

        List<Integer> moves = collectEmptyTiles(snapshot);
        if (moves.isEmpty()) return Integer.MIN_VALUE;

        int simsPerMove = Math.max(1, simulations / moves.size());
        int threads = Runtime.getRuntime().availableProcessors();

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            Map<Integer, Future<Double>> futures = new LinkedHashMap<>();

            for (int move : moves) {
                final int m = move;
                futures.put(m, pool.submit(() ->
                        evaluateMove(snapshot, m, currentPlayer, simsPerMove)
                ));
            }

            int bestMove = moves.get(0);
            double bestRate = -1;

            for (Map.Entry<Integer, Future<Double>> entry : futures.entrySet()) {
                double rate = entry.getValue().get();
                if (rate > bestRate) {
                    bestRate = rate;
                    bestMove = entry.getKey();
                    lastBestWinRate = rate;
                }
            }

            System.out.printf("Best move: ");
            if (bestMove >= 0) {
                char col = (char) ('A' + bestMove % 11);
                int row = (bestMove / 11) + 1;
                System.out.printf("Oct(%c,%d)  Win rate: %.1f%%%n", col, row, bestRate * 100);
            } else {
                int ri = -(bestMove + 1);
                int col = (ri % 10) + 1;
                int row = (ri / 10) + 1;
                System.out.printf("Rho(%d,%d)  Win rate: %.1f%%%n", col, row, bestRate * 100);
            }

            return bestMove;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private double evaluateMove(BitBoard snapshot, int move,
                                int player, int sims) {
        Random rng = new Random();
        int wins = 0;

        for (int i = 0; i < sims; i++) {
            BitBoard bb = snapshot.copy();

            // Play the fixed first move for this thread
            applyMove(bb, move, player);
            int next = (player == BitBoard.BLACK) ? BitBoard.WHITE : BitBoard.BLACK;
            bb.setBlackTurn(next == BitBoard.BLACK);

            // Randomise the rest
            if (simulateRest(bb, player, rng)) wins++;
        }

        return (double) wins / sims;
    }

    private boolean simulateRest(BitBoard bb, int currentPlayer, Random rng) {
        List<Integer> empty = collectEmptyTiles(bb);

        // Fisher-Yates shuffle
        for (int i = empty.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = empty.get(i); empty.set(i, empty.get(j)); empty.set(j, tmp);
        }

        // Build Union-Find from the current (partially filled) board
        UnionFind[] ufs = buildUF(bb);
        UnionFind ufBlack = ufs[0];
        UnionFind ufWhite = ufs[1];

        int turn = bb.isBlackTurn() ? BitBoard.BLACK : BitBoard.WHITE;

        for (int move : empty) {
            applyMove(bb, move, turn);

            UnionFind uf = (turn == BitBoard.BLACK) ? ufBlack : ufWhite;

            // Union the newly placed tile with its neighbours — O(α(n))
            if (move >= 0) {
                unionOct(bb, uf, move % 11, move / 11, turn);
            } else {
                int ri = -(move + 1);
                unionRho(bb, uf, ri % 10, ri / 10, turn);
            }

            // Win = virtual start and end nodes are now connected
            if (turn == BitBoard.BLACK && ufBlack.connected(BLACK_START, BLACK_END))
                return currentPlayer == BitBoard.BLACK;
            if (turn == BitBoard.WHITE && ufWhite.connected(WHITE_START, WHITE_END))
                return currentPlayer == BitBoard.WHITE;

            turn = (turn == BitBoard.BLACK) ? BitBoard.WHITE : BitBoard.BLACK;
        }

        return false;
    }

    // Helpers

    private List<Integer> collectEmptyTiles(BitBoard bb) {
        List<Integer> moves = new ArrayList<>();
        for (int r = 0; r < 11; r++)
            for (int c = 0; c < 11; c++)
                if (bb.getOctagon(c, r) == BitBoard.EMPTY)
                    moves.add(r * 11 + c);
        for (int r = 0; r < 10; r++)
            for (int c = 0; c < 10; c++)
                if (bb.getRhombus(c, r) == BitBoard.EMPTY)
                    moves.add(-(r * 10 + c + 1));
        return moves;
    }

    private void applyMove(BitBoard bb, int move, int player) {
        if (move >= 0)
            bb.setOctagon(move % 11, move / 11, player);
        else {
            int ri = -(move + 1);
            bb.setRhombus(ri % 10, ri / 10, player);
        }
    }

    public Map<Integer, Integer> getConnectivityScores(QuaxBoard board, TileState botColor) {
        System.out.println("Scoring for: " + botColor);
        BitBoard bb = board.toBitBoard();
        int player = botColor == TileState.BLACK ? BitBoard.BLACK : BitBoard.WHITE;
        Map<Integer, Integer> scores = new LinkedHashMap<>();

        for (int r = 0; r < 11; r++) {
            for (int c = 0; c < 11; c++) {
                if (bb.getOctagon(c, r) != BitBoard.EMPTY) continue;
                int score = 0;
                for (int[] n : bb.getNeighbours(0, c, r)) {
                    int state = (n[0] == 0) ? bb.getOctagon(n[1], n[2]) : bb.getRhombus(n[1], n[2]);
                    if (state == player) score += 2;
                    else if (state != BitBoard.EMPTY) score += 1;
                }
                scores.put(r * 11 + c, score);
            }
        }

        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                if (bb.getRhombus(c, r) != BitBoard.EMPTY) continue;
                int score = 0;
                for (int[] n : bb.getNeighbours(1, c, r)) {
                    int state = (n[0] == 0) ? bb.getOctagon(n[1], n[2]) : bb.getRhombus(n[1], n[2]);
                    if (state == player) score += 2;
                    else if (state != BitBoard.EMPTY) score += 1;
                }
                scores.put(-(r * 10 + c + 1), score);
            }
        }

        return scores;
    }

    public Map<Integer, Double> getAllWinRates(QuaxBoard board, TileState botColor) {
        int currentPlayer = botColor == TileState.BLACK ? BitBoard.BLACK : BitBoard.WHITE;
        BitBoard snapshot = board.toBitBoard();

        List<Integer> moves = collectEmptyTiles(snapshot);
        if (moves.isEmpty()) return Collections.emptyMap();

        int simsPerMove = Math.max(1, simulations / moves.size());
        int threads = Runtime.getRuntime().availableProcessors();

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            Map<Integer, Future<Double>> futures = new LinkedHashMap<>();
            for (int move : moves) {
                final int m = move;
                futures.put(m, pool.submit(() ->
                        evaluateMove(snapshot, m, currentPlayer, simsPerMove)
                ));
            }

            Map<Integer, Double> rates = new LinkedHashMap<>();
            for (Map.Entry<Integer, Future<Double>> entry : futures.entrySet()) {
                rates.put(entry.getKey(), entry.getValue().get());
            }
            return rates;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}