package me.chessCapstone;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

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
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                StackPane tilePane = this.getStiles()[row][col];
                tilePane.getChildren().removeIf(node -> node instanceof Circle);
            }
        }
    }

    public String boardToFEN(HashMap<String, Piece> pieces, boolean isWhiteTurn, Tile enPassantTile, int numberOfHalfMoves, int numberOfMoves) {

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

        }

        if(isWhiteTurn) {
            sb.append(" w");
        } else {
            sb.append(" b");
        }

        sb.append(" ").append(enPassantConversion(enPassantTile));
        sb.append(" ").append(castlingRights(pieces));
        sb.append(" ").append(numberOfHalfMoves);
        sb.append(" ").append(numberOfMoves);
        return sb.toString();
    }

    public String enPassantConversion(Tile enPassantTile) {
        return "" + (char)('a' + enPassantTile.getCol()) + enPassantTile.getRow();
    }

    public StringBuilder castlingRights(HashMap<String, Piece> pieces) {
        StringBuilder sb = new StringBuilder();

        // Check castling rights for white
        appendCastlingRights(sb, pieces, "white", "K", "Q");

        // Check castling rights for black
        appendCastlingRights(sb, pieces, "black", "k", "q");

        return sb;
    }

    private void appendCastlingRights(StringBuilder sb, HashMap<String, Piece> pieces, String color, String kingside, String queenside) {
        String kingKey = "king1" + color;
        String rook1Key = "rook1" + color;
        String rook2Key = "rook2" + color;

        if (!pieces.get(kingKey).hasMoved()) {
            sb.append(!pieces.get(rook2Key).hasMoved() ? kingside : "-");
            sb.append(!pieces.get(rook1Key).hasMoved() ? queenside : "-");
        } else {
            sb.append("--");
        }
    }
}
