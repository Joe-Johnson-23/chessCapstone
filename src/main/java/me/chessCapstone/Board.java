package me.chessCapstone;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.util.HashMap;

public class Board {

    private StackPane[][] stiles = new StackPane[BOARD_SIZE][BOARD_SIZE];
    private String[][] board = new String[BOARD_SIZE][BOARD_SIZE];
    private static final int BOARD_SIZE = 8;

    public Board(GridPane gridPane) {

        //This replaces the setupBoard method.
        for (int row = 0; row < BOARD_SIZE; row++) {

            for (int col = 0; col < BOARD_SIZE; col++) {

                StackPane stile = new StackPane();
                stile.setPrefWidth(200);
                stile.setPrefHeight(200);
                stile.setStyle((row + col) % 2 == 0 ? "-fx-background-color:WHITE" : "-fx-background-color:GRAY");
                stiles[row][col] = stile;
                gridPane.add(stile, col, row);

            }
        }

    }

    public StackPane[][] getStiles() {
        return stiles;
    }

    public String[][] getBoard() {
        return board;
    }

    public String get(int col, int row) {
        return board[col][row];
    }

    public void set(int col, int row, String value) {
        board[col][row] = value;
    }

    public void printBoardState() {
        System.out.println("NEW BOARD");
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                System.out.println("col " + col + " row " + row + " = " + board[col][row]);
            }
        }

    }

    public void resetTileColor() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if ((row + col) % 2 == 0) {
                    stiles[row][col].setStyle("-fx-background-color: WHITE;");
                } else {
                    stiles[row][col].setStyle("-fx-background-color: GRAY;");
                }
            }
        }
    }

    public String getFENNotation(HashMap<String, Piece> pieces, boolean isWhiteTurn) {

        StringBuilder sb = new StringBuilder();
        int emptyTileCounter = 0;

        for(int row = 0; row < BOARD_SIZE; row++) {

            for(int col = 0; col < BOARD_SIZE; col++) {

                Piece piece = pieces.get(this.get(col, row));
                String next = null;

                if(piece != null) {

                    if(emptyTileCounter != 0) {
                        sb.append(emptyTileCounter);
                        emptyTileCounter = 0;
                    }

                    switch (piece.getType()) {
                        case "king" -> next = "k";
                        case "queen" -> next = "q";
                        case "knight" -> next = "n";
                        case "bishop" -> next = "b";
                        case "rook" -> next = "r";
                        case "pawn" -> next = "p";
                    }

                    if(piece.getColor().equals("white")) {
                        next = next.toUpperCase();
                    }

                    sb.append(next);

                } else {
                    emptyTileCounter++;
                }

            }

            if(emptyTileCounter != 0) {
                sb.append(emptyTileCounter);
                emptyTileCounter = 0;
            }

            sb.append("/");

        }

        if(isWhiteTurn) {
            sb.append("w");
        } else {
            sb.append("b");
        }

        sb.append("/");

        if(!pieces.get("king1white").hasMoved()) {

            if(!pieces.get("rook2white").hasMoved()) {
                sb.append("K");
            } else {
                sb.append("-");
            }

            if(!pieces.get("rook1white").hasMoved()) {
                sb.append("Q");
            } else {
                sb.append("-");
            }

        } else {
            sb.append("-");
        }

        if(!pieces.get("king1black").hasMoved()) {

            if(!pieces.get("rook1black").hasMoved()) {
                sb.append("k");
            } else {
                sb.append("-");
            }

            if(!pieces.get("rook2black").hasMoved()) {
                sb.append("q");
            } else {
                sb.append("-");
            }
        } else {
            sb.append("-");
        }


        return sb.toString();
    }

}
