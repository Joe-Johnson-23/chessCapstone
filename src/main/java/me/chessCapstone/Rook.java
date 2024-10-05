package me.chessCapstone;

public class Rook extends Piece {

    public Rook(String color) {
        super("rook", color);
    }

    public boolean isValidRookMove(int startCol, int startRow, int endCol, int endRow, String[][] boardCurrent) {

        // 수직 이동
        if (startCol == endCol && startRow != endRow) {
            return isPathClear(startCol, startRow, endCol, endRow, boardCurrent);
        }
        // 수평 이동
        else if (startRow == endRow && startCol != endCol) {
            return isPathClear(startCol, startRow, endCol, endRow, boardCurrent);
        }

        return false;
    }

}
