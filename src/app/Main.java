package app;

import app.ui.AppController;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        new AppController(stage).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
