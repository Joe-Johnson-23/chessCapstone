package me.chessCapstone;

public class Knight extends Piece {

    //Constructor for a Knight object. Requires a String representation of color.
    public Knight(String color) {
        //Passes the type alongside the color to the superclass, Piece.
        super("knight", color);
    }

    //Given a target column and row, this method ensures that the target is valid given Knight movement rules.
    public boolean isValidKnightMove(int endCol, int endRow, String[][] boardCurrent) {
        int colDiff = Math.abs(endCol - getCol());
        int rowDiff = Math.abs(endRow - getRow());

        //Verifies that Knight movement rules are not violated. Knights move in an L shape.
        boolean isValidMove = (colDiff == 2 && rowDiff == 1) || (colDiff == 1 && rowDiff == 2);

        //If Knight movement rules are not violated, the Piece is moved to the target square.
        if (isValidMove) {
            String destinationPiece = boardCurrent[endCol][endRow];
            String currentPiece = boardCurrent[getCol()][getRow()];

            //The target square must be either empty or occupied by an opponent's piece
            return destinationPiece.equals("null") ||
                    (!destinationPiece.contains(currentPiece.contains("white") ? "white" : "black"));
        }

        //If the Knight's movement rules are violated, false is returned.
        return false;
    }

}
