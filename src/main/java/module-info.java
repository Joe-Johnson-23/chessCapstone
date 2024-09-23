module me.demo1 {
    requires javafx.controls;
    requires javafx.fxml;


    opens me.demo1 to javafx.fxml;
    exports me.demo1;
}