package me.chessCapstone;

public class Bishop extends Piece {




    public Bishop(String color) {
        super("bishop", color);
    }



public boolean isValidBishopMove(int startCol, int startRow, int endCol, int endRow, String[][] boardCurrent) {
    int colDiff = Math.abs(endCol - startCol);
    int rowDiff = Math.abs(endRow - startRow);

    if (colDiff == rowDiff) {
        return isPathClear(startCol, startRow, endCol, endRow, boardCurrent);
    }

    return false;
}

}


