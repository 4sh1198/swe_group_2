package comp20050.qssboard.controller;

import javafx.scene.paint.Color;

public interface BoardConfig {

    // Board dimensions
    int    CELLS      = 11;
    int    RADIUS     = 28;
    double BORDER     = 40.0;
    double STEP       = 2 * RADIUS * Math.cos(Math.PI / 8);
    double BOARD_SIZE = CELLS * STEP;
    double TOTAL_SIZE = BORDER + BOARD_SIZE + BORDER;

    // Colours
    Color OCT_FILL   = Color.DARKORANGE;
    Color OCT_STROKE = Color.BLACK;
    Color RHO_FILL   = Color.ORANGE;
    Color RHO_STROKE = Color.BLACK;
    Color BOARD_BG   = Color.web("#564138");
}