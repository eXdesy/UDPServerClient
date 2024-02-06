module com.updserver.updclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens com.updclient to javafx.fxml;
    exports com.updclient;
}