# UDP Server & Client - Aplicación
Esta aplicación de chat UDP Cliente está escrita en Java y utiliza JavaFX para la interfaz gráfica. La comunicación se realiza a través del protocolo UDP para el intercambio de mensajes y archivos de imagen.

[VER PARTE TECNICA DEL CLIENTE](https://github.com/eXdesy/UDPServerClient/blob/master/UDPClient/readme.md)

[VER PARTE TECNICA DEL SERVIDOR](https://github.com/eXdesy/UDPServerClient/blob/master/UDPServer/readme.md)


## Instrucciones de Uso:

### Configuración del Servidor:

<img src="https://github.com/eXdesy/UDPServerClient/blob/master/img/Server_Interface.png" alt="Server_Interface" width="400"/>

- **Interfaz de Usuario:**
	- El servidor escucha en el puerto `5010`.
	- Las imágenes recibidas se guardan en la ruta `C:\Users\Admin\Downloads\ServerImages`. (Cambiar si es necesario)
	- La interfaz incluye una consola (consoleTextArea) que muestra mensajes relevantes del servidor.

- **Conexión de Clientes:**
	- Los clientes deben conectarse al servidor utilizando mensajes de verificación de nombre de usuario `CHECK_USERNAME`.

- **Mensajes de Texto:**
	- Los mensajes de texto enviados por los clientes se reenvían a todos los clientes conectados.

- **Envío y Recepción de Imágenes:**
	- Los clientes pueden enviar imágenes al servidor mediante mensajes específicos.
	- El servidor reenvía las imágenes a todos los clientes conectados.

- **Ejecución:**
	- Compile y ejecute `UDPServer.java`.
	- Asegúrese de tener la configuración del entorno y las dependencias adecuadas.

### Configuración del Cliente:

<img src="https://github.com/eXdesy/UDPServerClient/blob/master/img/Client_Login.png" alt="Client_Login" width="400"/>

- **Interfaz de Usuario:**
	- El cliente escucha en el puerto `6010`.
	- Las imágenes recibidas se guardan en la ruta `C:\Users\Admin\Downloads\ClientImages`. (Cambiar si es necesario)

		<img src="https://github.com/eXdesy/UDPServerClient/blob/master/img/Client_Chat.png" alt="Client_Chat" width="400"/>

- **Enviar Mensajes de Texto:**
	- En la ventana principal, utiliza el cuadro de texto inferior para escribir tu mensaje.
	- Haz clic en el botón "Enviar" para enviar el mensaje al servidor y a otros usuarios conectados.
	- Tus mensajes se mostrarán en verde, mientras que los mensajes de otros usuarios se mostrarán en azul.

		<img src="https://github.com/eXdesy/UDPServerClient/blob/master/img/Client_Message.png" alt="Client_Message" width="400"/>

- **Enviar Imágenes:**

	- Haz clic en el botón "Enviar Imagen" para seleccionar un archivo de imagen desde tu dispositivo.
	-  Selecciona la imagen deseada en el cuadro de diálogo.
	- La imagen se mostrará en la ventana de chat y se enviará al servidor y otros usuarios.
	-  Tus imágenes se mostrarán en verde, mientras que las imágenes de otros usuarios se mostrarán en azul.

		<img src="https://github.com/eXdesy/UDPServerClient/blob/master/img/Client_Image.png" alt="Client_Image" width="400"/>

- **Ejecución:**
	- Compile y ejecute `HelloApplication`.
	- Asegúrese de tener la configuración del entorno y las dependencias adecuadas.

## Notas Adicionales:
- La dirección IP del servidor está predefinida en el código `IP = "192.168.0.18"`. Asegúrate de que sea la correcta para tu entorno.
- El código también incluye la lógica para la recepción y envío de archivos de imagen a través del protocolo UDP.
- Si encuentras algún problema, revisa la consola de tu IDE para mensajes de error.

## ¡Disfruta del chat con UDP!







