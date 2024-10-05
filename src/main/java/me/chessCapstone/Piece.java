package me.chessCapstone;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.util.Objects;

public class Piece extends Node {

    protected String type; // e.g., "Pawn", "Knight", etc.
    protected String color; // e.g., "White", "Black"
    protected ImageView piece;

    public Piece(String type, String color) {
        this.type = type;
        this.color = color;
        setPiece();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public ImageView getPiece() {
        return piece;
    }

    public void setPiece() {
        String path = "/pngPiece/" + getColor() + "-" + getType() + ".png";
        Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
        piece = new ImageView(image);
        piece.setFitWidth(100);
        piece.setFitHeight(100);
    }

    public void highlightValidMoves(int startCol, int startRow, StackPane[][] stiles, String[][] boardCurrent) {

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {

                switch(this.getType()) {
                    case "queen":
                        if(((Queen) this).isValidQueenMove(startCol, startRow, col, row, boardCurrent)) {
                            stiles[row][col].setStyle("-fx-background-color: LIMEGREEN;");
                        }
                        break;
                    case "bishop":
                        if(((Bishop) this).isValidBishopMove(startCol, startRow, col, row, boardCurrent)) {
                            stiles[row][col].setStyle("-fx-background-color: RED;");
                        }
                        break;
                    case "knight":
                        if(((Knight) this).isValidKnightMove(startCol, startRow, col, row, boardCurrent)) {
                            stiles[row][col].setStyle("-fx-background-color: BLUE;");
                        }
                        break;
                    case "pawn":
                        if(((Pawn) this).isValidPawnMove(startCol, startRow, col, row, boardCurrent)) {
                            stiles[row][col].setStyle("-fx-background-color: PURPLE;");
                        }
                        break;
                    case "rook":
                        if(((Rook) this).isValidRookMove(startCol, startRow, col, row, boardCurrent)) {
                            stiles[row][col].setStyle("-fx-background-color: YELLOW;");
                        }
                        break;
                    case "king":
                        if(((King) this).isValidKingMove(startCol, startRow, col, row, boardCurrent)) {
                            stiles[row][col].setStyle("-fx-background-color: PINK;");
                        }
                        break;
                }
            }
        }
    }

    public boolean isValidMove(int initialPieceCoordinateCOL, int initialPieceCoordinateROW, int col, int row, String[][] boardCurrent) {

        return switch (this.getType()) {
            case "queen" ->
                    ((Queen) this).isValidQueenMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
            case "bishop" ->
                    ((Bishop) this).isValidBishopMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
            case "knight" ->
                    ((Knight) this).isValidKnightMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
            case "pawn" ->
                    ((Pawn) this).isValidPawnMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
            case "rook" ->
                    ((Rook) this).isValidRookMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
            case "king" ->
                    ((King) this).isValidKingMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
            default -> false;
        };
    }

    public boolean isPathClear(int startCol, int startRow, int endCol, int endRow, String[][] boardCurrent) {
        int colDirection = Integer.compare(endCol, startCol);
        int rowDirection = Integer.compare(endRow, startRow);

        String currentPiece = boardCurrent[startCol][startRow];
        boolean isWhite = currentPiece.contains("white");

        int currentCol = startCol + colDirection;
        int currentRow = startRow + rowDirection;

        while (currentCol != endCol || currentRow != endRow) {
            if (!"null".equals(boardCurrent[currentCol][currentRow])) {
                return false; // Path is blocked by a piece
            }
            currentCol += colDirection;
            currentRow += rowDirection;
        }

        // Check the destination square
        String destinationPiece = boardCurrent[endCol][endRow];
        if ("null".equals(destinationPiece)) {
            return true; // Destination is empty, path is clear
        } else {
            // Allow capture of opponent's piece
            return isWhite ? destinationPiece.contains("black") : destinationPiece.contains("white");
        }
    }

}
