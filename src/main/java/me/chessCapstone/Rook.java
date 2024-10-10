package me.chessCapstone;

public class Rook extends Piece {

    public Rook(String color) {
        super("rook", color);
    }


    public boolean isValidRookMove(int endCol, int endRow, String[][] boardCurrent) {

        // 수직 이동
        if (getCol() == endCol && getRow() != endRow) {
            return isPathClear(endCol, endRow, boardCurrent);
        }
        // 수평 이동
        else if (getRow() == endRow && getCol() != endCol) {
            return isPathClear(endCol, endRow, boardCurrent);
        }

        return false;
    }


}
