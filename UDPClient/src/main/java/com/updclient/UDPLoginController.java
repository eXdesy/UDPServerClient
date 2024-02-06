package com.updclient;

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

public class UDPLoginController {
    // Definición de constantes:
    public static final int CLIENT_PORT = 6011; // Puerto del cliente.
    public static final int SERVER_PORT = 5010; // Puerto del servidor.
    public static final int BUFFER_SIZE = 1024; // Tamaño del buffer.
    public static final String IP = "192.168.0.18"; // dirección IP del servidor.
    public static  DatagramSocket clientSocket; // Declaración de un objeto DatagramSocket para el cliente.

    @FXML
    private TextField username;  // Campo de texto para el nombre de usuario.
    @FXML
    private Button enterButton;  // Botón de entrada.

    @FXML
    public void initialize() {
        try {
            // Inicialización del socket del cliente en el puerto especificado.
            clientSocket = new DatagramSocket(CLIENT_PORT);
            // Configuración de la acción cuando se hace clic en el botón de entrada.
            enterButton.setOnAction(event -> {
                String user = username.getText(); // Obtención del nombre de usuario ingresado.
                // Verificación de la disponibilidad del nombre de usuario.
                if (verificarNombreUsuario(user)) {
                    abrirVentanaTabla(user); // Abre la ventana principal si el nombre de usuario está disponible.
                }
            });
        } catch (Exception e) {
            e.printStackTrace(); // Imprime la traza de la excepción en caso de error.
        }
    }

    private boolean verificarNombreUsuario(String username) {
        try {
            // Envía el nombre de usuario al servidor para su verificación.
            String message = "CHECK_USERNAME|" + username;
            byte[] sendData = message.getBytes();
            InetAddress serverAddress = InetAddress.getByName(IP);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
            clientSocket.send(sendPacket);

            // Recibe la respuesta del servidor.
            byte[] receiveData = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            // Convierte la respuesta a String y devuelve true si el nombre de usuario está disponible.
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            return response.equals("USERNAME_AVAILABLE");
        } catch (Exception e) {
            // Muestra un mensaje al usuario indicando que el nombre no está disponible.
            System.out.println("El nombre de usuario no está disponible. Introduce otro nombre.");
            return false;
        }
    }

    public static void stopClient() {
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
        }
    }

    private void abrirVentanaTabla(String user) {
        try {
            // Carga el archivo FXML de la ventana principal.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
            Parent root = loader.load();
            // Obtiene el controlador de la ventana principal.
            UDPChatController chatController = loader.getController();
            chatController.setUserName(user); // Pasa el nombre de usuario al controlador de la ventana principal.
            // Obtiene el Stage de la ventana actual y configura la nueva escena.
            Stage stage = (Stage) enterButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

            stage.setOnCloseRequest(event -> stopClient());
        } catch (IOException e) {
            throw new RuntimeException("Error al abrir la ventana" + e.getMessage());
        }
    }
}
