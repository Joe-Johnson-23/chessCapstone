package me.chessCapstone;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {

        // See https://stackoverflow.com/a/52654791/3956070 for explanation
//        ChessGame.main(args);
        Application.launch(ChessGame.class, args);
    }
}

