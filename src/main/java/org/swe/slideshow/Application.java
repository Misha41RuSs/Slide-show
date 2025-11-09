package org.swe.slideshow;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Application extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application
                .class.getResource("application.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 1000);
        stage.setTitle("Слайд-шоу");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}