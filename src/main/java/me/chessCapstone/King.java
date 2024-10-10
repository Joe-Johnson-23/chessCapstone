package me.chessCapstone;

import java.util.ArrayList;

public class King extends Piece {

    public King(String color) {
        super("king", color);
    }

    public boolean isValidKingMove(int endCol, int endRow, String[][] boardCurrent, ArrayList<Tile> threatenedSquares) {
        int colDiff = Math.abs(endCol - getCol());
        int rowDiff = Math.abs(endRow - getRow());

        if(threatenedSquares != null) {
            for(Tile tile : threatenedSquares) {
                if(endCol == tile.getCol() && endRow == tile.getRow()) {
                    return false;
                }
            }
        }
        // Regular king move
        if (colDiff <= 1 && rowDiff <= 1) {

            String destinationPiece = boardCurrent[endCol][endRow];
            String currentPiece = boardCurrent[getCol()][getRow()];
            return destinationPiece.equals("null") ||
                    (!destinationPiece.contains(currentPiece.contains("white") ? "white" : "black"));
        }
        return false;
    }

    public boolean isInCheck(ArrayList<Tile> threatenedSquares) {
        for(Tile tile : threatenedSquares) {
            if(this.col == tile.getCol() && this.row == tile.getRow()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValidMove(int endCol, int endRow, String[][] board, ArrayList<Tile> threatenedSquares) {
        // Check for regular king move
        if (Math.abs(endCol - getCol()) <= 1 && Math.abs(endRow - getRow()) <= 1) {
            return super.isValidMove(endCol, endRow, board, threatenedSquares);
        }

        // Check for castling
        return !hasMoved() && getRow() == endRow && Math.abs(endCol - getCol()) == 2; // The actual validation will be done in the ChessGame class
    }



}