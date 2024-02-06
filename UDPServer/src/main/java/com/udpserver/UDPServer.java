package com.udpserver;

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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class UDPServer {
    // Puerto en el que el servidor está escuchando conexiones.
    private static final int SERVER_PORT = 5010;
    // Tamaño del búfer para almacenar datos recibidos.
    private static final int BUFFER_SIZE = 1024;
    // Ruta de guardado para las imágenes recibidas por el servidor.
    private static final String SAVE_RUTE = "C:\\Users\\Admin\\Downloads\\ServerImages";
    // Lista de direcciones IP de clientes conectados.
    private static final List<InetAddress> CLIENT_IP_LIST = new ArrayList<>();
    // Lista de puertos de clientes conectados.
    private static final List<Integer> CLIENT_PORT_LIST = new ArrayList<>();
    // Conjunto de nombres de usuario disponibles para conexión.
    private static final Set<String> AVAILABLE_USERNAMES = new HashSet<>();

    private static DatagramSocket serverSocket;
    @FXML
    private TextFlow chatHistoryTextFlow;


    public void initialize() {
        try {
            serverSocket = new DatagramSocket(SERVER_PORT);
            startServer(serverSocket);
        } catch (Exception e) {
            // Se imprime la traza de la excepción en caso de error.
            e.printStackTrace();
        }
    }

    private void startServer(DatagramSocket serverSocket) {
        new Thread(() -> {
            try {
                // Imprime en la consola que el servidor está escuchando en un puerto específico.
                log("Servidor iniciado en el puerto " + SERVER_PORT);
                System.out.println("Servidor iniciado en el puerto " + SERVER_PORT);

                // Bucle principal del servidor.
                while (true) {
                    // Prepara el búfer y el paquete para recibir datos
                    byte[] receiveData = new byte[BUFFER_SIZE];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    // Recibe el paquete de datos del cliente
                    serverSocket.receive(receivePacket);

                    // Obtiene la dirección IP y el puerto del cliente
                    InetAddress clientAddress = receivePacket.getAddress();
                    int clientPort = receivePacket.getPort();

                    // Convierte los datos recibidos a una cadena de texto
                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    // Divide el mensaje utilizando el delimitador "|"
                    String[] messageParts = message.split("\\|");

                    // Verifica el tipo de mensaje y realiza acciones correspondientes:
                    if (messageParts[0].equals("CHECK_USERNAME")) {
                        // Verifica si el nombre de usuario está disponible
                        String username = messageParts[1];
                        if (!AVAILABLE_USERNAMES.contains(username)) {
                            // Envía una respuesta al cliente indicando que el nombre de usuario está disponible
                            sendResponse(clientAddress, clientPort, "USERNAME_AVAILABLE");
                            // Agrega el nombre de usuario a la lista de disponibles
                            AVAILABLE_USERNAMES.add(username);

                            // Agrega la dirección IP y el puerto a las listas correspondientes:
                            if (!CLIENT_IP_LIST.contains(clientAddress) || !CLIENT_PORT_LIST.contains(clientPort)) {
                                CLIENT_IP_LIST.add(clientAddress);
                                CLIENT_PORT_LIST.add(clientPort);
                            }

                            // Imprime en la consola la información del cliente.
                            log("Cliente aceptado - PORT: " + clientPort + ", IP: " + clientAddress);
                            System.out.println("Cliente aceptado - PORT: " + clientPort + ", IP: " + clientAddress);
                        } else {
                            // Envía una respuesta al cliente indicando que el nombre de usuario no está disponible.
                            sendResponse(clientAddress, clientPort, "USERNAME_UNAVAILABLE");
                        }
                    } else if (messageParts[0].equals("TEXT")) {
                        // Lógica para manejar mensajes de texto.
                        receiveMessage(message);
                    } else {
                        // Lógica para manejar imágenes.
                        receiveImage(receivePacket, serverSocket);
                    }
                }
            } catch (Exception e) {
                // Imprime en la consola cualquier excepción ocurrida
                log("Error al iniciar el servidor en el puerto " + CLIENT_PORT_LIST);
                Platform.exit();
            }
        }).start();
    }

    // Método para enviar una respuesta a un cliente a través de un socket UDP:
    private void sendResponse(InetAddress clientAddress, int clientPort, String response) {
        try {
            // Convierte la respuesta (String) en un arreglo de bytes.
            byte[] sendData = response.getBytes();

            // Crea un nuevo socket UDP para enviar la respuesta.
            DatagramSocket responseSocket = new DatagramSocket();
            // Crea un paquete UDP que contiene los datos a enviar, la longitud de los datos,
            // la dirección del cliente y el puerto del cliente.
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);

            // Envía el paquete a través del socket UDP.
            responseSocket.send(sendPacket);
            // Cierra el socket después de enviar la respuesta.
            responseSocket.close();
            log("Respuesta enviada al cliente - IP: " + clientAddress + ", Puerto: " + clientPort + ", Respuesta: " + response);
            System.out.println("Respuesta enviada al cliente - IP: " + clientAddress + ", Puerto: " + clientPort + ", Respuesta: " + response);
        } catch (Exception e) {
            // En caso de error, imprime la traza de la excepción.
            log("Error al enviar respuesta al cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Método para agregar mensajes a la consola de la interfaz:
    public void log(String message) {
        // Ejecuta la operación en el hilo de la interfaz de usuario (JavaFX).
        Platform.runLater(() -> {
            // Crea un nuevo objeto Text con el nombre de usuario, el mensaje y un salto de línea.
            Text AddMessage = new Text(message + "\n");
            // Agrega el mensaje al flujo de texto del historial del chat.
            chatHistoryTextFlow.getChildren().add(AddMessage);
        });
    }
    // Si el socket no es nulo y no está cerrado cierra el servidor
    public static void stopServer() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    // Método para recibir mensajes de texto:
    private void receiveMessage(String message) {
        // Divide el mensaje en partes usando el carácter "|" como delimitador/
        String[] messageParts = message.split("\\|");
        // Obtiene el nombre de usuario del mensaje/
        String receivedUserName = messageParts[1];
        // Obtiene el mensaje del usuario del mensaje/
        String receivedMessage = messageParts[2];

        // Imprime en la consola el nombre de usuario y el mensaje recibido/
        log("Mensaje recibido de " + receivedUserName + ": " + receivedMessage);
        System.out.println("Mensaje recibido de " + receivedUserName + ": " + receivedMessage);

        // Comprueba si el mensaje es igual a "STOP":
        if (receivedMessage.equals("STOP")) {
            // Si el mensaje es igual a "STOP", imprime en la consola y cierra el servidor/
            log("Servidor detenido...");
            System.out.println("Servidor detenido...");
            // Se para el servidor
            stopServer();
            // Se cierra la aplicación
            Platform.exit();
        } else {
            // Si el mensaje no es "STOP", reenvía el mensaje a los clientes conectados.
            forwardMessageToClients(message);
        }
    }
    // Logica para reenviar mensajes a los clientes conectados:
    private void forwardMessageToClients(String message) {
        try {
            // Convierte el mensaje en un arreglo de bytes.
            byte[] sendData = message.getBytes();
            // Crea un nuevo socket Datagram para enviar datos.
            DatagramSocket clientSocket = new DatagramSocket();

            // Bucle por cada cliente en la lista.
            for (int i = 0; i < CLIENT_IP_LIST.size(); i++) {
                // Obtiene la dirección IP y el puerto del cliente.
                InetAddress clientAddress = CLIENT_IP_LIST.get(i);
                int clientPort = CLIENT_PORT_LIST.get(i);

                // Crea un paquete Datagram con los datos y la dirección del cliente.
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                // Envía el paquete al cliente.
                clientSocket.send(sendPacket);
            }

            // Cierra el socket después de enviar el mensaje a todos los clientes.
            clientSocket.close();

            log("Mensaje reenviado a todos los clientes");
            System.out.println("Mensaje reenviado a todos los clientes");
        } catch (Exception e) {
            // Imprime la traza de la excepción si se produce algún error.
            log("Error al reenviar mensaje a los clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para recibir mensajes de imagen:
    private void receiveImage(DatagramPacket receivePacket, DatagramSocket serverSocket) {
        try {
            // Leyendo el nombre en bytes.
            byte [] data = receivePacket.getData();
            // Convirtiendo el nombre a cadena de texto.
            String combinedMessage  = new String(data, 0, receivePacket.getLength());
            String[] parts = combinedMessage.split("\\|");
            String userName = parts[0];
            String fileName = parts[1];

            // Ruta donde se guardará la imagen.
            String savedImagePath = SAVE_RUTE + "\\" + fileName;
            // Creando el archivo.
            File f = new File(savedImagePath);
            // Creando el flujo a través del cual escribiremos el contenido del archivo.
            FileOutputStream outToFile = new FileOutputStream(f);
            // Recibiendo el archivo.
            saveImage(outToFile, serverSocket);
            // Reenviando las imágenes a los clientes.
            forwardImagesToClients(f, userName);
        } catch (FileNotFoundException e) {
            // Lanzando una excepción de tiempo de ejecución en caso de no encontrar el archivo.
            throw new RuntimeException(e);
        }
    }
    // Logica para reenviar imagenes a los clientes conectados:
    private void forwardImagesToClients(File imageFile, String userName) {
        try {
            // Creando el socket del cliente.
            DatagramSocket clientSocket = new DatagramSocket();
            String fileName = imageFile.getName();
            // Combinando el nombre de usuario y el nombre del archivo.
            String combinedMessage = userName + "|" + fileName;
            byte[] fileNameBytes = combinedMessage.getBytes();

            // Bucle por cada cliente:
            for (int i = 0; i < CLIENT_IP_LIST.size(); i++) {
                InetAddress clientAddress = CLIENT_IP_LIST.get(i);
                int clientPort = CLIENT_PORT_LIST.get(i);

                // Enviando el nombre del archivo al servidor.
                DatagramPacket fileStatPacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, clientAddress, clientPort);
                clientSocket.send(fileStatPacket);
                log("Archivo enviado a " + clientAddress + " " + clientPort);
                System.out.println("Archivo enviado a " + clientAddress + " " + clientPort);

                // Leyendo el archivo y enviándolo al servidor.
                byte[] fileByteArray = readFileToByteArray(imageFile);
                sendFile(clientSocket, fileByteArray, clientAddress, clientPort);
            }

            // Cerrando el socket del cliente.
            clientSocket.close();
        } catch (Exception e) {
            // Imprimiendo la traza de la excepción en caso de error.
            log("Error al reenviar imágenes a los clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Logica para recibir imagen y guardarlo:
    private void saveImage(FileOutputStream outToFile, DatagramSocket socket) {
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
                    log("Número de secuencia esperado: " + (foundLast + 1) + " pero se recibió " + sequenceNumber + ". DESCARTANDO");
                    System.out.println("Número de secuencia esperado: " + (foundLast + 1) + " pero se recibió " + sequenceNumber + ". DESCARTANDO");
                    // Reenviar el acuse de recibo.
                    sendAck(foundLast, socket, address, port);
                }
                // Verificar el último datagrama.
                if (flag) {
                    log("Imagen recibida");
                    System.out.println("Imagen recibida");
                    outToFile.close();
                    break;
                }
            }
        } catch (IOException e) {
            log("Error al enviar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Método que envía un acuse de recibo (acknowledgement) a través de un socket UDP:
    private void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) {
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
            log("Acuse de recibo enviado: Número de secuencia = " + foundLast);
            System.out.println("Acuse de recibo enviado: Número de secuencia = " + foundLast);
        } catch (Exception e) {
            // Se imprime la traza de la excepción en caso de error durante el envío del acuse de recibo.
            log("Error al enviar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Logica para transformar imagen a bytes y enviarlo:
    private void sendFile(DatagramSocket socket, byte[] fileByteArray, InetAddress address, int port) {
        try {
            log("Enviando file to: " + address + " " + port);
            System.out.println("Enviando file");
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
                DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, port);
                socket.send(sendPacket);
                // Enviando los datos.
                log("Sent: Sequence number = " + sequenceNumber);
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
                        log("Socket timed out waiting for ack");
                        System.out.println("Socket timed out waiting for ack");
                        ackRec = false;
                    }

                    if ((ackSequence == sequenceNumber) && (ackRec)) {
                        // Si el paquete se recibió correctamente se puede enviar el siguiente paquete.
                        log("Ack received: Sequence Number = " + ackSequence);
                        System.out.println("Ack received: Sequence Number = " + ackSequence);
                        break;
                    } else {
                        // El paquete no fue recibido, por lo que lo reenviamos.
                        socket.send(sendPacket);
                        log("Resending: Sequence Number = " + sequenceNumber);
                        System.out.println("Resending: Sequence Number = " + sequenceNumber);
                    }
                }
            }
        } catch (Exception e) {
            log("Error al enviar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Método que toma un objeto de tipo File como parámetro:
    public byte[] readFileToByteArray(File file) {
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
}