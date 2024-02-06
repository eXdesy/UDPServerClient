package com.udpserver;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ServerInterfaceController {

    @FXML
    private TextFlow chatHistoryTextFlow;

    public void appendMessage(String message) {
        // Agrega un mensaje al historial de chat en la interfaz de usuario
        Platform.runLater(() -> {
            // Crea un nuevo objeto Text con el nombre de usuario, el mensaje y un salto de línea.
            Text AddMessage = new Text(message + "\n");
            // Agrega el mensaje al flujo de texto del historial del chat.
            chatHistoryTextFlow.getChildren().add(AddMessage);
        });
    }
}
