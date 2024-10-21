package me.chessCapstone;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.Objects;

public abstract class Piece extends Node {

    protected String type; // e.g., "Pawn", "Knight", etc.
    protected String color; // e.g., "White", "Black"
    protected ImageView piece;
    private boolean hasMoved = false;
    protected int col, row;

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

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public void setPiece() {
        String path = "/pngPiece/" + getColor() + "-" + getType() + ".png";
        Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
        piece = new ImageView(image);
        piece.setFitWidth(100);
        piece.setFitHeight(100);
    }

    public void highlightValidMoves(StackPane[][] stiles, String[][] boardCurrent, ArrayList<Tile> threatenedSquares, ChessGame game) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                boolean isValidMove = false;
                // String highlightColor = "";
                Color highlightColor2 = Color.WHITE;

                switch(this.getType()) {
                    case "queen":
                        if (((Queen) this).isValidQueenMove(col, row, boardCurrent) &&
                                game.simulateMoveProtectKing(this, col, row)) {
                            isValidMove = true;
                            // highlightColor = "-fx-background-color: rgba(255, 192, 203, 0.5);";
                            highlightColor2 = Color.PEACHPUFF;
                        }
                        break;
                    case "king":
                        if ((((King) this).isValidKingMove(col, row, boardCurrent, threatenedSquares) ||
                                ((King) this).isCastlingValid(col, boardCurrent, threatenedSquares)) &&
                                game.simulateMoveProtectKing(this, col, row)) {
                            isValidMove = true;
                            //highlightColor = "-fx-background-color: ORANGE;";
                            highlightColor2 = Color.LAVENDER;
                        }
                        break;
                    case "rook":
                        if (((Rook) this).isValidRookMove(col, row, boardCurrent) &&
                                game.simulateMoveProtectKing(this, col, row)) {
                            isValidMove = true;
                            // highlightColor = "-fx-background-color: RED;";
                            highlightColor2 = Color.TOMATO;
                        }
                        break;
                    case "bishop":
                        if (((Bishop) this).isValidBishopMove(col, row, boardCurrent) &&
                                game.simulateMoveProtectKing(this, col, row)) {
                            isValidMove = true;
                            // highlightColor = "-fx-background-color: YELLOW;";
                            highlightColor2 = Color.GOLDENROD;
                        }
                        break;
                    case "knight":
                        if (((Knight) this).isValidKnightMove(col, row, boardCurrent) &&
                                game.simulateMoveProtectKing(this, col, row)) {
                            isValidMove = true;
                            // highlightColor = "-fx-background-color: GREEN;";
                            highlightColor2 = Color.MEDIUMAQUAMARINE;
                        }
                        break;
                    case "pawn":
                        if (((Pawn) this).isValidPawnMove(col, row, boardCurrent) &&
                                game.simulateMoveProtectKing(this, col, row)) {
                            isValidMove = true;
                            // highlightColor = "-fx-background-color: BLUE;";
                            highlightColor2 = Color.CORNFLOWERBLUE;
                        }
                        break;
                }

                if (isValidMove) {
                    // stiles[row][col].setStyle(highlightColor);

                    StackPane tilePane = stiles[row][col];
                    addHighlightCircle(tilePane, highlightColor2);
                }
            }
        }
    }
    private void addHighlightCircle(StackPane tilePane, Color color) {
        // Remove any existing highlight circles
        tilePane.getChildren().removeIf(node -> node instanceof Circle);

        // Create a new circle
        Circle highlightCircle = new Circle(15); // Adjust the size as needed
        highlightCircle.setFill(color.deriveColor(0, 1, 1, 0.8)); // 50% opacity
        highlightCircle.setStroke(color);
        highlightCircle.setStrokeWidth(2);

        // Add the circle to the tile
        tilePane.getChildren().add(highlightCircle);
    }

    public boolean isValidMove(int col, int row, String[][] boardCurrent, ArrayList<Tile> threatenedSquares) {

        return switch (this.getType()) {
            case "queen" ->
                    ((Queen) this).isValidQueenMove(col, row, boardCurrent);
            case "bishop" ->
                    ((Bishop) this).isValidBishopMove(col, row, boardCurrent);
            case "knight" ->
                    ((Knight) this).isValidKnightMove(col, row, boardCurrent);
            case "pawn" ->
                    ((Pawn) this).isValidPawnMove(col, row, boardCurrent);
            case "rook" ->
                    ((Rook) this).isValidRookMove(col, row, boardCurrent);
            case "king" ->
                    ((King) this).isValidKingMove(col, row, boardCurrent, threatenedSquares);
            default -> false;
        };
    }

    public ArrayList<Tile> findThreatenedSquares(String[][] boardCurrent) {

        ArrayList<Tile> threatenedSquares =  new ArrayList<Tile>();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {

                switch(this.getType()) {
                    case "queen":
                        if(((Queen) this).isValidQueenMove(col, row, boardCurrent)) {
                            threatenedSquares.add(new Tile(col, row));
                        }
                        break;
                    case "bishop":
                        if(((Bishop) this).isValidBishopMove(col, row, boardCurrent)) {
                            threatenedSquares.add(new Tile(col, row));
                        }
                        break;
                    case "knight":
                        if(((Knight) this).isValidKnightMove(col, row, boardCurrent)) {
                            threatenedSquares.add(new Tile(col, row));
                        }
                        break;
                    case "pawn":
                        if(((Pawn) this).isThreatenedSquare(col, row, boardCurrent)) {
                            threatenedSquares.add(new Tile(col, row));
                        }
                        break;
                    case "rook":
                        if(((Rook) this).isValidRookMove(col, row, boardCurrent)) {
                            threatenedSquares.add(new Tile(col, row));
                        }
                        break;
                    case "king":
                        if(((King) this).isValidKingMove(col, row, boardCurrent, null)) {
                            threatenedSquares.add(new Tile(col, row));
                        }
                        break;
                }
            }
        }
        return threatenedSquares;
    }

    public boolean isPathClear(int endCol, int endRow, String[][] boardCurrent) {
        int colDirection = Integer.compare(endCol, getCol());
        int rowDirection = Integer.compare(endRow, getRow());

        String currentPiece = boardCurrent[getCol()][getRow()];
        boolean isWhite = currentPiece.contains("white");

        int currentCol = getCol() + colDirection;
        int currentRow = getRow() + rowDirection;

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

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setMoved(boolean moved) {
        this.hasMoved = moved;
    }

}

