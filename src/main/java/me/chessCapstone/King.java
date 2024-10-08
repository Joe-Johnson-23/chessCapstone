package me.chessCapstone;

import java.util.ArrayList;

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

    public boolean isValidKingMove(int startCol, int startRow, int endCol, int endRow, String[][] boardCurrent, ArrayList<Tile> threatenedSquares) {
        int colDiff = Math.abs(endCol - startCol);
        int rowDiff = Math.abs(endRow - startRow);

        // Regular king move
        if (colDiff <= 1 && rowDiff <= 1) {

            //Differentiate between black and white
            //Maybe add threatenedBy (color) attribute to Tile?
            for(Tile tile : threatenedSquares) {
                if(endCol == tile.getCol() && endRow == tile.getRow()) {
                    return false;
                }
            }

            String destinationPiece = boardCurrent[endCol][endRow];
            String currentPiece = boardCurrent[startCol][startRow];
            return destinationPiece.equals("null") ||
                    (!destinationPiece.contains(currentPiece.contains("white") ? "white" : "black"));
        }
        return false;
    }

}