package com.server;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class InterfaceController {
    @FXML
    private TextFlow chatHistoryTextFlow; // The TextFlow used to display chat history or server logs.
    @FXML
    private ScrollPane scrollPane; // The ScrollPane that wraps the TextFlow for scrollable content.

    /**
     * Appends a new message to the chat history.
     *
     * This method ensures that the TextFlow is updated on the JavaFX Application Thread
     * and automatically scrolls to the bottom when a new message is added.
     *
     * @param message The message to be appended to the chat history.
     */
    public void appendMessage(String message) {
        Platform.runLater(() -> {
            Text AddMessage = new Text(message + "\n"); // Create a new Text node with the provided message, followed by a newline.
            chatHistoryTextFlow.getChildren().add(AddMessage); // Add the new Text node to the children of the TextFlow.
            // Ensure the ScrollPane scrolls to the bottom to show the latest message.
            scrollPane.layout(); // Force layout updates to ensure accurate scrolling behavior.
            scrollPane.setVvalue(1.0); // Set the vertical scrollbar value to the bottom.
        });
    }
}
