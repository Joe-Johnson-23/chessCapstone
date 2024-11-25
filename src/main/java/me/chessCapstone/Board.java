package me.chessCapstone;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.util.HashMap;

public class Board {

    //Tracks highlighted squares on the board.
    private StackPane[][] stiles = new StackPane[BOARD_SIZE][BOARD_SIZE];
    //Tracks Pieces on the board.
    private String[][] board = new String[BOARD_SIZE][BOARD_SIZE];
    //Dimensions of the board.
    private static final int BOARD_SIZE = 8;

    public Board(GridPane gridPane) {

        //Iterate through the board
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {

                //Create a StackPane for each square on the Chess board.
                StackPane stile = new StackPane();

                //Set the Width and Height of the StackPane.
                stile.setPrefWidth(200);
                stile.setPrefHeight(200);
                
                //Set the color based on Chess convention, switching between white and gray.
                stile.setStyle((row + col) % 2 == 0 ? "-fx-background-color:WHITE" : "-fx-background-color:GRAY");
                
                //Add the StackPane to the StackPane array.
                stiles[row][col] = stile;
                
                //Add the StackPane to the GridPane.
                gridPane.add(stile, col, row);
            }
        }
    }

    //Returns the array of StackPanes
    public StackPane[][] getStiles() {
        return stiles;
    }

    //Returns the array of String keys for the Pieces HashMap.
    public String[][] getBoard() {
        return board;
    }
    
    //Returns the String key representation of a Piece from the board array.
    public String get(int col, int row) {
        return board[col][row];
    }

    //Sets the value of a String at a given index on the board array.
    public void set(int col, int row, String value) {
        board[col][row] = value;
    }

    //Prints the contents of the board.
    public void printBoardState() {
        System.out.println("NEW BOARD");

        //Iterate through the board.
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                //Print the column and row alongside the Piece on that index, if any.
                System.out.println("col " + col + " row " + row + " = " + board[col][row]);
            }
        }
    }

    //Resets any highlighted circles currently on the board.
    public void resetTileColor() {

        //Iterate through the board.
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                //Pulls the StackPane at the given index.
                StackPane tilePane = this.getStiles()[row][col];
                //If any children of the StackPane are an instance of Circle, it is removed.
                tilePane.getChildren().removeIf(node -> node instanceof Circle);
            }
        }
    }

    //Returns a String representation of the Forsyth-Edwards Notation.
    //FEN Notation is passed to Stockfish in order for it to understand the happenings on the board.
    public String boardToFEN(HashMap<String, Piece> pieces, boolean isWhiteTurn, Tile enPassantTile, int numberOfHalfMoves, int numberOfMoves) {

        //Initialize variables to track progress.
        StringBuilder sb = new StringBuilder();
        int emptyTileCounter = 0;

        //Iterate through the chess board.
        for(int row = 0; row < BOARD_SIZE; row++) {
            for(int col = 0; col < BOARD_SIZE; col++) {

                //Get the Piece (if any) from the current index.
                Piece piece = pieces.get(this.get(col, row));
                String next = null;

                //If a Piece exists on the given index.
                if(piece != null) {

                    //If there were any empty squares in the row prior to the Piece being found, the number is appended.
                    if(emptyTileCounter != 0) {
                        
                        //Number of empty squares are appended to the notation.
                        sb.append(emptyTileCounter);
                        
                        //Counter resets.
                        emptyTileCounter = 0;
                    }
                    
                    //Letter is selected based on the type of Piece.
                    switch (piece.getType()) {
                        case "king" -> next = "k";
                        case "queen" -> next = "q";
                        case "knight" -> next = "n";
                        case "bishop" -> next = "b";
                        case "rook" -> next = "r";
                        case "pawn" -> next = "p";
                    }

                    //Uppercase = Piece is White.
                    //Lowercase = Piece is Black.
                    if(piece.getColor().equals("white")) {
                        next = next.toUpperCase();
                    }

                    //Append the Piece letter.
                    sb.append(next);
                    
                } else {
                    //No Piece was found on the current index. As such, the counter goes up by one.
                    emptyTileCounter++;
                }
            }

            //If there are empty indices at the end of the row, it is appended and the counter reset.
            if(emptyTileCounter != 0) {
                sb.append(emptyTileCounter);
                emptyTileCounter = 0;
            }

            //In FEN Notation "/" marks a new row.
            sb.append("/");

        }

        //A "/" is not needed at the end since there is no row after.
        sb.deleteCharAt(sb.length() - 1);

        //w = White's turn.
        //b = Black's turn.
        if(isWhiteTurn) {
            sb.append(" w");
        } else {
            sb.append(" b");
        }

        //TODO Finish source code documentation.
        sb.append(" ").append(enPassantConversion(enPassantTile));
        sb.append(" ").append(castlingRights(pieces));
        sb.append(" ").append(numberOfHalfMoves);
        sb.append(" ").append(numberOfMoves);
        return sb.toString();
    }

    public String enPassantConversion(Tile enPassantTile) {
        if(enPassantTile.getCol() == -1 && enPassantTile.getRow() == -1) {
            return "-";
        } else {
            return "" + (char)('a' + enPassantTile.getCol()) + enPassantTile.getRow();
        }
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
