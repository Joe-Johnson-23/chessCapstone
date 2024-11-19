package me.chessCapstone;

public class Queen extends Piece {

    //Constructor for a Queen object. Requires a String representation of color.
    public Queen(String color) {
        //Passes the type alongside the color to the superclass, Piece.
        super("queen", color);
    }

    //Given a target column and row, this method ensures that the target is valid given Queen movement rules.
    public boolean isValidQueenMove(int endCol, int endRow, String[][] boardCurrent) {

        int colDiff = Math.abs(endCol - getCol());
        int rowDiff = Math.abs(endRow - getRow());

        // 수직 이동
        if (getCol() == endCol && getRow() != endRow) {
            return isPathClear(endCol, endRow, boardCurrent);
        }
        // 수평 이동
        else if (getRow() == endRow && getCol() != endCol) {
            return isPathClear(endCol, endRow, boardCurrent);
        }
        // 대각선 이동
        else if (colDiff == rowDiff) {
            return isPathClear(endCol, endRow, boardCurrent);
        }

        return false;
    }

}
