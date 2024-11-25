package me.chessCapstone;

public class Queen extends Piece {

    //Constructor for a Queen object. Requires a String representation of color.
    public Queen(String color) {
        //Passes the type alongside the color to the superclass, Piece.
        super("queen", color);
    }

    //This method ensures that the target square is valid given Queen movement rules.
    public boolean isValidQueenMove(int endCol, int endRow, String[][] boardCurrent) {

        //Calculates the difference between the current and target squares.
        int colDiff = Math.abs(endCol - getCol());
        int rowDiff = Math.abs(endRow - getRow());

        //Checks to see if the target square can be reached vertically.
        if (getCol() == endCol && getRow() != endRow) {
             //Target square can be reached. The path is then checked.
            return isPathClear(endCol, endRow, boardCurrent);
        }
            
        //Checks to see if the target square can be reached horizontally.
        else if (getRow() == endRow && getCol() != endCol) {
            //Target square can be reached. The path is then checked.
            return isPathClear(endCol, endRow, boardCurrent);
        }
            
        //Checks to see if the target square can be reached diagonally.
        else if (colDiff == rowDiff) {
            //Target square can be reached. The path is then checked.
            return isPathClear(endCol, endRow, boardCurrent);
        }

        //The target square cannot be reached by Queen movement rules.
        return false;
    }

}
