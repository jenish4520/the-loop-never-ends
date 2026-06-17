package seda_project.control_alt_defeat.gamebox;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MyController {
    private int n = 0;

    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        ++n;
        welcomeText.setText("The button was clicked " + n + " times");
    }
}
