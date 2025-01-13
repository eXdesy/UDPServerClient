module com.updserver.updclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens com.client to javafx.fxml;
    exports com.client;
}