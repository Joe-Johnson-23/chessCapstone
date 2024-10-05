package me.chessCapstone;

public class Knight extends Piece {

    public Knight(String color) {
        super("knight", color);
    }

    public boolean isValidKnightMove(int startCol, int startRow, int endCol, int endRow, String[][] boardCurrent) {
        int colDiff = Math.abs(endCol - startCol);
        int rowDiff = Math.abs(endRow - startRow);

        // Knight moves in an L-shape: 2 squares in one direction and 1 square perpendicular to that
        boolean isValidMove = (colDiff == 2 && rowDiff == 1) || (colDiff == 1 && rowDiff == 2);

        if (isValidMove) {
            String destinationPiece = boardCurrent[endCol][endRow];
            String currentPiece = boardCurrent[startCol][startRow];

            // The destination must be either empty or occupied by an opponent's piece
            return destinationPiece.equals("null") ||
                    (!destinationPiece.contains(currentPiece.contains("white") ? "white" : "black"));
        }

        return false;
    }

}
