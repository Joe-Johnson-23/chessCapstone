package me.chessCapstone;


//Custom chess AI implementation using piece-square tables and material evaluation
//Evaluates positions based on piece values, position values, and strategic factors

public class customAi {
    //Material values for pieces (in centipawns)
    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;
    //Penalty for moving the king
    private static final int KING_MOVE_PENALTY = 1000;
    //Bonus for developing pieces
    private static final int DEVELOPMENT_BONUS = 50;

    //Determines if AI is playing as white or black
    private final boolean isWhite;

    //Piece position tables (higher values for better squares)
    private static final int[][] PAWN_POSITION = {
            { 0,  0,  0,  0,  0,  0,  0,  0},
            {50, 50, 50, 50, 50, 50, 50, 50},
            {10, 10, 20, 30, 30, 20, 10, 10},
            { 5,  5, 10, 25, 25, 10,  5,  5},
            { 0,  0,  0, 20, 20,  0,  0,  0},
            { 5, -5,-10,  0,  0,-10, -5,  5},
            { 5, 10, 10,-20,-20, 10, 10,  5},
            { 0,  0,  0,  0,  0,  0,  0,  0}
    };

    private static final int[][] KNIGHT_POSITION = {
            {-50,-40,-30,-30,-30,-30,-40,-50},
            {-40,-20,  0,  0,  0,  0,-20,-40},
            {-30,  0, 10, 15, 15, 10,  0,-30},
            {-30,  5, 15, 20, 20, 15,  5,-30},
            {-30,  0, 15, 20, 20, 15,  0,-30},
            {-30,  5, 10, 15, 15, 10,  5,-30},
            {-40,-20,  0,  5,  5,  0,-20,-40},
            {-50,-40,-30,-30,-30,-30,-40,-50}
    };

    private static final int[][] BISHOP_POSITION = {
            {-20,-10,-10,-10,-10,-10,-10,-20},
            {-10,  0,  0,  0,  0,  0,  0,-10},
            {-10,  0,  5, 10, 10,  5,  0,-10},
            {-10,  5,  5, 10, 10,  5,  5,-10},
            {-10,  0, 10, 10, 10, 10,  0,-10},
            {-10, 10, 15, 10, 10, 15, 10,-10},
            {-10, 20,  0,  0,  0,  0, 20,-10},
            {-20,-10,-10,-10,-10,-10,-10,-20}
    };

    private static final int[][] ROOK_POSITION = {
            { 0,  0,  0,  0,  0,  0,  0,  0},
            { 5, 10, 10, 10, 10, 10, 10,  5},
            {-5,  0,  0,  0,  0,  0,  0, -5},
            {-5,  0,  0,  0,  0,  0,  0, -5},
            {-5,  0,  0,  0,  0,  0,  0, -5},
            {-5,  0,  0,  0,  0,  0,  0, -5},
            {-5,  0,  0,  0,  0,  0,  0, -5},
            { 0,  0,  0,  5,  5,  0,  0,  0}
    };

    private static final int[][] QUEEN_POSITION = {
            {-20,-10,-10, -5, -5,-10,-10,-20},
            {-10,  0,  0,  0,  0,  0,  0,-10},
            {-10,  0,  5,  5,  5,  5,  0,-10},
            { -5,  0,  5,  5,  5,  5,  0, -5},
            {  0,  0,  5,  5,  5,  5,  0, -5},
            {-10,  5,  5,  5,  5,  5,  0,-10},
            {-10,  0,  5,  0,  0,  0,  0,-10},
            {-20,-10,-10, -5, -5,-10,-10,-20}
    };

    public customAi(boolean isWhite) {
        this.isWhite = isWhite;
    }

    //Evaluates current board position
    public int evaluatePosition(Board board, Move lastMove) {
        int score = 0;

        //Penalize king movement in early game
        if ( lastMove.piece.contains("king")) {
            score += KING_MOVE_PENALTY;
        }

        //Evaluate each square on the board
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                String piece = board.get(col, row);
                if (!piece.equals("null")) {
                    //Add material value
                    score += getMaterialValue(piece);
                    //Add position value
                    score += getPositionValue(piece, col, row);
                    //Add development bonus for minor pieces
                    if ((piece.contains("knight") || piece.contains("bishop")) &&
                            piece.contains(isWhite ? "white" : "black")) {
                        boolean isStartingRank = (isWhite && row == 7) || (!isWhite && row == 0);
                        if (!isStartingRank) {
                            score += DEVELOPMENT_BONUS;
                        }
                    }
                }
            }
        }


        String capturedPiece = board.get(lastMove.endCol, lastMove.endRow);
        if (!capturedPiece.equals("null")) {
            score += getMaterialValue(capturedPiece);
        }

        //Evaluate center control
        score += evaluateCenterControl(board) * 3;

        //Final adjustment based on AI's color
        return isWhite ? score : -score;
    }

    //Calculates the material value of a piece
    private int getMaterialValue(String piece) {
        //Determine piece color and set appropriate multiplier
        boolean isPieceWhite = piece.contains("white");
        //Multiplier is 1 for AI's pieces, -1 for opponent's pieces
        int multiplier = (isPieceWhite == isWhite) ? 1 : -1;
        //Return appropriate value based on piece type
        if (piece.contains("pawn")) return PAWN_VALUE * multiplier;
        if (piece.contains("knight")) return KNIGHT_VALUE * multiplier;
        if (piece.contains("bishop")) return BISHOP_VALUE * multiplier;
        if (piece.contains("rook")) return ROOK_VALUE * multiplier;
        if (piece.contains("queen")) return QUEEN_VALUE * multiplier;

        //Return 0 for kings
        return 0;
    }

    //Calculates the positional value of a piece based on its location
    private int getPositionValue(String piece, int col, int row) {
        boolean isPieceWhite = piece.contains("white");
        int multiplier = (isPieceWhite == isWhite) ? 1 : -1;
        // Flip row index for black pieces to use same position tables
        if (!isPieceWhite) {
            row = 7 - row;
        }

        //Return position value based on piece type
        if (piece.contains("pawn")) {
            return PAWN_POSITION[row][col] * multiplier;
        }
        if (piece.contains("knight")) {
            return KNIGHT_POSITION[row][col] * multiplier;
        }
        if (piece.contains("bishop")) {
            return BISHOP_POSITION[row][col] * multiplier;
        }
        if (piece.contains("rook")) {
            return ROOK_POSITION[row][col] * multiplier;
        }
        if (piece.contains("queen")) {
            return QUEEN_POSITION[row][col] * multiplier;
        }

        //Return 0 for kings
        return 0;
    }



    //Evaluates control of the center squares of the board
    //Center control is crucial for chess strategy
    private int evaluateCenterControl(Board board) {
        int centerScore = 0;
        //Define the four center squares
        int[][] centerSquares = {{3,3}, {3,4}, {4,3}, {4,4}};

        //Check each center square
        for (int[] square : centerSquares) {
            String piece = board.get(square[0], square[1]);
            if (!piece.equals("null")) {
                //Add to score if AI's piece controls center
                boolean isPieceWhite = piece.contains("white");
                if (isPieceWhite == isWhite) {
                    //Bonus for controlling center square
                    centerScore += 10;
                }
            }
        }

        return centerScore;
    }
}

