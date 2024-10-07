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
            return true;
        }

        //First move: option to move 2 squares forward
        if (colDiff == 0 && rowDiff == 2 * direction &&
                (isWhite ? startRow == 6 : startRow == 1) &&
                boardCurrent[endCol][endRow].equals("null") &&
                boardCurrent[endCol][endRow - direction].equals("null")) {
            return true;
        }

        //Capture move: 1 square diagonally
        if (colDiff == 1 && rowDiff == direction && !boardCurrent[endCol][endRow].equals("null") &&
                !boardCurrent[endCol][endRow].contains(isWhite ? "white" : "black")) {
            return true;
        }

//En passant capture (simplified, doesn't check if the last move was a double pawn push)
        if (colDiff == 1 && rowDiff == direction && boardCurrent[endCol][endRow].equals("null") &&
                !boardCurrent[endCol][startRow].equals("null") &&
                boardCurrent[endCol][startRow].contains(isWhite ? "black" : "white") &&
                boardCurrent[endCol][startRow].contains("pawn"))

        {
            ChessGame.globalEnPassant = true;
            return true;
        }




        return false;
    }
}