module me.chessCapstone {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.dynalink;

    requires java.desktop;




    opens me.chessCapstone to javafx.fxml;
    exports me.chessCapstone;
}