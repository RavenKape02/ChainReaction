package app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("JavaFX 26 is running on JDK " + System.getProperty("java.version"));
        StackPane root = new StackPane(label);

        Scene scene = new Scene(root, 500, 180);
        stage.setTitle("JavaFX 26 Smoke Test");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
