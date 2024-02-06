package com.updclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("LoginWindow.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("CHAT 1");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(event -> UDPLoginController.stopClient());
    }

    public static void main(String[] args) {
        launch();
    }
}