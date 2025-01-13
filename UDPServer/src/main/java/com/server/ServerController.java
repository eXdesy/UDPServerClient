package com.server;

import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ServerController {
    private static final int SERVER_PORT = 5010; // Port on which the server will listen for connections.
    private static final int BUFFER_SIZE = 1024; // Buffer size for receiving data packets.
    private static final String SAVE_RUTE = "C:\\Users\\Admin\\Downloads\\ServerImages"; // Path where received images will be saved.
    private static final List<InetAddress> CLIENT_IP_LIST = new ArrayList<>(); // List of connected clients' IP addresses.
    private static final List<Integer> CLIENT_PORT_LIST = new ArrayList<>(); // List of connected clients' ports.
    private static final Set<String> AVAILABLE_USERNAMES = new HashSet<>(); // Set to keep track of available usernames.
    private static DatagramSocket serverSocket; // Socket used for server communication.

    @FXML
    private TextFlow chatHistoryTextFlow;
    @FXML
    private ScrollPane scrollPane;

    /**
     * Initializes the server and starts listening for connections.
     */
    public void initialize() {
        try {
            serverSocket = new DatagramSocket(SERVER_PORT);
            startServer(serverSocket);
        } catch (Exception e) {
            e.printStackTrace(); // Print the exception for debugging.
        }
    }

    /**
     * Logs messages to the chat history in the UI.
     *
     * @param message The message to be displayed.
     */
    public void log(String message) {
        Platform.runLater(() -> {
            Text addMessage = new Text(message + "\n");
            addMessage.setFill(Color.WHITE); // Set text color to white.
            chatHistoryTextFlow.getChildren().add(addMessage); // Add the message to the chat history.
            scrollPane.layout(); // Update scrollPane layout.
            scrollPane.setVvalue(1.0); // Scroll to the bottom.
        });
    }

    /**
     * Starts the server in a separate thread.
     *
     * @param serverSocket The DatagramSocket used for server communication.
     */
    private void startServer(DatagramSocket serverSocket) {
        new Thread(() -> {
            try {
                log("Server started on port " + SERVER_PORT);
                while (true) {
                    byte[] receiveData = new byte[BUFFER_SIZE]; // Buffer for incoming data.
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket); // Receive a packet from a client.

                    InetAddress clientAddress = receivePacket.getAddress(); // Client IP address.
                    int clientPort = receivePacket.getPort(); // Client port.

                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength()); // Decode message.
                    String[] messageParts = message.split("\\|"); // Split message into parts.

                    if (messageParts[0].equals("CHECK_USERNAME")) { // Handle different message types.
                        handleUsernameCheck(messageParts[1], clientAddress, clientPort);
                    } else if (messageParts[0].equals("TEXT")) {
                        receiveMessage(message);
                    } else {
                        receiveImage(receivePacket, serverSocket);
                    }
                }
            } catch (Exception e) {
                log("Error starting server on port " + SERVER_PORT);
                Platform.exit(); // Exit application on error.
            }
        }).start();
    }

    /**
     * Handles username availability checks from clients.
     *
     * @param username      The username to be checked.
     * @param clientAddress The IP address of the client requesting the check.
     * @param clientPort    The port of the client requesting the check.
     */
    private void handleUsernameCheck(String username, InetAddress clientAddress, int clientPort) {
        if (!AVAILABLE_USERNAMES.contains(username)) {
            sendResponse(clientAddress, clientPort, "USERNAME_AVAILABLE"); // Notify client username is available.
            AVAILABLE_USERNAMES.add(username); // Add username to the set.

            // Add client to the list if not already present.
            if (!CLIENT_IP_LIST.contains(clientAddress) || !CLIENT_PORT_LIST.contains(clientPort)) {
                CLIENT_IP_LIST.add(clientAddress);
                CLIENT_PORT_LIST.add(clientPort);
            }

            log("Client accepted - PORT: " + clientPort + ", IP: " + clientAddress);
        } else {
            sendResponse(clientAddress, clientPort, "USERNAME_UNAVAILABLE"); // Notify client username is taken.
        }
    }

    /**
     * Stops the server by closing the DatagramSocket.
     */
    public static void stopServer() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close(); // Close the server socket.
        }
    }

    /**
     * Sends a response message to a client.
     *
     * @param clientAddress The IP address of the client.
     * @param clientPort    The port of the client.
     * @param response      The response message to be sent.
     */
    private void sendResponse(InetAddress clientAddress, int clientPort, String response) {
        try {
            byte[] sendData = response.getBytes(); // Convert response to bytes.
            DatagramSocket responseSocket = new DatagramSocket();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);

            responseSocket.send(sendPacket); // Send the packet.
            responseSocket.close(); // Close the socket.
            log("Response sent to client - IP: " + clientAddress + ", Port: " + clientPort + ", Response: " + response);
        } catch (Exception e) {
            log("Error sending response to client: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging.
        }
    }

    /**
     * Handles received text messages.
     *
     * @param message The message received from a client.
     */
    private void receiveMessage(String message) {
        String[] messageParts = message.split("\\|"); // Split message into parts.
        String receivedUserName = messageParts[1]; // Extract username.
        String receivedMessage = messageParts[2]; // Extract message content.
        log("Message received from " + receivedUserName + ": " + receivedMessage);

        if (receivedMessage.equals("STOP")) {
            log("Server stopped...");
            stopServer(); // Stop the server if "STOP" command is received.
            Platform.exit(); // Exit the application.
        } else {
            forwardMessageToClients(message); // Forward message to all connected clients.
        }
    }

    /**
     * Forwards a message to all connected clients.
     *
     * @param message The message to be forwarded.
     */    private void forwardMessageToClients(String message) {
        try {
            byte[] sendData = message.getBytes(); // Convert message to bytes.
            DatagramSocket clientSocket = new DatagramSocket();

            for (int i = 0; i < CLIENT_IP_LIST.size(); i++) {
                InetAddress clientAddress = CLIENT_IP_LIST.get(i);
                int clientPort = CLIENT_PORT_LIST.get(i);

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                clientSocket.send(sendPacket); // Send packet to each client.
            }
            clientSocket.close(); // Close the socket after forwarding.

            log("Message forwarded to all clients");
        } catch (Exception e) {
            log("Error forwarding message to clients: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging.
        }
    }

    /**
     * Handles received image data and saves it to the server's storage.
     *
     * @param receivePacket The DatagramPacket containing the image data.
     * @param serverSocket  The DatagramSocket used to receive the image.
     */
    private void receiveImage(DatagramPacket receivePacket, DatagramSocket serverSocket) {
        try {
            byte[] data = receivePacket.getData(); // Get packet data.
            String combinedMessage = new String(data, 0, receivePacket.getLength()); // Decode message.
            String[] parts = combinedMessage.split("\\|");
            String userName = parts[0]; // Extract username.
            String fileName = parts[1]; // Extract filename.

            String savedImagePath = SAVE_RUTE + "\\" + fileName; // Build save path.
            File file = new File(savedImagePath); // Create file object.
            FileOutputStream outToFile = new FileOutputStream(file); // Open file output stream.
            saveImage(outToFile, serverSocket); // Save the image.
            forwardImagesToClients(file, userName); // Forward image to clients.
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e); // Throw runtime exception if file not found.
        }
    }

    /**
     * Forwards images to all connected clients.
     *
     * @param imageFile The file containing the image to be forwarded.
     * @param userName  The username of the client sending the image.
     */
    private void forwardImagesToClients(File imageFile, String userName) {
        try {
            DatagramSocket clientSocket = new DatagramSocket(); // Create socket.
            String fileName = imageFile.getName(); // Get file name.

            String combinedMessage = userName + "|" + fileName; // Combine username and filename.
            byte[] fileNameBytes = combinedMessage.getBytes();

            for (int i = 0; i < CLIENT_IP_LIST.size(); i++) {
                InetAddress clientAddress = CLIENT_IP_LIST.get(i);
                int clientPort = CLIENT_PORT_LIST.get(i);

                DatagramPacket fileStatPacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, clientAddress, clientPort);
                clientSocket.send(fileStatPacket); // Send file details.
                log("File sent to " + clientAddress + " " + clientPort);

                byte[] fileByteArray = readFileToByteArray(imageFile); // Read file as bytes.
                sendFile(clientSocket, fileByteArray, clientAddress, clientPort); // Send file content.
            }
            clientSocket.close(); // Close the socket.
        } catch (Exception e) {
            log("Error forwarding images to clients: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging.
        }
    }

    /**
     * Saves an image received via a DatagramSocket. It uses a sequence number to ensure the packets
     * are written in the correct order and sends acknowledgments for each packet received.
     *
     * @param outToFile The FileOutputStream to write the received data.
     * @param socket    The DatagramSocket used for receiving packets.
     */
    private void saveImage(FileOutputStream outToFile, DatagramSocket socket) {
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
                    log("Expected sequence number: " + (foundLast + 1) + " but received " + sequenceNumber + ". DISCARDING");
                    sendAck(foundLast, socket, address, port); // Send an acknowledgment for the last successfully received packet.
                }

                if (flag) { // If this is the last packet, close the file and exit the loop.
                    log("Image received");
                    outToFile.close();
                    break;
                }
            }
        } catch (IOException e) {
            log("Error forwarding: " + e.getMessage());
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
    private void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) {
        try {
            byte[] ackPacket = new byte[2]; // Buffer for acknowledgment packet.
            ackPacket[0] = (byte) (foundLast >> 8); // High byte of the sequence number.
            ackPacket[1] = (byte) (foundLast); // Low byte of the sequence number.

            // Create a DatagramPacket with the acknowledgment data.
            DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);
            socket.send(acknowledgement); // Send the acknowledgment packet to the sender.
            log("Acknowledgement Sent: Sequence Number = " + foundLast);
        } catch (Exception e) {
            log("Error forwarding: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends file data in chunks over a UDP connection.
     *
     * @param socket          the UDP socket to use for sending.
     * @param fileByteArray   the byte array containing the file's data.
     * @param port   the server's IP address.
     */
    private void sendFile(DatagramSocket socket, byte[] fileByteArray, InetAddress address, int port) {
        try {
            int sequenceNumber = 0; // Sequence number for each chunk.
            int ackSequence = 0;
            log("Sent file to: " + address + " " + port);

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

                DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, port); // Create packet.
                socket.send(sendPacket); // Send the packet.
                log("Sent: Sequence number = " + sequenceNumber);// Log sent packet.

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
                        log("Socket timed out waiting for ack");
                        ackRec = false; // We didn't receive the ack.
                    }

                    if ((ackSequence == sequenceNumber) && (ackRec)) {
                        log("Ack received: Sequence Number = " + ackSequence);
                        break; // Acknowledgment matches sent sequence number.
                    } else {
                        socket.send(sendPacket); // Resend packet on timeout.
                        log("Resending: Sequence Number = " + sequenceNumber);
                    }
                }
            }
        } catch (Exception e) {
            log("Error forwarding: " + e.getMessage());
            e.printStackTrace();
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
            ioExp.printStackTrace();
        }
        return bArray; // Return the byte array.
    }
}