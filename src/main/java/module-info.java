module com.example.project {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.example.project to javafx.fxml;
    exports com.example.project;

    opens com.example.project.tcpip to javafx.fxml;
    exports com.example.project.tcpip;
}