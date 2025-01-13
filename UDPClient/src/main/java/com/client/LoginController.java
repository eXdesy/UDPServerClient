package com.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class LoginController {
    public static final int CLIENT_PORT = 6011; // Port for the client socket
    public static final int SERVER_PORT = 5010; // Port for the server socket
    public static final int BUFFER_SIZE = 1024; // Buffer size for UDP packets
    public static final String SERVER_IP = ""; // IP address of the server
    public static DatagramSocket clientSocket; // Socket for client communication

    @FXML
    private TextField username; // TextField for entering username
    @FXML
    private Button enterButton; // Button to trigger login action

    /**
     * Initializes the login controller. Sets up the DatagramSocket and defines the action for the enter button.
     */
    @FXML
    public void initialize() {
        try {
            clientSocket = new DatagramSocket(CLIENT_PORT); // Create a DatagramSocket bound to the client port
            enterButton.setOnAction(event -> { // Define the action when the enter button is clicked
                String user = username.getText(); // Get the entered username
                if (verifyUsername(user)) { // Verify the username with the server
                    openChatWindow(user); // Open chat window if username is valid
                } else {
                    System.out.println("Error, server is unavailable."); // Display error if server is unreachable
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops the client by closing the DatagramSocket if it is open.
     */
    public static void stopClient() {
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close(); // Close the socket
            System.out.println("Socket closed."); // Notify that the socket is closed
        }
    }

    /**
     * Verifies the availability of the username by sending it to the server.
     *
     * @param username The username to verify.
     * @return true if the username is available, false otherwise.
     */
    private boolean verifyUsername(String username) {
        if (username == null || username.trim().isEmpty()) { // Validate that the username is not null or empty
            System.out.println("Please enter a valid username.");
            return false;
        }
        try {
            String message = "CHECK_USERNAME|" + username; // Prepare the message to be sent to the server
            byte[] sendData = message.getBytes();

            InetAddress serverAddress = InetAddress.getByName(SERVER_IP); // Get the server's IP address and create a packet to send
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
            clientSocket.send(sendPacket); // Send the packet to the server

            byte[] receiveData = new byte[BUFFER_SIZE]; // Prepare to receive the server's response
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket); // Receive the response packet

            String response = new String(receivePacket.getData(), 0, receivePacket.getLength()); // Decode the server's response
            return response.equals("USERNAME_AVAILABLE"); // Check if the username is available
        } catch (Exception e) {
            System.out.println("The username is not available. Please enter another name.");
            return false;
        }
    }

    /**
     * Opens the chat window and transitions from the login window.
     *
     * @param user The username to pass to the chat window.
     */
    private void openChatWindow(String user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml")); // Load the chat window layout from the FXML file
            Parent root = loader.load();

            ChatController chatController = loader.getController(); // Pass the username to the chat controller
            chatController.setUserName(user);

            Stage stage = (Stage) enterButton.getScene().getWindow(); // Get the current stage and set the new scene for the chat window
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show(); // Display the chat window

            stage.setOnCloseRequest(event -> stopClient()); // Ensure the socket is closed when the window is closed
        } catch (IOException e) {
            throw new RuntimeException("Error opening chat window" + e.getMessage());
        }
    }
}
