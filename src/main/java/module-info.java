module com.example.proyectocorte2 {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;




    opens com.example.proyectocorte2 to javafx.fxml;
    opens com.example.proyectocorte2.model to javafx.base;
    opens com.example.proyectocorte2.controller to javafx.fxml;
    exports com.example.proyectocorte2.controller to javafx.fxml;
    exports com.example.proyectocorte2;

}