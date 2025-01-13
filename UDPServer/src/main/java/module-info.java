module com.udpserver.udpserver {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens com.server to javafx.fxml;
    exports com.server;
}