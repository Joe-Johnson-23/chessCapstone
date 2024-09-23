module me.chessCapstone {
    requires javafx.controls;
    requires javafx.fxml;


    opens me.chessCapstone to javafx.fxml;
    exports me.chessCapstone;
}