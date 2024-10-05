package me.chessCapstone;

public class Queen extends Piece {

    public Queen(String color) {
        super("queen", color);
    }


    public boolean isValidQueenMove(int startCol, int startRow, int endCol, int endRow, String[][] boardCurrent) {
        int colDiff = Math.abs(endCol - startCol);
        int rowDiff = Math.abs(endRow - startRow);

        // 수직 이동
        if (startCol == endCol && startRow != endRow) {
            return isPathClear(startCol, startRow, endCol, endRow, boardCurrent);
        }
        // 수평 이동
        else if (startRow == endRow && startCol != endCol) {
            return isPathClear(startCol, startRow, endCol, endRow, boardCurrent);
        }
        // 대각선 이동
        else if (colDiff == rowDiff) {
            return isPathClear(startCol, startRow, endCol, endRow, boardCurrent);
        }

        return false;
    }

}
