package comp20050.qssboard.model;

import java.util.*;
import java.util.concurrent.*;

public class MonteCarloSimulator {

    private final int simulations;

    public MonteCarloSimulator(int simulations) {
        this.simulations = simulations;
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

        int turn = bb.isBlackTurn() ? BitBoard.BLACK : BitBoard.WHITE;
        for (int move : empty) {
            applyMove(bb, move, turn);
            turn = (turn == BitBoard.BLACK) ? BitBoard.WHITE : BitBoard.BLACK;
        }

        return bb.checkWin(currentPlayer);
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

    private void applyMove(BitBoard bb, int move, int player) {
        if (move >= 0)
            bb.setOctagon(move % 11, move / 11, player);
        else {
            int ri = -(move + 1);
            bb.setRhombus(ri % 10, ri / 10, player);
        }
    }
}
