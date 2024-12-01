package me.chessCapstone;

import java.util.ArrayList;

/**
 * King
 * (Requirement 3.4.0)
 */
public class King extends Piece {

    //Constructor for a King object. Requires a String representation of color.
    public King(String color) {
        //Passes the type alongside the color to the superclass, Piece.
        super("king", color);
    }

    //Like most IsValid___Move methods, this one ensures that movement rules are followed.
    //Additionally, this method ensures the King is not walking into a Check of any kind.
    public boolean isValidKingMove(int endCol, int endRow, String[][] boardCurrent, ArrayList<Tile> threatenedSquares) {

        //Calculates the difference between the beginning and target squares
        int colDiff = Math.abs(endCol - getCol());
        int rowDiff = Math.abs(endRow - getRow());

        //If the enemy controls any squares, the ArrayList is iterated to ensure the target square is not threatened.
        if(threatenedSquares != null) {
            //Iterate the enemy's threatened squares.
            for(Tile tile : threatenedSquares) {
                if(endCol == tile.getCol() && endRow == tile.getRow()) {
                    //The target square is threatened by the opponent. Therefore, this is not a valid square to move to.
                    return false;
                }
            }
            //The target square was not found in the given ArrayList. Therefore, the method continues.
        }
        
        //Check to ensure King movement rules (One square at a time in any direction) are followed.
        if (colDiff <= 1 && rowDiff <= 1) {
            //Pulls the Piece (if any) from the board at the target square.
            String destinationPiece = boardCurrent[endCol][endRow];
            //Pulls the current Piece from the board.
            String currentPiece = boardCurrent[getCol()][getRow()];

            //If the target square is empty or is taken by an enemy Piece, it is considered a valid square.
            return destinationPiece.equals("null") ||
                    (!destinationPiece.contains(currentPiece.contains("white") ? "white" : "black"));
        }
        
        //If the movement rules are broken, return false.
        return false;
    }

    /**
     * CheckMate
     * (Requirement 5.0.0)
     */
    //This method checks if the King is in Check, given the enemy's threatened squares.
    public boolean isInCheck(ArrayList<Tile> threatenedSquares) {
        //Iterate the enemy's threatened squares.
        for(Tile tile : threatenedSquares) {
            if(this.col == tile.getCol() && this.row == tile.getRow()) {
                //The King's current square is threatened, therefore it is in Check.
                return true;
            }
        }
        //The King's current square is not threatened, therefore it is not in Check.
        return false;
    }

    @Override
    //This method checks if the target square is a valid move.
    //An override was needed in order to account for castling.
    public boolean isValidMove(int endCol, int endRow, String[][] board, ArrayList<Tile> threatenedSquares) {

        if (Math.abs(endCol - getCol()) <= 1 && Math.abs(endRow - getRow()) <= 1) {
            //The target square can be reached through a regular King move.
            return super.isValidMove(endCol, endRow, board, threatenedSquares);
        }

        //Check for castling
        return !hasMoved() && getRow() == endRow && Math.abs(endCol - getCol()) == 2 && isCastlingValid(endCol, board, threatenedSquares);
    }

    //This method checks to see if Castling is an option.
    public boolean isCastlingValid(int endCol, String[][] boardCurrent, ArrayList<Tile> threatenedSquares) {

        //Ensure the King is not currently in Check.
        if (isInCheck(threatenedSquares)) {
            //Castling while in Check is an illegal move, therefore, the Check must be dealt with before Castling.
            return false;
        }

        //Determine which side is being checked: King side or Queen side.
        boolean isKingSide = endCol > this.getCol();
        //Numeric representation of the side: 1 is King side, 2 is Queen side.
        int direction = isKingSide ? 1 : -1;
        //The column the Castling Rook is on. Determined by isKingSide.
        int rookCol = isKingSide ? 7 : 0;

        //Check if the King has moved and if there are any obstacles between the King and Rook.
        if (this.hasMoved() || !isPathClearForCastling(rookCol, boardCurrent)) {
            //If either condition is true, Castling is not available.
            return false;
        }

        //Iterate to ensure that none of the squares needed to Castle are threatened.
        for (int col = this.getCol(); col != endCol; col += direction) {
            if (isSquareThreatened(col, this.getRow(), threatenedSquares)) {
                //If the current square is threatened, Castling cannot be performed.
                return false;
            }
        }
        //All conditions are met for Castling.
        return true;
    }

    //This method checks to ensure that all square in between the King and Rook needed are clear of obstacles.
    private boolean isPathClearForCastling(int rookCol, String[][] boardCurrent) {
        //Determines what direction Castling is occuring in: King side (1) or Queen side (-1).
        int direction = rookCol > this.getCol() ? 1 : -1;

        //Iterate through the necessary squares to ensure there are no Pieces blocking Castling.
        for (int col = this.getCol() + direction; col != rookCol; col += direction) {
            if (!boardCurrent[col][this.getRow()].equals("null")) {
                //If a square is not null, that means that there is a Piece preventing Castling.
                return false;
            }
        }

        //There are no Pieces preventing Castling.
        return true;
    }

    //This method checks to ensure that a given square is not threatened by the enemy.
    private boolean isSquareThreatened(int col, int row, ArrayList<Tile> threatenedSquares) {
        //Iterate through all threatened squares.
        for (Tile tile : threatenedSquares) {
            if (tile.getCol() == col && tile.getRow() == row) {
                //The target square is threatened by the enemy.
                return true;
            }
        }

        //The target square is not threatened by the enemy.
        return false;
    }

}
