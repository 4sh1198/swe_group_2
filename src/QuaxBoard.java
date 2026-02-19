import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class QuaxBoard extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {


        int cells = 11;
        double cellSize = 50;
        double border = 40;
        double boardSize = cells * cellSize;  // 550px
        double totalSize = border + boardSize + border;  // 630px

        Pane pane = new Pane();

        // board placeholder
        Rectangle board = new Rectangle(border, border, boardSize, boardSize);
        board.setFill(Color.ORANGE);
        pane.getChildren().add(board);

        // boarder top and bottom
        Rectangle topStrip = new Rectangle(0, 0, totalSize, border);
        topStrip.setFill(Color.web("#2A2A2A"));
        pane.getChildren().add(topStrip);

        Rectangle bottomStrip = new Rectangle(0, totalSize - border, totalSize, border);
        bottomStrip.setFill(Color.web("#2A2A2A"));
        pane.getChildren().add(bottomStrip);

        // boarder sides
        Rectangle leftStrip = new Rectangle(0, 0, border, totalSize);
        leftStrip.setFill(Color.web("#D0D0D0"));
        pane.getChildren().add(leftStrip);

        Rectangle rightStrip = new Rectangle(totalSize - border, 0, border, totalSize);
        rightStrip.setFill(Color.web("#D0D0D0"));
        pane.getChildren().add(rightStrip);

        // displaying A-K
        String[] letters = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K"};

        for (int col = 0; col < cells; col++) {
            double x = border + col * cellSize + cellSize / 2;


            Text topLabel = new Text(letters[col]);
            topLabel.setFill(Color.WHITE);
            topLabel.setFont(Font.font("Arial", 14));
            topLabel.setX(x - topLabel.getBoundsInLocal().getWidth() / 2);
            topLabel.setY(border / 2 + 5);
            pane.getChildren().add(topLabel);


            Text bottomLabel = new Text(letters[col]);
            bottomLabel.setFill(Color.WHITE);
            bottomLabel.setFont(Font.font("Arial", 14));
            bottomLabel.setX(x - bottomLabel.getBoundsInLocal().getWidth() / 2);
            bottomLabel.setY(totalSize - border / 2 + 5);
            pane.getChildren().add(bottomLabel);
        }

        //displaying 1-11
        for (int row = 0; row < cells; row++) {
            int number = cells - row;
            double y = border + row * cellSize + cellSize / 2 + 5;

            // left label
            Text leftLabel = new Text(String.valueOf(number));
            leftLabel.setFill(Color.BLACK);
            leftLabel.setFont(Font.font("Arial", 14));
            leftLabel.setX(border / 2 - leftLabel.getBoundsInLocal().getWidth() / 2);
            leftLabel.setY(y);
            pane.getChildren().add(leftLabel);

            // right label
            Text rightLabel = new Text(String.valueOf(number));
            rightLabel.setFill(Color.BLACK);
            rightLabel.setFont(Font.font("Arial", 14));
            rightLabel.setX(totalSize - border / 2 - rightLabel.getBoundsInLocal().getWidth() / 2);
            rightLabel.setY(y);
            pane.getChildren().add(rightLabel);
        }

        Scene scene = new Scene(pane, totalSize, totalSize);
        stage.setTitle("Quax (Human V Human)");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
}
