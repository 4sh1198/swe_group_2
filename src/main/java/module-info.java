module comp20050.qssboard {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;

    opens comp20050.qssboard to javafx.fxml;
    exports comp20050.qssboard;
    exports comp20050.qssboard.model;
    exports comp20050.qssboard.controller;
    opens comp20050.qssboard.controller to javafx.fxml;
}