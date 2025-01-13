package com.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class ChatController {
    private String USER_NAME; // Stores the username of the current user.
    public static final String SAVE_RUTE = "C:\\Users\\Admin\\Downloads\\ClientImages"; // Path to save received images.
    private final Map<String, Color> userColors = new HashMap<>();
    private final Random random = new Random();

    @FXML
    private TextField messageInput; // TextField for user to input messages.
    @FXML
    private TextFlow chatHistoryTextFlow; // TextFlow to display chat history.
    @FXML
    private ScrollPane scrollPane;

    /**
     * Initializes the chat controller by starting the message receiving thread.
     */
    public void initialize() {
        try {
            receiveMessage(); // Start the thread to listen for incoming messages.
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets the username for the chat session.
     *
     * @param USER_NAME the username of the current user.
     */
    public void setUserName(String USER_NAME) {
        this.USER_NAME = USER_NAME; // Assigns the username for this session.
    }

    private Color generateRandomColor() {
        double hue = random.nextDouble() * 360;
        double saturation = 0.5 + random.nextDouble() * 0.5;
        double brightness = 0.7 + random.nextDouble() * 0.3;
        return Color.hsb(hue, saturation, brightness);
    }

    /**
     * Continuously listens for incoming messages and updates the chat UI accordingly.
     */
    private void receiveMessage() {
        new Thread(() -> {
            try {
                while (true) { // Infinite loop to listen for messages.
                    byte[] receiveData = new byte[LoginController.BUFFER_SIZE]; // Buffer for receiving data.
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    LoginController.clientSocket.receive(receivePacket); // Receive a packet from the server.

                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength()); // Convert packet data to string.
                    String[] messageParts = message.split("\\|"); // Split the message into components.

                    if (messageParts[0].equals("TEXT")) {
                        String receivedUserName = messageParts[1]; // Extract sender's username.
                        String messageType = messageParts[2]; // Extract the actual message content.

                        if (!receivedUserName.equals(USER_NAME)) { // Determine message color based on sender.
                            appendMessage(receivedUserName, messageType);
                        } else {
                            appendMessage(USER_NAME, messageType);
                        }
                    } else {
                        byte[] data = receivePacket.getData();
                        String combinedMessage = new String(data, 0, receivePacket.getLength()); // Convert to string.
                        String[] parts = combinedMessage.split("\\|"); // Extract components.

                        String receivedUserName = parts[0]; // Extract sender's username.
                        String fileName = parts[1]; // Extract filename.

                        String savedImagePath = SAVE_RUTE + "\\" + fileName; // Build save path.
                        File f = new File(savedImagePath);
                        FileOutputStream outToFile = new FileOutputStream(f); // Create file output stream.
                        saveImage(outToFile, LoginController.clientSocket); // Save the received image.

                        if (!receivedUserName.equals(USER_NAME)) { // Append image to chat UI based on sender.
                            appendImage(receivedUserName, savedImagePath);
                        } else {
                            appendImage(USER_NAME, savedImagePath);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                Platform.exit();
            }
        }).start();
    }

    /**
     * Saves an image received via a DatagramSocket. It uses a sequence number to ensure the packets
     * are written in the correct order and sends acknowledgments for each packet received.
     *
     * @param outToFile The FileOutputStream to write the received data.
     * @param socket    The DatagramSocket used for receiving packets.
     */
    private static void saveImage(FileOutputStream outToFile, DatagramSocket socket) {
        try {
            boolean flag; // Indicates if the current packet is the last one.
            int sequenceNumber; // Stores the sequence number of the current packet.
            int foundLast = 0; // Tracks the last successfully received sequence number.

            while (true) {
                byte[] message = new byte[1024]; // Buffer for incoming packet data.
                byte[] fileByteArray = new byte[1021]; // Buffer to extract file data from the packet.

                DatagramPacket receivedPacket = new DatagramPacket(message, message.length); // Receive the next packet.
                socket.receive(receivedPacket);
                message = receivedPacket.getData(); // Get data from the received packet.

                InetAddress address = receivedPacket.getAddress(); // Address of the sender.
                int port = receivedPacket.getPort(); // Port of the sender.
                // Extract the sequence number from the first two bytes of the packet.
                sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);
                flag = (message[2] & 0xff) == 1; // Check if this is the last packet based on the third byte.

                if (sequenceNumber == (foundLast + 1)) { // Verify if the sequence number matches the expected sequence number.
                    foundLast = sequenceNumber; // Update the last successfully received sequence number.
                    // Copy the file data (starting from the 4th byte) into the file buffer.
                    System.arraycopy(message, 3, fileByteArray, 0, 1021);
                    outToFile.write(fileByteArray); // Write the data to the file.

                    sendAck(foundLast, socket, address, port); // Send an acknowledgment for the successfully received packet.
                } else {
                    // Log a message if the packet's sequence number is not as expected.
                    System.out.println("Expected sequence number: " + (foundLast + 1) + " but received " + sequenceNumber + ". DISCARDING");
                    sendAck(foundLast, socket, address, port); // Send an acknowledgment for the last successfully received packet.
                }

                if (flag) { // If this is the last packet, close the file and exit the loop.
                    System.out.println("Image received");
                    outToFile.close();
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends an acknowledgment for a specific sequence number to the sender.
     *
     * @param foundLast The last successfully received sequence number.
     * @param socket    The DatagramSocket used for sending the acknowledgment.
     * @param address   The address of the sender.
     * @param port      The port of the sender.
     */
    private static void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) {
        try {
            byte[] ackPacket = new byte[2]; // Buffer for acknowledgment packet.
            // Store the sequence number in the acknowledgment packet.
            ackPacket[0] = (byte) (foundLast >> 8); // High byte of the sequence number.
            ackPacket[1] = (byte) (foundLast); // Low byte of the sequence number.
            // Create a DatagramPacket with the acknowledgment data.
            DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);
            socket.send(acknowledgement); // Send the acknowledgment packet to the sender.
            System.out.println("Sent ack: Sequence Number = " + foundLast);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Handles the "Send" button click event by sending the entered message.
     */
    @FXML
    private void sendMessageButton() {
        try {
            String message = messageInput.getText(); // Get the input message.
            if (message != null) {
                sendMessage(message); // Send the message to the server.
                messageInput.clear(); // Clear the input field after sending.
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends a text message to the server.
     *
     * @param message the message content.
     */
    private void sendMessage(String message) {
        try {
            InetAddress serverAddress = InetAddress.getByName(LoginController.SERVER_IP); // Get server address.

            String combinedMessage = "TEXT|" + USER_NAME + "|" + message; // Format the message.
            byte[] sendData = combinedMessage.getBytes(); // Convert to byte array.

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, LoginController.SERVER_PORT); // Create packet.
            LoginController.clientSocket.send(sendPacket); // Send packet to the server.
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Appends a text message to the chat UI.
     *
     * @param userName the username of the sender.
     * @param message  the message content.
     */
    private void appendMessage(String userName, String message) {
        Platform.runLater(() -> {
            userColors.putIfAbsent(userName, generateRandomColor()); // If the user has not yet set a color, we generate one.
            Color userColor = userColors.get(userName); // We take the user's color from the map.

            Text userText = new Text(userName + ": "); // Format message with username.
            userText.setFill(userColor); // Set text color.

            Text messageText = new Text(message + "\n"); // Format message with message.
            messageText.setFill(Color.WHITE); // Set text color.

            chatHistoryTextFlow.getChildren().addAll(userText, messageText); // Add message to chat UI.
            scrollPane.layout();
            scrollPane.setVvalue(1.0);
        });
    }


    /**
     * Handles the "Send Image" button click event by selecting and sending an image file.
     */    @FXML
    private void sendImageButton() {
        try {
            FileChooser fileChooser = new FileChooser(); // Create file chooser.
            fileChooser.setTitle("Choose Image File");

            File selectedFile = fileChooser.showOpenDialog(null); // Show dialog to choose file.

            if (selectedFile != null) {
                sendImage(selectedFile); // Send the selected image.
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends an image file to the server.
     *
     * @param imageFile the image file to send.
     */
    private void sendImage(File imageFile) {
        try {
            DatagramSocket clientSocket = new DatagramSocket(); // Create a new socket for sending.
            InetAddress serverAddress = InetAddress.getByName(LoginController.SERVER_IP); // Get server address.

            String fileName = imageFile.getName(); // Get the file name.
            String combinedMessage = USER_NAME + "|" + fileName; // Format message with file name.
            byte[] fileNameBytes = combinedMessage.getBytes(); // Convert to byte array.
            DatagramPacket fileStatPacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, serverAddress, LoginController.SERVER_PORT); // Create packet.
            clientSocket.send(fileStatPacket); // Send file information.

            byte[] fileByteArray = readFileToByteArray(imageFile); // Read the file into a byte array.
            sendFile(clientSocket, fileByteArray, serverAddress); // Send file data.
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Reads a file and converts its content to a byte array.
     *
     * @param file the file to read.
     * @return the byte array containing the file's data.
     */
    public static byte[] readFileToByteArray(File file) {
        FileInputStream fis;
        byte[] bArray = new byte[(int) file.length()]; // Create byte array of file size.
        try {
            fis = new FileInputStream(file); // Open file input stream.
            int bytesRead = fis.read(bArray); // Read file data into array.
            while (bytesRead != -1 && fis.available() > 0) {
                bytesRead = fis.read(bArray, bytesRead, fis.available()); // Read remaining data.
            }
            fis.close(); // Close the file input stream.
        } catch (IOException ioExp) {
            System.out.println(ioExp.getMessage());
            ioExp.printStackTrace();
        }
        return bArray; // Return the byte array.
    }

    /**
     * Sends file data in chunks over a UDP connection.
     *
     * @param socket          the UDP socket to use for sending.
     * @param fileByteArray   the byte array containing the file's data.
     * @param serverAddress   the server's IP address.
     */
    private static void sendFile(DatagramSocket socket, byte[] fileByteArray, InetAddress serverAddress) {
        try {
            int sequenceNumber = 0; // Sequence number for each chunk.
            int ackSequence = 0;

            for (int i = 0; i < fileByteArray.length; i = i + 1021) { // Loop through the file data in chunks.
                sequenceNumber += 1; // Increment sequence number.
                byte[] message = new byte[1024]; // Create message buffer.
                message[0] = (byte) (sequenceNumber >> 8); // Store high byte of sequence number.
                message[1] = (byte) (sequenceNumber); // Store low byte of sequence number.

                // Have we reached the end of the file?
                if ((i + 1021) >= fileByteArray.length) { // We have reached the end of the file (last datagram to send).
                    message[2] = (byte) (1); // Indicate end of file.
                    System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length - i); // Copy remaining data.
                } else { // We have not reached the end of the file, we are still sending datagrams.
                    message[2] = (byte) (0); // Indicate not end of file.
                    System.arraycopy(fileByteArray, i, message, 3, 1021); // Copy chunk data.
                }

                DatagramPacket sendPacket = new DatagramPacket(message, message.length, serverAddress, LoginController.SERVER_PORT); // Create packet.
                socket.send(sendPacket); // Send the packet.
                System.out.println("Sent: Sequence number = " + sequenceNumber); // Log sent packet.

                boolean ackRec; // Was the datagram received?
                while (true) {
                    byte[] ack = new byte[2]; // Buffer for acknowledgment.
                    DatagramPacket backpack = new DatagramPacket(ack, ack.length);

                    try {
                        socket.setSoTimeout(50); // Set timeout for acknowledgment.
                        socket.receive(backpack); // Wait for acknowledgment.
                        ackSequence = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff); // Calculate the sequence number.
                        ackRec = true; // We received the ack.
                    } catch (SocketTimeoutException e) {
                        System.out.println("Socket timed out waiting for ack");
                        ackRec = false; // We didn't receive the ack.
                    }

                    if ((ackSequence == sequenceNumber) && (ackRec)) {
                        System.out.println("Ack received: Sequence Number = " + ackSequence);
                        break; // Acknowledgment matches sent sequence number.
                    } else {
                        socket.send(sendPacket); // Resend packet on timeout.
                        System.out.println("Resending: Sequence Number = " + sequenceNumber);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Appends an image to the chat UI.
     *
     * @param userName the username of the sender.
     * @param image    the path to the image file.
     */
    private void appendImage(String userName, String image) {
        Platform.runLater(() -> {
            userColors.putIfAbsent(userName, generateRandomColor()); // If the user has not yet set a color, we generate one.
            Color userColor = userColors.get(userName); // We take the user's color from the map.

            Label userNameLabel = new Label(userName + ":"); // Label for username.
            userNameLabel.setTextFill(userColor); // Set label color.

            ImageView imageView = new ImageView(new Image(image)); // Create ImageView with the image.
            imageView.setPreserveRatio(true); // Preserve aspect ratio.
            imageView.setFitWidth(200); // Set image width.
            imageView.setFitHeight(200); // Set image height.
            Text spacer = new Text("\n"); // Add vertical space between messages

            chatHistoryTextFlow.getChildren().addAll(userNameLabel, imageView, spacer); // Add username and image to chat UI.
        });
    }
}