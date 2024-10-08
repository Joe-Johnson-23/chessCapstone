package me.chessCapstone;

public class Pawn extends Piece {

    public Pawn(String color) {
        super("pawn", color);

    }

    public boolean isValidPawnMove(int startCol, int startRow, int endCol, int endRow, String[][] boardCurrent) {
        String pawn = boardCurrent[startCol][startRow];

        boolean isWhite = pawn.contains("white");
        int direction = isWhite ? -1 : 1; // White pawns move up (-1), black pawns move down (+1)
        int rowDiff = endRow - startRow;
        int colDiff = Math.abs(endCol - startCol);

        //Regular move: 1 square forward
        if (colDiff == 0 && rowDiff == direction && boardCurrent[endCol][endRow].equals("null")) {
            ChessGame.EnPassantPossible  = false;
            return true;
        }

        //First move: option to move 2 squares forward
        if (colDiff == 0 && rowDiff == 2 * direction &&
                (isWhite ? startRow == 6 : startRow == 1) &&
                boardCurrent[endCol][endRow].equals("null") &&
                boardCurrent[endCol][endRow - direction].equals("null")) {
            ChessGame.EnPassantPossible  = false;
            return true;
        }

        //Capture move: 1 square diagonally
        if (colDiff == 1 && rowDiff == direction && !boardCurrent[endCol][endRow].equals("null") &&
                !boardCurrent[endCol][endRow].contains(isWhite ? "white" : "black")) {
            ChessGame.EnPassantPossible  = false;
            return true;
        }

//En passant capture
        if (Math.abs(endCol - startCol) == 1 && boardCurrent[endCol][endRow].equals("null")) {
            if (endRow == startRow + direction && ChessGame.lastMoveWasDoublePawnMove) {
                // Check if the last moved pawn is in the correct position
                if ((color.equals("white") && endRow == 2) || (color.equals("black") && endRow == 5)) {
                    // The pawn to be captured should be in the same column as the destination
                    if (boardCurrent[endCol][startRow].equals(ChessGame.lastPawnMoved)) {
                        ChessGame.EnPassantPossible  = true;
                        return true; // Valid en passant
                    }
                }
            }
        }




        return false;
    }


}