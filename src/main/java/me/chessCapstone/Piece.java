package me.chessCapstone;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.Objects;


//The Piece class serves as a superclass for all other chess pieces (e.g. King, Queen, etc.).
public abstract class Piece extends Node {

    //The given piece's type and color (e.g. King, black).
    protected String type, color;

    //The given piece's image representation.
    protected ImageView piece;

    //A boolean to track movement. Primarily used for castling rights.
    private boolean hasMoved = false;

    //The given piece's current location on the chess board.
    protected int col, row;

    public Piece(String type, String color) {
        this.type = type;
        this.color = color;
        setPiece();
    }

    //Returns the type of piece.
    public String getType() {
        return type;
    }

    //Sets the type of piece.
    public void setType(String type) {
        this.type = type;
    }

    //Returns the color of the piece.
    public String getColor() {
        return color;
    }

    //Sets the color of the piece.
    public void setColor(String color) {
        this.color = color;
    }

    //Gets the image of the piece.
    public ImageView getPiece() {
        return piece;
    }

    //Sets the image of the piece. Only done once on initialization.
    public void setPiece() {
        //Path is chosen based on the naming scheme of the individual images.
        String path = "/pngPiece/" + getColor() + "-" + getType() + ".png";
        //String is turned into an Image object.
        Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
        //The Image data is then turned into an ImageView object and then set as the ImageView of the given Piece.
        piece = new ImageView(image);
        piece.setFitWidth(100);
        piece.setFitHeight(100);
    }

    //Returns the current column of the Piece.
    public int getCol() {
        return col;
    }

    //Sets the current column of the Piece.
    public void setCol(int col) {
        this.col = col;
    }

    //Returns the current row of the Piece.
    public int getRow() {
        return row;
    }

    //Sets the current row of the Piece.
    public void setRow(int row) {
        this.row = row;
    }

    //Returns the boolean hasMoved.
    public boolean hasMoved() {
        return hasMoved;
    }

    //Sets the boolean hasMoved.
    public void setMoved(boolean moved) {
        this.hasMoved = moved;
    }

    //This method is called when setOnMousePressed event occurs. It finds all valid moves are highlights a small circle inside of the appropriate square.
    public void highlightValidMoves(StackPane[][] stiles, String[][] boardCurrent, ArrayList<Tile> threatenedSquares, ChessGame game) {

        //Iteration through the entire board.
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                boolean isValidMove = false;
                Color highlightColor2 = Color.WHITE;

                //The Piece is downcasted into it's appropriate Piece type in order to access individual movement types and movement restrictions.
                //If the given square is a valid move and said move protects the King (if applicable; in check), it is highlighted.
                switch(this.getType()) {
                    case "queen":
                        if (((Queen) this).isValidQueenMove(col, row, boardCurrent) &&
                                game.simulateMoveProtectKing(this, col, row)) {
                            isValidMove = true;
                            highlightColor2 = Color.PEACHPUFF;
                        }
                        break;
                    case "king":
                        if ((((King) this).isValidKingMove(col, row, boardCurrent, threatenedSquares) ||
                                ((King) this).isCastlingValid(col, boardCurrent, threatenedSquares)) &&
                                game.simulateMoveProtectKing(this, col, row)) {
                            isValidMove = true;
                            highlightColor2 = Color.LAVENDER;
                        }
                        break;
                    case "rook":
                        if (((Rook) this).isValidRookMove(col, row, boardCurrent) &&
                                game.simulateMoveProtectKing(this, col, row)) {
                            isValidMove = true;
                            highlightColor2 = Color.TOMATO;
                        }
                        break;
                    case "bishop":
                        if (((Bishop) this).isValidBishopMove(col, row, boardCurrent) &&
                                game.simulateMoveProtectKing(this, col, row)) {
                            isValidMove = true;
                            highlightColor2 = Color.GOLDENROD;
                        }
                        break;
                    case "knight":
                        if (((Knight) this).isValidKnightMove(col, row, boardCurrent) &&
                                game.simulateMoveProtectKing(this, col, row)) {
                            isValidMove = true;
                            highlightColor2 = Color.MEDIUMAQUAMARINE;
                        }
                        break;
                    case "pawn":
                        if (((Pawn) this).isValidPawnMove(col, row, boardCurrent) &&
                                game.simulateMoveProtectKing(this, col, row)) {
                            isValidMove = true;
                            highlightColor2 = Color.CORNFLOWERBLUE;
                        }
                        break;
                }

                //If the switch case returned a valid move,
                if (isValidMove) {

                    StackPane tilePane = stiles[row][col];
                    addHighlightCircle(tilePane, highlightColor2);
                }
            }
        }
    }

    //Adds a small circle in the center of a StackPane to reflect a valid Piece move.
    //Generally called after highlightValidMoves
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

    //Checks if a given location on the chess board is a potentially valid square for the Piece.
    public boolean isValidMove(int col, int row, String[][] boardCurrent, ArrayList<Tile> threatenedSquares) {

        //The Piece is downcasted into it's appropriate Piece type in order to access individual movement types and movement restrictions.
        //The King, in particular, takes the threatenedSquares ArrayList in order to not move into check.
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

    //Returns an ArrayList containing all the Squares it controls.
    public ArrayList<Tile> findThreatenedSquares(String[][] boardCurrent) {

        ArrayList<Tile> threatenedSquares =  new ArrayList<Tile>();

        //Iteration through the entire board to find all threatened squares.
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {

                //The Piece is downcasted and the given square is checked. If the square is valid move for said Piece, it is added to the ArrayList.
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

    //Accounts for blockages in between a Piece and a square, such as another Piece.
    public boolean isPathClear(int endCol, int endRow, String[][] boardCurrent) {

        //Calculates the direction of the target square.
        int colDirection = Integer.compare(endCol, getCol());
        int rowDirection = Integer.compare(endRow, getRow());

        //Returns the String representation for the given Piece in the board.
        String currentPiece = boardCurrent[getCol()][getRow()];

        //Determines who's turn it currently is: white or black.
        boolean isWhite = currentPiece.contains("white");

        //Calculates the next target square.
        int currentCol = getCol() + colDirection;
        int currentRow = getRow() + rowDirection;

        while (currentCol != endCol || currentRow != endRow) {
            //Path is blocked by a Piece.
            if (!"null".equals(boardCurrent[currentCol][currentRow])) {
                return false;
            }
            //Iterates in order to check the next potential square.
            currentCol += colDirection;
            currentRow += rowDirection;
        }

        // Check the destination square
        String destinationPiece = boardCurrent[endCol][endRow];

        if ("null".equals(destinationPiece)) {
            //Target square is empty, path is clear.
            return true;
        } else {
            //If the piece on the target square is the opponent's allow capture.
            return isWhite ? destinationPiece.contains("black") : destinationPiece.contains("white");
        }
    }
}

