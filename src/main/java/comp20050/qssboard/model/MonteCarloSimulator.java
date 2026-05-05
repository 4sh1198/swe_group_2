package comp20050.qssboard.model;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MonteCarloSimulator {

    // Index layout for Union-Find
    // Octagons:  r*11 + c         →   0..120
    // Rhombuses: 121 + r*10 + c   → 121..220
    // Virtual:   BLACK_START=221, BLACK_END=222, WHITE_START=223, WHITE_END=224
    private static final int OCT_OFFSET = 0;
    private static final int RHO_OFFSET = 121;
    private static final int BLACK_START = 221;
    private static final int BLACK_END = 222;
    private static final int WHITE_START = 223;
    private static final int WHITE_END = 224;
    private static final int UF_SIZE = 225;
    private final int simulations;
    public double lastBestWinRate = 0.0;

    // Cached after each bestMove() call — reused by getAllWinRates() with no extra computation
    private Map<Integer, Double> lastWinRates = Collections.emptyMap();

    public MonteCarloSimulator(int simulations) {
        this.simulations = simulations;
    }

    // Union-Find
    // It's faster than BFS, so used here to simulate millions of games in good time

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

    // Build a Union-Find from the current board state

    // Union helpers
    private void unionOct(BitBoard bb, UnionFind uf, int c, int r, int player) {
        int id = OCT_OFFSET + r * 11 + c;

        // Connect to virtual edge nodes
        if (player == BitBoard.BLACK) {
            if (r == 0) uf.union(id, BLACK_START);
            if (r == 10) uf.union(id, BLACK_END);
        } else {
            if (c == 0) uf.union(id, WHITE_START);
            if (c == 10) uf.union(id, WHITE_END);
        }

        // Union with same-player occupied neighbours
        for (int[] neighbour : bb.getNeighbours(0, c, r)) {
            int nState = (neighbour[0] == 0) ? bb.getOctagon(neighbour[1], neighbour[2]) : bb.getRhombus(neighbour[1], neighbour[2]);
            if (nState != player) continue;
            int nId = (neighbour[0] == 0)
                    ? OCT_OFFSET + neighbour[2] * 11 + neighbour[1]
                    : RHO_OFFSET + neighbour[2] * 10 + neighbour[1];
            uf.union(id, nId);
        }
    }

    private void unionRho(BitBoard bb, UnionFind uf, int c, int r, int player) {
        int id = RHO_OFFSET + r * 10 + c;

        for (int[] neighbour : bb.getNeighbours(1, c, r)) {
            int nState = (neighbour[0] == 0) ? bb.getOctagon(neighbour[1], neighbour[2]) : bb.getRhombus(neighbour[1], neighbour[2]);
            if (nState != player) continue;
            int nId = (neighbour[0] == 0)
                    ? OCT_OFFSET + neighbour[2] * 11 + neighbour[1]
                    : RHO_OFFSET + neighbour[2] * 10 + neighbour[1];
            uf.union(id, nId);
        }
    }

    /*
     * Returns the index of the best move found.
     * Positive = octagon (decode: col = idx%11, row = idx/11)
     * Negative = rhombus (decode: ri = -(idx+1), col = ri%10, row = ri/10)
     */
    public int bestMove(QuaxBoard board) {
        int currentPlayer = board.isBlackTurn() ? BitBoard.BLACK : BitBoard.WHITE;
        Map<Integer, Double> rates = computeWinRates(board.toBitBoard(), currentPlayer);
        if (rates.isEmpty()) return Integer.MIN_VALUE;

        // Cache for use by getAllWinRates()
        lastWinRates = rates;

        Map.Entry<Integer, Double> best = Collections.max(
                rates.entrySet(), Map.Entry.comparingByValue()
        );

        lastBestWinRate = best.getValue();
        return best.getKey();
    }

    private double evaluateMove(BitBoard snapshot, int move, int player, int sims) {
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

        fisherYatesShuffle(rng, empty);

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

    private void fisherYatesShuffle(Random rng, List<Integer> empty) {
        for (int i = empty.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = empty.get(i);
            empty.set(i, empty.get(j));
            empty.set(j, tmp);
        }
    }

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

    // Helpers
    private void applyMove(BitBoard bb, int move, int player) {
        if (move >= 0)
            bb.setOctagon(move % 11, move / 11, player);
        else {
            int ri = -(move + 1);
            bb.setRhombus(ri % 10, ri / 10, player);
        }
    }

    public Map<Integer, Double> getAllWinRates(QuaxBoard board, TileState botColor) {
        return lastWinRates;
    }

    private Map<Integer, Double> computeWinRates(BitBoard snapshot, int player) {
        List<Integer> moves = collectEmptyTiles(snapshot);
        if (moves.isEmpty()) return Collections.emptyMap();

        int simsPerMove = Math.max(1, simulations / moves.size());
        int threads = Runtime.getRuntime().availableProcessors();

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            Map<Integer, Future<Double>> futures = new LinkedHashMap<>();
            for (int move : moves) {
                final int m = move;
                futures.put(m, pool.submit(() -> evaluateMove(snapshot, m, player, simsPerMove)));
            }
            Map<Integer, Double> rates = new LinkedHashMap<>();
            for (Entry<Integer, Future<Double>> entry : futures.entrySet()) {
                rates.put(entry.getKey(), entry.getValue().get());
            }
            return rates;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class UnionFind {
        private final int[] parent, rank;

        UnionFind(int size) {
            parent = new int[size];
            rank = new int[size];
            for (int i = 0; i < size; i++) parent[i] = i;
        }

        int find(int x) {
            if (parent[x] != x) parent[x] = find(parent[x]); // path compression
            return parent[x];
        }

        void union(int a, int b) {
            a = find(a);
            b = find(b);
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
}