package me.chessCapstone;

public class Pawn extends Piece {

    public Pawn(String color) {
        super("pawn", color);
    }

    public boolean isValidPawnMove(int endCol, int endRow, String[][] boardCurrent) {
        String pawn = boardCurrent[getCol()][getRow()];
        boolean isWhite = pawn.contains("white");
        int direction = isWhite ? -1 : 1; // White pawns move up (-1), black pawns move down (+1)
        int rowDiff = endRow - getRow();
        int colDiff = Math.abs(endCol - getCol());

        //Regular move: 1 square forward
        if (colDiff == 0 && rowDiff == direction && boardCurrent[endCol][endRow].equals("null")) {
            return true;
        }

        //First move: option to move 2 squares forward
        if (colDiff == 0 && rowDiff == 2 * direction &&
                (isWhite ? getRow() == 6 : getRow() == 1) &&
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
                !boardCurrent[endCol][getRow()].equals("null") &&
                boardCurrent[endCol][getRow()].contains(isWhite ? "black" : "white") &&
                boardCurrent[endCol][getRow()].contains("pawn")) {
            return true;
        }

        return false;

    }

    public boolean isThreatenedSquare(int endCol, int endRow, String[][] boardCurrent) {
        boolean isWhite = this.getColor().equals("white");
        int direction = isWhite ? -1 : 1; // White pawns move up (-1), black pawns move down (+1)
        int rowDiff = endRow - getRow();
        int colDiff = Math.abs(endCol - getCol());

        return colDiff == 1 && rowDiff == direction &&
                !boardCurrent[endCol][endRow].contains(isWhite ? "white" : "black");
    }
}