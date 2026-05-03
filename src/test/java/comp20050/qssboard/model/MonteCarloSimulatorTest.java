package comp20050.qssboard.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MonteCarloSimulatorTest {
    @Test
    void botReturnsValidMove(){
        QuaxBoard board = new QuaxBoard(11);
        MonteCarloSimulator sim = new MonteCarloSimulator(1000);

        int move = sim.bestMove(board);

        assertNotEquals(Integer.MIN_VALUE, move);
    }

    @Test
    void botChooseEmptyTile(){
        QuaxBoard board = new QuaxBoard(11);
        board.getOctagon(0,0).setState(TileState.BLACK);

        MonteCarloSimulator sim = new MonteCarloSimulator(1000);
        int move = sim.bestMove(board);

        if(move >= 0){
            int column = move % 11;
            int row = move / 11;

            assertEquals(TileState.EMPTY, board.getOctagon(column, row).getState());
        }
    }

    @Test
    void botFindsWinningMove(){
        QuaxBoard board = new QuaxBoard(11);

        for (int r = 0; r < 10; r++){
            board.getOctagon(0, r).setState(TileState.BLACK);
        }

        board.setBlackTurn(true);

        MonteCarloSimulator sim = new MonteCarloSimulator(50000);
        int move = sim.bestMove(board);

        if (move >= 0){
            int column = move % 11;
            int row = move / 11;

            board.getOctagon(column, row).setState(TileState.BLACK);
        }
        assertTrue(board.checkWin(TileState.BLACK));
    }

    @Test
    void winRateIsValid(){
        QuaxBoard board = new QuaxBoard(11);
        MonteCarloSimulator sim = new MonteCarloSimulator(1000);

        sim.bestMove(board);

        assertTrue(sim.lastBestWinRate >= 0.0);
        assertTrue(sim.lastBestWinRate <= 1.0);
    }

    @Test
    void botDoesNotReturnOccupiedRhombus() {
        QuaxBoard board = new QuaxBoard(11);
        board.getRhombus(0.5, 0.5).setState(TileState.WHITE);

        MonteCarloSimulator sim = new MonteCarloSimulator(1000);
        int move = sim.bestMove(board);

        if (move < 0) {
            int ri = -(move + 1);
            int col = ri % 10;
            int row = ri / 10;
            assertEquals(TileState.EMPTY, board.getRhombus(col + 0.5, row + 0.5).getState());
        }
    }

    @Test
    void fullBoardReturnsNoMove() {
        QuaxBoard board = new QuaxBoard(11);

        for (int r = 0; r < 11; r++) {
            for (int c = 0; c < 11; c++) {
                board.getOctagon(c, r).setState(TileState.BLACK);
            }
        }

        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                board.getRhombus(c + 0.5, r + 0.5).setState(TileState.WHITE);
            }
        }

        MonteCarloSimulator sim = new MonteCarloSimulator(1000);

        assertEquals(Integer.MIN_VALUE, sim.bestMove(board));
    }
}
