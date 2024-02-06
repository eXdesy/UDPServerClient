module com.udpserver.udpserver {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens com.udpserver to javafx.fxml;
    exports com.udpserver;
}