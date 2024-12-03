package me.chessCapstone;

/**
 * Pawn
 * (Requirement 3.6.0)
 */
public class Pawn extends Piece {

    //Constructor for a Pawn object. Requires a String representation of color.
    public Pawn(String color) {
        //Passes the type alongside the color to the superclass, Piece.
        super("pawn", color);
    }

    //Like all other isValid___Move meethods, this ensures that the Piece follows movement rules given a target square.
    public boolean isValidPawnMove(int endCol, int endRow, String[][] boardCurrent) {
        //Gets the String representation of the given Piece.
        String pawn = boardCurrent[getCol()][getRow()];
        
        //Checks the color of the given Piece.
        boolean isWhite = pawn.contains("white");
        
        //White pawns move up (-1), black pawns move down (+1)
        int direction = isWhite ? -1 : 1;

        //Calculate the difference between the current and target squares.
        int rowDiff = endRow - getRow();
        int colDiff = Math.abs(endCol - getCol());

        //Checks to see if the target square can be reached by moving up (or down) one square.
        if (colDiff == 0 && rowDiff == direction && boardCurrent[endCol][endRow].equals("null")) {
            //The target square can be reached through a regular Pawn move.
            ChessGame.EnPassantPossible  = false;
            return true;
        }

        //Checks to see if the target square can be reached by moving two squares as a first move.
        if (colDiff == 0 && rowDiff == 2 * direction &&
                (isWhite ? getRow() == 6 : getRow() == 1) &&
                boardCurrent[endCol][endRow].equals("null") &&
                boardCurrent[endCol][endRow - direction].equals("null")) {
            //The target square can be reached through a two square Pawn move.
            ChessGame.EnPassantPossible  = false;
            return true;
        }

        //Checks to see if the target square involves the diagonal capture of an enemy Piece.
        if (colDiff == 1 && rowDiff == direction && !boardCurrent[endCol][endRow].equals("null") &&
                !boardCurrent[endCol][endRow].contains(isWhite ? "white" : "black")) {
            //The target square can be reached through an available diagonal capture.
            ChessGame.EnPassantPossible  = false;
            return true;
        }

        //Checks to see if the target square involves an en passant capture of an enemy Piece.
        //"En passant" is french for "in passing". If a pawn moves two squares and ends next to an enemy Pawn, it can be captured.
        //The capturing Pawn moves diagonally behind the opposing Pawn.
        if (Math.abs(endCol - getCol()) == 1 && boardCurrent[endCol][endRow].equals("null")) {
            if (endRow == getRow() + direction && ChessGame.lastMoveWasDoublePawnMove) {
                //Check if the last moved pawn is in the correct position
                if ((color.equals("white") && endRow == 2) || (color.equals("black") && endRow == 5)) {
                    //The pawn to be captured should be in the same column as the destination
                    if (boardCurrent[endCol][getRow()].equals(ChessGame.lastPawnMoved)) {
                        //En passant is available.
                        ChessGame.EnPassantPossible  = true;
                        return true;
                    }
                }
            }
        }

        //None of the given movement or capture rules apply.
        //Therefore, the target square cannot be reached.
        return false;

    }

    //This method checks to see what squares the Pawn currently threatens.
    public boolean isThreatenedSquare(int endCol, int endRow, String[][] boardCurrent) {

        //Checks the color of the given Piece.
        boolean isWhite = this.getColor().equals("white");
        
        //White pawns move up (-1), black pawns move down (+1)
        int direction = isWhite ? -1 : 1;

        //Calculate the difference between the current and target Square.
        int rowDiff = endRow - getRow();
        int colDiff = Math.abs(endCol - getCol());

        //If the target square is a diagonal move and not taken by a friendly Piece, it is a threatened square.
        return colDiff == 1 && rowDiff == direction &&
                !boardCurrent[endCol][endRow].contains(isWhite ? "white" : "black");
    }


}
