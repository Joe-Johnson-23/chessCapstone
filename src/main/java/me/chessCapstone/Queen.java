package me.chessCapstone;

public class Queen extends Piece {

    public Queen(String color) {
        super("queen", color);
    }


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
