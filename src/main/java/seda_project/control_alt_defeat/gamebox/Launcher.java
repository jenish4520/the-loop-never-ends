package seda_project.control_alt_defeat.gamebox;

import javafx.application.Application;
import javafx.stage.Stage;


// Entry point.
public class Launcher extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setFullScreenExitHint(""); // Optional: remove the annoying ESC message if desired
        primaryStage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                primaryStage.setFullScreen(true);
            }
        });
        primaryStage.setFullScreen(true);
        GameHub hub = new GameHub(primaryStage);
        hub.show();
    }

    // Launch app.
    public static void main(String[] args) {
        launch(args);
    }
}
