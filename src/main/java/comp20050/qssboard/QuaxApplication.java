package comp20050.qssboard;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class QuaxApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(QuaxApplication.class.getResource("game-view.fxml"));
        double totalSize = 40 + 11 * (2 * 28 * Math.cos(Math.PI / 8)) + 40; // BORDER + BOARD + BORDER
        double sceneHeight = totalSize + 80;
        Scene scene = new Scene(fxmlLoader.load(), totalSize, sceneHeight);
        stage.setTitle("Quax (Human V Bot)");
        stage.setScene(scene);
        stage.setResizable(true);  // Allow horizontal resizing so the strategy side-panel can expand the window.
        stage.setMinHeight(sceneHeight + 40);
        stage.setMaxHeight(sceneHeight + 40);
        stage.setMinWidth(totalSize + 16);
        stage.show();
    }
}
