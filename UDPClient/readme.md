# Estructura del Cliente UDP
Cliente del chat basada en JavaFX que utiliza el protocolo UDP para la comunicación entre diferentes clientes a traves de servidor. Aquí está la explicación de la lógica del código y una guía básica para usarlo.

## `HelloApplication.java`:
Esta clase inicia la aplicación JavaFX y carga la interfaz gráfica de la ventana de inicio de sesión (`LoginWindow.fxml`).

- **Método `start`**:
  - Carga el archivo FXML de la ventana de inicio de sesión.
  - Configura la escena y muestra la ventana.

## `UPDLoginController.java`:
Este controlador maneja la lógica de la ventana de inicio de sesión.

- **Constantes**:
  - `CLIENT_PORT`: Puerto del cliente.
  - `SERVER_PORT`: Puerto del servidor.
  - `BUFFER_SIZE`: Tamaño del buffer.
  - `IP`: Dirección IP del servidor.
  - `clientSocket`: Objeto `DatagramSocket` para el cliente.

- **FXML Elements**:
  - `username`: Campo de texto para el nombre de usuario.
  - `enterButton`: Botón de entrada.

- **Método `initialize`**:
  - Inicializa el socket del cliente en el puerto especificado.
  - Configura la acción al hacer clic en el botón de entrada.
  - Verifica la disponibilidad del nombre de usuario y abre la ventana principal si está disponible.

```
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
```

- **Método `verificarNombreUsuario`**:
  - Envía el nombre de usuario al servidor para su verificación.
  - Espera la respuesta del servidor y devuelve `true` si el nombre de usuario está disponible.

```
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
```

- **Método `abrirVentanaTabla`**:
  - Carga el archivo FXML de la ventana principal (`MainWindow.fxml`).
  - Obtiene el controlador de la ventana principal y pasa el nombre de usuario.
  - Configura la nueva escena y muestra la ventana principal.

```
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
```

## `UPDChatController.java`:
Esta clase maneja la lógica de envío y recepción de mensajes, así como la lógica para enviar y recibir imágenes (`MainWindow.fxml`).

- **Atributos y Constantes**:
  - `userName`: Almacena el nombre de usuario actual.
  - `SAVE_RUTE`: Ruta donde se guardarán las imágenes recibidas. IMPORTANTE! NO OLVIDES CAMBIAR LA RUTA!

- **FXML Elements**:
  - `messageInput`: Campo de texto para ingresar mensajes.
  - `chatHistoryTextFlow`: `TextFlow` que muestra el historial de mensajes e imágenes.

- **Método `initialize`**:
  - Llama al método `receiveMessage()` en un hilo separado para recibir mensajes continuamente sin bloquear la interfaz de usuario.

- **Método `setUserName`**:
  - Establece el nombre de usuario.

```
    public void setUserName(String userName) {
        this.userName = userName;
    }
```

- **Método `receiveMessage`**:
  - Método en un hilo separado que recibe mensajes continuamente.
  - Verifica si el mensaje es de texto o una imagen.
  - Para mensajes de texto, actualiza el historial de chat con el nombre de usuario y el mensaje.
  - Para imágenes, guarda la imagen y actualiza el historial con el nombre de usuario y la imagen.

```
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
```

- **Métodos `appendMessage` y `appendImage`**:
  - Métodos para agregar mensajes de texto e imágenes al historial de chat desde cualquier hilo.
  - Utiliza `Platform.runLater()` para realizar operaciones en el hilo de la interfaz de usuario (JavaFX).

```
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
```
```
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
```

- **Método `sendMessageButton`**:
  - Maneja el evento de hacer clic en el botón de enviar mensaje.
  - Obtiene el texto del campo de entrada y llama a `sendMessage` para enviar el mensaje al servidor.

```
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
```

- **Método `sendMessage`**:
  - Construye el mensaje en el formato "TEXT|userName|message" y lo envía al servidor.

```
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
```

- **Métodos `sendImageButton`**:
  - Maneja el evento de hacer clic en el botón de enviar imagen.
  - Muestra un cuadro de diálogo para seleccionar un archivo de imagen y llama a `sendImage` para enviar la imagen al servidor.

```
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
```

- **Método `sendImage`**:
  - Envía el nombre del archivo al servidor y llama a `sendFile` para enviar el contenido de la imagen.

```
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
```

- **Método `sendFile`**:
  - Envia el archivo de imagen dividido en datagramas al servidor.
  - Utiliza un esquema de reconocimiento de paquetes para garantizar la integridad y el orden de los datagramas.

```
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
```

- **Método `readFileToByteArray`**:
  - Convierte el contenido de un archivo en un array de bytes.

```
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
```

- **Método `saveImage`**:
  - Recibe y guarda el contenido de la imagen enviada por el servidor.

```
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
```

- **Método `sendAck`**:
  - Envía un acuse de recibo al servidor indicando el número de secuencia del último paquete recibido correctamente.

```
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
```

## Notas Importantes:
  - La aplicación utiliza hilos separados para recibir mensajes y para operaciones en la interfaz de usuario.
  - El envío de imágenes se realiza dividiendo el archivo en datagramas, enviándolos al servidor y esperando acuses de recibo.
  - Existe posibles errores no manejadas que pueden causar problemas de uso. Primero hay que lanzar el servidor, y luego la aplicación. 










