package comp20050.qssboard.controller;

import javafx.application.Platform;
import javafx.scene.Node;
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
    void turnDisplay_textIsWhiteToPlay() throws Exception {
        Pane pane = initControllerAndGetPane();

        boolean found = pane.getChildren().stream()
                .filter(n -> n instanceof Text)
                .map(n -> (Text) n)
                .anyMatch(t -> t.getText().equals("WHITE to play"));

        assertTrue(found);
    }


    private static Pane initControllerAndGetPane() throws Exception {
        GameController controller = new GameController();
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
}
