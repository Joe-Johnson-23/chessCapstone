package me.chessCapstone;


public class customAi {
    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;
    private static final int KING_MOVE_PENALTY = 1000;
    private static final int DEVELOPMENT_BONUS = 50;

    private final boolean isWhite;

    // Piece position tables (higher values for better squares)
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

    public int evaluatePosition(Board board, Move lastMove) {
        int score = 0;

        if ( lastMove.piece.contains("king")) {

            score += KING_MOVE_PENALTY;

        }

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                String piece = board.get(col, row);
                if (!piece.equals("null")) {
                    score += getMaterialValue(piece);
                    score += getPositionValue(piece, col, row);

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


        score += evaluateCenterControl(board) * 3;

        return isWhite ? score : -score;
    }

    private int getMaterialValue(String piece) {
        boolean isPieceWhite = piece.contains("white");
        int multiplier = (isPieceWhite == isWhite) ? 1 : -1;

        if (piece.contains("pawn")) return PAWN_VALUE * multiplier;
        if (piece.contains("knight")) return KNIGHT_VALUE * multiplier;
        if (piece.contains("bishop")) return BISHOP_VALUE * multiplier;
        if (piece.contains("rook")) return ROOK_VALUE * multiplier;
        if (piece.contains("queen")) return QUEEN_VALUE * multiplier;

        return 0;
    }

    private int getPositionValue(String piece, int col, int row) {
        boolean isPieceWhite = piece.contains("white");
        int multiplier = (isPieceWhite == isWhite) ? 1 : -1;
        // Flip row index for black pieces to use same tables
        if (!isPieceWhite) {
            row = 7 - row;
        }

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

        return 0;
    }



    private int evaluateCenterControl(Board board) {
        int centerScore = 0;
        int[][] centerSquares = {{3,3}, {3,4}, {4,3}, {4,4}};

        for (int[] square : centerSquares) {
            String piece = board.get(square[0], square[1]);
            if (!piece.equals("null")) {
                boolean isPieceWhite = piece.contains("white");
                if (isPieceWhite == isWhite) {
                    centerScore += 10;
                }
            }
        }

        return centerScore;
    }
}


