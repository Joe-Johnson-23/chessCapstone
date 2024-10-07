package me.chessCapstone;

public class King extends Piece {

    public King(String color) {
        super("king", color);
    }


    public boolean isValidKingMove(int startCol, int startRow, int endCol, int endRow, String[][] boardCurrent) {
        int colDiff = Math.abs(endCol - startCol);
        int rowDiff = Math.abs(endRow - startRow);

        // Regular king move
        if (colDiff <= 1 && rowDiff <= 1) {
            String destinationPiece = boardCurrent[endCol][endRow];
            String currentPiece = boardCurrent[startCol][startRow];

            return destinationPiece.equals("null") ||
                    (!destinationPiece.contains(currentPiece.contains("white") ? "white" : "black"));
        }
        return false;
    }

    @Override
    public boolean isValidMove(int startCol, int startRow, int endCol, int endRow, String[][] board) {
        // Check for regular king move
        if (Math.abs(endCol - startCol) <= 1 && Math.abs(endRow - startRow) <= 1) {
            return super.isValidMove(startCol, startRow, endCol, endRow, board);
        }

        // Check for castling
        if (!hasMoved() && startRow == endRow && Math.abs(endCol - startCol) == 2) {
            return true; // The actual validation will be done in the ChessGame class
        }

        return false;
    }



}
