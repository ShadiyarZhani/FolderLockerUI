module com.example.folderlockerui {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.commons.codec;
    requires zip4j;


    opens com.example.folderlockerui to javafx.fxml;
    exports com.example.folderlockerui;
}