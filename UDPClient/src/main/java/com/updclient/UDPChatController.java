package com.updclient;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.control.Label;
import javafx.scene.image.Image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class UDPChatController {
    private String userName; // Guardar nombre de usuario
    public static final String SAVE_RUTE = "C:\\Users\\Admin\\Downloads\\ClientImages"; // Ruta para imágenes

    @FXML
    private TextField messageInput;
    @FXML
    private TextFlow chatHistoryTextFlow;

    public void initialize() {
        try {
            receiveMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Establece el nombre de usuario.
    public void setUserName(String userName) {
        this.userName = userName;
    }

    // Este método recibe mensajes en un hilo separado para no bloquear la interfaz de usuario:
    private void receiveMessage() {
        new Thread(() -> {
            try {
                // Bucle infinito para recibir mensajes continuamente.
                while (true) {
                    // Se crea un arreglo de bytes para almacenar los datos recibidos.
                    byte[] receiveData = new byte[UDPLoginController.BUFFER_SIZE];
                    // Se prepara un DatagramPacket para recibir los datos.
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    // Se recibe el paquete utilizando el socket del controlador de login UDP.
                    UDPLoginController.clientSocket.receive(receivePacket);

                    // Se convierten los datos recibidos a una cadena de texto.
                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    // Se divide el mensaje en partes usando el carácter "|" como delimitador.
                    String[] messageParts = message.split("\\|");

                    // Se verifica si el mensaje es de tipo "TEXT".
                    if (messageParts[0].equals("TEXT")) {
                        // Se extraen el nombre de usuario y el tipo de mensaje.
                        String receivedUserName = messageParts[1];
                        String messageType = messageParts[2];

                        // Se imprime en la consola el nombre de usuario y el tipo de mensaje.
                        System.out.println(receivedUserName + " - " + messageType);

                        // Se verifica si el mensaje es de otro usuario o del usuario actual y se agrega al historial de mensajes.
                        if (!receivedUserName.equals(userName)) {
                            appendMessage(receivedUserName, messageType, Color.BLUE);  // Color para mensajes de otros usuarios.
                        } else {
                            appendMessage(userName, messageType, Color.GREEN);  // Color para tus propios mensajes.
                        }
                    }
                    // Si el mensaje no es de tipo "TEXT", se asume que es un mensaje de imagen.
                    else {
                        // Se obtiene el nombre de usuario y el nombre del archivo de imagen.
                        byte [] data = receivePacket.getData();
                        // Converting the name to string
                        String combinedMessage  = new String(data, 0, receivePacket.getLength());
                        String[] parts = combinedMessage.split("\\|");
                        String receivedUserName = parts[0];
                        String fileName = parts[1];

                        // Se construye la ruta donde se guardará la imagen.
                        String savedImagePath = SAVE_RUTE + "\\" + fileName;
                        // Se crea el objeto File para representar el archivo de imagen.
                        File f = new File(savedImagePath);
                        // Se crea un FileOutputStream para escribir el contenido del archivo.
                        FileOutputStream outToFile = new FileOutputStream(f);
                        // Se llama a la función saveImage para recibir y guardar la imagen.
                        saveImage(outToFile, UDPLoginController.clientSocket);

                        // Se verifica si la imagen es de otro usuario o del usuario actual y se agrega al historial de imágenes.
                        if (!receivedUserName.equals(userName)) {
                            appendImage(receivedUserName, savedImagePath, Color.BLUE);  // Color para mensajes de otros usuarios.
                        } else {
                            appendImage(userName, savedImagePath, Color.GREEN); // Color para tus propios mensajes.
                        }
                    }
                }
            } catch (Exception e) {
                // Se imprime la traza de la excepción en caso de error.
                e.printStackTrace();
                Platform.exit();
            }
        }).start();
    }

    // Métodos para agregar mensajes al área de chat desde cualquier hilo:
    private void appendMessage(String userName, String message, Color color) {
        // Ejecuta la operación en el hilo de la interfaz de usuario (JavaFX).
        Platform.runLater(() -> {
            // Crea un nuevo objeto Text con el nombre de usuario, el mensaje y un salto de línea.
            Text AddMessage = new Text(userName + ": " + message + "\n");
            // Establece el color del texto del mensaje.
            AddMessage.setFill(color);
            // Agrega el mensaje al flujo de texto del historial del chat.
            chatHistoryTextFlow.getChildren().add(AddMessage);
        });
    }
    // Métodos para agregar imágenes al área de chat desde cualquier hilo:
    private void appendImage(String userName, String image, Color color) {
        // Ejecuta la operación en el hilo de la interfaz de usuario (JavaFX).
        Platform.runLater(() -> {
            // Crea una etiqueta con el nombre de usuario y establece el color del texto.
            Label userNameLabel = new Label(userName + ":");
            // Establece el color del texto del mensaje.
            userNameLabel.setTextFill(color);

            // Crea un objeto ImageView con la imagen proporcionada.
            ImageView imageView = new ImageView(new Image(image));
            // Conserva la proporción original de la imagen.
            imageView.setPreserveRatio(true);
            // Ajusta el ancho de la imagen según sea necesario.
            imageView.setFitWidth(200);
            // Ajusta la altura de la imagen según sea necesario.
            imageView.setFitHeight(200);

            // Agrega la etiqueta y la imagen al VBox del historial del chat.
            chatHistoryTextFlow.getChildren().addAll(userNameLabel, imageView);
        });
    }

    // Método para enviar mensaje:
    @FXML
    private void sendMessageButton() {
        try {
            // Obtiene el texto ingresado en el campo de mensaje.
            String message = messageInput.getText();

            // Verifica si el mensaje no es nulo.
            if (message != null) {
                // Llama al método sendMessage para enviar el mensaje.
                sendMessage(message);
                // Limpia el campo de entrada de mensaje después de enviarlo.
                messageInput.clear();
            }
        } catch (Exception e) {
            // Imprime la traza de la excepción en caso de error.
            e.printStackTrace();
        }
    }
    // Logica para enviar mensaje:
    private void sendMessage(String message) {
        try {
            // Obtiene la dirección del servidor utilizando la IP definida en UDPLoginController
            InetAddress serverAddress = InetAddress.getByName(UDPLoginController.IP);

            // Combina el mensaje con el nombre de usuario y el formato "TEXT|userName|message"
            String combinedMessage = "TEXT|" + userName + "|" + message;
            // Convierte la cadena combinada en un array de bytes
            byte[] sendData = combinedMessage.getBytes();

            // Crea un paquete DatagramPacket con los datos del mensaje, la longitud, la dirección del servidor y el puerto del servidor
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, UDPLoginController.SERVER_PORT);
            // Envía el paquete al servidor utilizando el socket del controlador UDPLoginController
            UDPLoginController.clientSocket.send(sendPacket);
        } catch (Exception e) {
            // Imprime la traza de la excepción en caso de error
            e.printStackTrace();
        }
    }

    // Método para enviar imagen:
    @FXML
    private void sendImageButton() {
        try {
            // Crear un objeto FileChooser para permitir al usuario seleccionar un archivo de imagen.
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose Image File");

            // Mostrar el diálogo de selección de archivo y obtener el archivo seleccionado.
            File selectedFile = fileChooser.showOpenDialog(null);

            // Verificar si se seleccionó un archivo antes de continuar.
            if (selectedFile != null) {
                // Llamar al método sendImage() para procesar y enviar la imagen seleccionada.
                sendImage(selectedFile);
            }
        } catch (Exception e) {
            // Manejar cualquier excepción imprevista e imprimir la traza de la pila.
            e.printStackTrace();
        }
    }
    // Logica para enviar imagen:
    private void sendImage(File imageFile) {
        try {
            // Crear un socket Datagram para la comunicación de red.
            DatagramSocket clientSocket = new DatagramSocket();
            // Obtener la dirección IP del servidor desde la clase UDPLoginController.
            InetAddress serverAddress = InetAddress.getByName(UDPLoginController.IP);

            // Enviar el nombre del archivo al servidor en forma de paquete Datagram.
            String fileName = imageFile.getName();
            String combinedMessage = userName + "|" + fileName;
            byte[] fileNameBytes = combinedMessage.getBytes();
            DatagramPacket fileStatPacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, serverAddress, UDPLoginController.SERVER_PORT);
            clientSocket.send(fileStatPacket);

            // Leer el contenido del archivo a un arreglo de bytes y enviarlo al servidor.
            byte[] fileByteArray = readFileToByteArray(imageFile);
            sendFile(clientSocket, fileByteArray, serverAddress);
        } catch (Exception ex) {
            // Manejar cualquier excepción imprevista e imprimir la traza de la pila.
            ex.printStackTrace();
        }
    }

    // Logica para transformar imagen a bytes y enviarlo:
    private static void sendFile(DatagramSocket socket, byte[] fileByteArray, InetAddress serverAddress) {
        try {
            System.out.println("Sending file");
            // Para ordenar.
            int sequenceNumber = 0;
            // Para ver si llegamos al final del archivo.
            boolean flag;
            // Para ver si el datagrama se recibió correctamente.
            int ackSequence = 0;

            for (int i = 0; i < fileByteArray.length; i = i + 1021) {
                sequenceNumber += 1;
                // Crear un mensaje
                byte[] message = new byte[1024];
                // Los primeros dos bytes de los datos son para control (integridad y orden del datagrama).
                message[0] = (byte) (sequenceNumber >> 8);
                message[1] = (byte) (sequenceNumber);

                // ¿Hemos llegado al final del archivo?
                if ((i + 1021) >= fileByteArray.length) {
                    // Llegamos al final del archivo (último datagrama a enviar).
                    flag = true;
                    message[2] = (byte) (1);
                } else {
                    // No hemos llegado al final del archivo, seguimos enviando datagramas.
                    flag = false;
                    message[2] = (byte) (0);
                }

                if (!flag) {
                    System.arraycopy(fileByteArray, i, message, 3, 1021);
                } else {
                    // Si es el último datagrama.
                    System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length - i);
                }

                // Los datos a enviar.
                DatagramPacket sendPacket = new DatagramPacket(message, message.length, serverAddress, UDPLoginController.SERVER_PORT);
                socket.send(sendPacket);
                // Enviando los datos.
                System.out.println("Sent: Sequence number = " + sequenceNumber);

                // ¿Se recibió el datagrama?
                boolean ackRec;
                while (true) {
                    // Cree otro paquete para el reconocimiento de datagramas.
                    byte[] ack = new byte[2];
                    DatagramPacket backpack = new DatagramPacket(ack, ack.length);

                    try {
                        // Esperando que el servidor envíe el acuse de recibo.
                        socket.setSoTimeout(50);
                        socket.receive(backpack);
                        // Calcular el número de secuencia.
                        ackSequence = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
                        // Recibimos el ack.
                        ackRec = true;
                    } catch (SocketTimeoutException e) {
                        // No recibimos un acuse de recibo.
                        System.out.println("Socket timed out waiting for ack");
                        ackRec = false;
                    }

                    if ((ackSequence == sequenceNumber) && (ackRec)) {
                        // Si el paquete se recibió correctamente se puede enviar el siguiente paquete.
                        System.out.println("Ack received: Sequence Number = " + ackSequence);
                        break;
                    } else {
                        // El paquete no fue recibido, por lo que lo reenviamos.
                        socket.send(sendPacket);
                        System.out.println("Resending: Sequence Number = " + sequenceNumber);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Método que toma un objeto de tipo File como parámetro:
    public static byte[] readFileToByteArray(File file) {
        // Declara un objeto FileInputStream para leer bytes desde un archivo
        FileInputStream fis;
        // Crea un arreglo de bytes con la longitud del archivo
        byte[] bArray = new byte[(int) file.length()];
        try {
            // Inicializa el objeto FileInputStream con el archivo proporcionado
            fis = new FileInputStream(file);

            // Lee los bytes desde el archivo y almacena la cantidad de bytes leídos en la variable "bytesRead"
            int bytesRead = fis.read(bArray);

            // Mientras haya más bytes por leer y estén disponibles en el flujo de entrada
            while (bytesRead != -1 && fis.available() > 0) {
                // Lee los bytes restantes y actualiza la variable "bytesRead"
                bytesRead = fis.read(bArray, bytesRead, fis.available());
            }

            // Cierra el flujo de entrada después de leer todos los bytes
            fis.close();
        } catch (IOException ioExp) {
            // En caso de una excepción de E/S (IOException), imprime la traza de la excepción
            ioExp.printStackTrace();
        }
        // Devuelve el arreglo de bytes que contiene los datos del archivo
        return bArray;
    }

    // Logica para recibir imagen y guardarlo:
    private static void saveImage(FileOutputStream outToFile, DatagramSocket socket) {
        try {
            // ¿Hemos llegado al final del archivo?
            boolean flag;
            // Orden de las secuencias.
            int sequenceNumber;
            // La última secuencia encontrada.
            int foundLast = 0;

            while (true) {
                // Donde se almacena los datos del datagrama recibido.
                byte[] message = new byte[1024];
                // Donde almacenamos los datos que se escribirán en el archivo.
                byte[] fileByteArray = new byte[1021];
                // Recibir paquete y obtener los datos.
                DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
                socket.receive(receivedPacket);
                // Datos que se escribirán en el archivo.
                message = receivedPacket.getData();
                // Obtener puerto y dirección para enviar el acuse de recibo.
                InetAddress address = receivedPacket.getAddress();
                int port = receivedPacket.getPort();
                // Obtener número de secuencia.
                sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);
                // Verificar si llegamos al último datagrama (fin del archivo).
                flag = (message[2] & 0xff) == 1;
                // Si el número de secuencia es el último visto + 1, entonces es correcto.
                // Obtenemos los datos del mensaje y escribimos el acuse de recibo de que se ha recibido correctamente.
                if (sequenceNumber == (foundLast + 1)) {
                    // Establecer el último número de secuencia como el que acabamos de recibir.
                    foundLast = sequenceNumber;
                    // Obtener datos del mensaje.
                    System.arraycopy(message, 3, fileByteArray, 0, 1021);
                    // Escribir los datos recuperados en el archivo e imprimir el número de secuencia recibido.
                    outToFile.write(fileByteArray);
                    //System.out.println("Received: Sequence number:" + foundLast);
                    // Enviar acuse de recibo.
                    sendAck(foundLast, socket, address, port);
                } else {
                    System.out.println("Expected sequence number: " + (foundLast + 1) + " but received " + sequenceNumber + ". DISCARDING");
                    // Reenviar el acuse de recibo.
                    sendAck(foundLast, socket, address, port);
                }
                // Verificar el último datagrama.
                if (flag) {
                    System.out.println("Image received");
                    outToFile.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Método que envía un acuse de recibo (acknowledgement) a través de un socket UDP.
    private static void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) {
        try {
            // Se crea un array de bytes para almacenar el paquete de acuse de recibo (acknowledgement).
            byte[] ackPacket = new byte[2];

            // Se asignan los bytes correspondientes al número de secuencia del último paquete recibido.
            ackPacket[0] = (byte) (foundLast >> 8);
            ackPacket[1] = (byte) (foundLast);

            // Se crea un DatagramPacket que contiene el paquete de acuse de recibo, la longitud del paquete,
            // la dirección IP de destino y el puerto de destino.
            DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);

            // Se envía el paquete de acuse de recibo a través del socket.
            socket.send(acknowledgement);

            // Se imprime un mensaje (comentado) indicando el número de secuencia enviado en el acuse de recibo.
            System.out.println("Sent ack: Sequence Number = " + foundLast);
        } catch (Exception e) {
            // Se imprime la traza de la excepción en caso de error durante el envío del acuse de recibo.
            e.printStackTrace();
        }
    }
}