package me.chessCapstone;

/**
 * Bishop
 * (Requirement 3.2.0)
 */
public class Bishop extends Piece {

    //Constructor for a Bishop object. Requires a String representation of color.
    public Bishop(String color) {
        //Passes the type alongside the color to the superclass, Piece.
        super("bishop", color);
    }

    //Given a target column and row, this method ensures that the target is valid given Bishop movement rules.
    public boolean isValidBishopMove(int endCol, int endRow, String[][] boardCurrent) {

        //Finds the difference between the target square and current square.
        int colDiff = Math.abs(endCol - getCol());
        int rowDiff = Math.abs(endRow - getRow());

        //Since a Bishop can only move diagonally, the difference in the column and row must equal each other, else it is not a diagonal move, violating Bishop movement rules.
        if (colDiff == rowDiff) {
            //If Bishop movement rules are followed, the path is then checked.
            return isPathClear(endCol, endRow, boardCurrent);
        }
        //If the Bishop movement rules are broken, it returns false.
        return false;
    }

}


