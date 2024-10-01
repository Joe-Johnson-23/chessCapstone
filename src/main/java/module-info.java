module me.chessCapstone {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.dynalink;


    opens me.chessCapstone to javafx.fxml;
    exports me.chessCapstone;
}