package me.chessCapstone;

public class Rook extends Piece {

    //Constructor for a Rook object. Requires a String representation of color.
    public Rook(String color) {
        //Passes the type alongside the color to the superclass, Piece.
        super("rook", color);
    }


    //Checks a given square's validity as a target square give Rook movement rules.
    public boolean isValidRookMove(int endCol, int endRow, String[][] boardCurrent) {

        //Checks if the target square is a vertical move away.
        if (getCol() == endCol && getRow() != endRow) {
            //Target square shares the column with the beginning square. The path is then checked.
            return isPathClear(endCol, endRow, boardCurrent);
        }
            
        //Checks if the target square is a horizontal move away.
        else if (getRow() == endRow && getCol() != endCol) {
            //Target square shares the row with the beginning square. The path is then checked.
            return isPathClear(endCol, endRow, boardCurrent);
        }

        //The target square cannot be reached through a vertical or horizontal move.
        //A rook can only move vertically or horizontally, therefore, the target square violates Rook movement rules.
        return false;
    }

    //This method handles castling. Only the column of the Rook's current square changes.
    public void handleCastlingMove(int newCol) {
        //The given column is used to replace the past column.
        this.setCol(newCol);
        //Castling necessitates that the given Rook has not moved.
        //Thus, their setMoved condition will always be false and must be changed.
        this.setMoved(true);
    }

}
