package comp20050.qssboard.controller;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class LabelsTest {

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void lettersAtoKexist() throws Exception {
        GameController controller = new GameController();
        Pane pane = new Pane();

        inject(controller, pane);
        runFX(controller::initialize);

        Set<String> texts = getTexts(pane);

        for (char c = 'A'; c <= 'K'; c++) {
            assertTrue(texts.contains(String.valueOf(c)));
        }
    }

    @Test
    void numbers1to11exist() throws Exception {
        GameController controller = new GameController();
        Pane pane = new Pane();

        inject(controller, pane);
        runFX(controller::initialize);

        Set<String> texts = getTexts(pane);

        for (int i = 1; i <= 11; i++) {
            assertTrue(texts.contains(String.valueOf(i)));
        }
    }


    private static void inject(GameController gc, Pane pane) throws Exception {
        Field f = GameController.class.getDeclaredField("boardPane");
        f.setAccessible(true);
        f.set(gc, pane);
    }

    private static void runFX(Runnable r) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            r.run();
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);
    }

    private static Set<String> getTexts(Pane pane) {
        Set<String> set = new HashSet<>();
        for (Node n : pane.getChildren()) {
            if (n instanceof Text) {
                set.add(((Text) n).getText());
            }
        }
        return set;
    }
}
