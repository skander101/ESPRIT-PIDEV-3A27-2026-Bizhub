package com.bizhub;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL fxml = App.class.getResource("/com/bizhub/fxml/login.fxml");
        if (fxml == null) {
            throw new IllegalStateException("Cannot find /com/bizhub/fxml/login.fxml on classpath");
        }

        Parent root = FXMLLoader.load(fxml);

        var bounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, Math.max(980, bounds.getWidth()), Math.max(640, bounds.getHeight()));

        stage.setTitle("BizHub - Users & Reviews Administration");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);

        // Full screen
        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
