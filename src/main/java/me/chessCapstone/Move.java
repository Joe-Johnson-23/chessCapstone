package me.chessCapstone;

//Represents a chess move with starting position, ending position,
//and piece information
//helps with move validation and applying moves for custom AI
public class Move {
    //Final fields to ensure immutability
    public final int startCol, startRow, endCol, endRow;
    public final String piece;

    public Move(int startCol, int startRow, int endCol, int endRow, String piece) {
        //Initialize fields
        this.startCol = startCol;
        this.startRow = startRow;
        this.endCol = endCol;
        this.endRow = endRow;
        this.piece = piece;
    }
}
