package comp20050.qssboard.controller;

import comp20050.qssboard.model.MonteCarloSimulator;
import comp20050.qssboard.model.QuaxBoard;
import comp20050.qssboard.model.TileState;
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
}
