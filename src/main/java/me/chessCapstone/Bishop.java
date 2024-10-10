package me.chessCapstone;

public class Bishop extends Piece {

    public Bishop(String color) {
        super("bishop", color);
    }

public boolean isValidBishopMove(int endCol, int endRow, String[][] boardCurrent) {
    int colDiff = Math.abs(endCol - getCol());
    int rowDiff = Math.abs(endRow - getRow());

    if (colDiff == rowDiff) {
        return isPathClear(endCol, endRow, boardCurrent);
    }

    return false;
}

}


