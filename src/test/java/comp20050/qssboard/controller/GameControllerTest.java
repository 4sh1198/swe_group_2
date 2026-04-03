package comp20050.qssboard.controller;

import comp20050.qssboard.model.Octagon;
import comp20050.qssboard.model.QuaxBoard;
import comp20050.qssboard.model.Rhombus;
import comp20050.qssboard.model.TileState;
import javafx.scene.control.Button;
import java.lang.reflect.Method;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.sun.javafx.reflect.FieldUtil.getField;
import static org.junit.jupiter.api.Assertions.*;

public class GameControllerTest {

    // javafx setup
    @BeforeAll
    static void startJavaFxToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }



    // Checks that an 11x11 board of octagons is created
    @Test
    void CorrectNumberOfOctagons() throws Exception {
        Pane pane = initControllerAndGetPane();

        long octagons = pane.getChildren().stream()
                .filter(n -> n instanceof Polygon)
                .map(n -> (Polygon) n)
                .filter(p -> p.getPoints().size() == 16)
                .filter(p -> !"turnDisplay".equals(p.getUserData()))
                .count();

        assertEquals(121, octagons);
    }

    // Checks that 10x10 rhombus connectors are created
    @Test
    void CorrectNumberOfRhombuses() throws Exception {
        Pane pane = initControllerAndGetPane();

        long rhombuses = pane.getChildren().stream()
                .filter(n -> n instanceof Polygon)
                .map(n -> (Polygon) n)
                .filter(p -> p.getPoints().size() == 8)
                .filter(p -> !"turnDisplay".equals(p.getUserData()))
                .count();

        assertEquals(100, rhombuses);
    }

    // test board background and four borders exist
    @Test
    void board_hasBackgroundAndBorders() throws Exception {
        Pane pane = initControllerAndGetPane();

        long rectangles = pane.getChildren().stream()
                .filter(n -> n instanceof Rectangle)
                .map(n -> (Rectangle) n)
                .filter(r -> !"turnDisplay".equals(r.getUserData()))
                .count();

        assertEquals(5, rectangles);
    }



    // checks that letters A-K appear on top and bottom
    @Test
    void labels_lettersAToKExist() throws Exception {
        Pane pane = initControllerAndGetPane();
        Map<String, Long> counts = countTexts(pane);

        for (char c = 'A'; c <= 'K'; c++) {
            assertEquals(2L, counts.get(String.valueOf(c)));
        }
    }

    // checks that numbers 1-11 appear on left anf right
    @Test
    void labels_numbers1To11Exist() throws Exception {
        Pane pane = initControllerAndGetPane();
        Map<String, Long> counts = countTexts(pane);

        for (int i = 1; i <= 11; i++) {
            assertEquals(2L, counts.get(String.valueOf(i)));
        }
    }


    // checks that the turn display area is created
    @Test
    void turnDisplay_exists() throws Exception {
        Pane pane = initControllerAndGetPane();

        long nodes = pane.getChildren().stream()
                .filter(n -> "turnDisplay".equals(n.getUserData()))
                .count();

        assertEquals(4, nodes);
    }

    // checks that the text shows whose turn it is
    @Test
    void turnDisplay_textIsBlackToPlay() throws Exception {
        Pane pane = initControllerAndGetPane();

        boolean found = pane.getChildren().stream()
                .filter(n -> n instanceof Text)
                .map(n -> (Text) n)
                .anyMatch(t -> t.getText().equals("Black to play"));

        assertTrue(found);
    }

    // checks that clicking an empty octagon on the first move places a black tile
    @Test
    void firstOctagonClick_placesBlackTile() throws Exception {
        GameController controller = initController();
        QuaxBoard board = getBoard(controller);
        Octagon oct = board.getOctagon(0, 0);

        runFX(() -> {
            try {
                invokeOnOctagonClicked(controller, oct, new Polygon());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals(TileState.BLACK, oct.getState());
    }

    // checks that after black moves, white can place a tile on a rhombus
    @Test
    void secondMoveOnRhombus() throws Exception {
        GameController controller = initController();
        QuaxBoard board = getBoard(controller);

        Octagon firstMove = board.getOctagon(0, 0);
        Rhombus rho = board.getRhombus(0.5, 0.5);

        runFX(() -> {
            try {
                invokeOnOctagonClicked(controller, firstMove, new Polygon());
                invokeOnRhombusClicked(controller, rho, new Polygon());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals(TileState.WHITE, rho.getState());
    }



    // checks that clicking an already occupied octagon throws an exception
    @Test
    void OccupiedOctagonThrowsException() throws Exception {
        GameController controller = initController();
        QuaxBoard board = getBoard(controller);

        Octagon oct = board.getOctagon(0, 0);

        // make the tile occupied before clicking it
        oct.setState(TileState.BLACK);

        assertThrows(Exception.class, () -> {
            invokeOnOctagonClicked(controller, oct, new Polygon());
        });
    }


    // checks that the pie rule button appears after the first move
    @Test
    void pieButtonAfterFirstMove() throws Exception {
        GameController controller = initController();
        Pane pane = getPane(controller);
        QuaxBoard board = getBoard(controller);

        Octagon oct = board.getOctagon(0, 0);

        runFX(() -> {
            try {
                invokeOnOctagonClicked(controller, oct, new Polygon());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        boolean found = pane.getChildren().stream()
                .anyMatch(n -> "pieButton".equals(n.getUserData()));

        assertTrue(found);
    }

    // checks that the pie rule button disappears after white makes the next move
    @Test
    void pieButtonDisappearsAfterSecondMove() throws Exception {
        GameController controller = initController();
        Pane pane = getPane(controller);
        QuaxBoard board = getBoard(controller);

        Octagon firstMove = board.getOctagon(0, 0);
        Octagon secondMove = board.getOctagon(1, 0);

        runFX(() -> {
            try {
                invokeOnOctagonClicked(controller, firstMove, new Polygon());
                invokeOnOctagonClicked(controller, secondMove, new Polygon());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        boolean found = pane.getChildren().stream()
                .anyMatch(n -> "pieButton".equals(n.getUserData()));

        assertFalse(found);
    }

    // Win detection test for when black wins
    @Test
    void blackWinDetected() throws Exception{
        GameController controller = initController();
        QuaxBoard board = getBoard(controller);

        for (int i = 0; i < 11; i++){
            board.getOctagon(5, i).setState(TileState.BLACK);
        }
        assertTrue(board.checkWin(TileState.BLACK));
    }

    // Win detection tests for when white wins
    @Test
    void whiteWinDetected() throws Exception{
        GameController controller = initController();
        QuaxBoard board = getBoard(controller);

        for (int i = 0; i < 11; i++){
            board.getOctagon(i, 5).setState(TileState.WHITE);
        }
        assertTrue(board.checkWin(TileState.WHITE));

    }

    // Win detection when there is no winner
    @Test
    void noWinDetected() throws Exception{
        GameController controller = initController();
        QuaxBoard board = getBoard(controller);

        board.getOctagon(0, 0).setState(TileState.BLACK);
        board.getOctagon(1,1).setState(TileState.WHITE);

        assertFalse(board.checkWin(TileState.BLACK));
    }

    // testing if bot and humans are assigned their respective colours
    @Test
    void choosingWhiteAtStart() throws Exception{
        GameController controller = initController();

        ButtonType white = new ButtonType("White");

        runFX(() -> controller.applyChosenColor(white));

        TileState human = getField(controller, "humanColor");
        TileState bot = getField(controller, "botColor");

        assertEquals(TileState.WHITE, human);
        assertEquals(TileState.BLACK, bot);
    }

    @Test
    void choosingBlackAtStart() throws Exception{
        GameController controller = initController();

        ButtonType black = new ButtonType("blck");

        runFX(() -> controller.applyChosenColor(black));

        TileState human = getField(controller, "humanColor");
        TileState bot = getField(controller, "botColor");

        assertEquals(TileState.BLACK, human);
        assertEquals(TileState.WHITE, bot);
    }

    private static <T> T getField(GameController controller, String name) throws Exception{
        Field f = GameController.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(controller);
    }
    private static Pane initControllerAndGetPane() throws Exception {
        GameController controller = new GameController();
        controller.setTestMode(true);
        Pane pane = new Pane();
        injectPane(controller, pane);
        runFX(controller::initialize);
        return pane;
    }

    private static void injectPane(GameController controller, Pane pane) throws Exception {
        Field f = GameController.class.getDeclaredField("boardPane");
        f.setAccessible(true);
        f.set(controller, pane);
    }

    private static void runFX(Runnable r) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            r.run();
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);
    }

    private static Map<String, Long> countTexts(Pane pane) {
        Map<String, Long> map = new HashMap<>();
        for (Node n : pane.getChildren()) {
            if (n instanceof Text t) {
                map.merge(t.getText(), 1L, Long::sum);
            }
        }
        return map;
    }


    private static GameController initController() throws Exception {
        GameController controller = new GameController();
        controller.setTestMode(true);
        Pane pane = new Pane();
        injectPane(controller, pane);
        runFX(controller::initialize);
        return controller;
    }

    private static Pane getPane(GameController controller) throws Exception {
        Field f = GameController.class.getDeclaredField("boardPane");
        f.setAccessible(true);
        return (Pane) f.get(controller);
    }

    private static QuaxBoard getBoard(GameController controller) throws Exception {
        Field f = GameController.class.getDeclaredField("board");
        f.setAccessible(true);
        return (QuaxBoard) f.get(controller);
    }

    private static void invokeOnOctagonClicked(GameController controller, Octagon oct, Polygon shape) throws Exception {
        Method m = GameController.class.getDeclaredMethod("onOctagonClicked", Octagon.class, Polygon.class);
        m.setAccessible(true);
        m.invoke(controller, oct, shape);
    }

    private static void invokeOnRhombusClicked(GameController controller, Rhombus rho, Polygon shape) throws Exception {
        Method m = GameController.class.getDeclaredMethod("onRhombusClicked", Rhombus.class, Polygon.class);
        m.setAccessible(true);
        m.invoke(controller, rho, shape);
    }
}
