package me.chessCapstone;

public class Move {
    public final int startCol, startRow, endCol, endRow;
    public final String piece;

    public Move(int startCol, int startRow, int endCol, int endRow, String piece) {
        this.startCol = startCol;
        this.startRow = startRow;
        this.endCol = endCol;
        this.endRow = endRow;
        this.piece = piece;
    }
}
