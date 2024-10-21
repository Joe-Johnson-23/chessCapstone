package me.chessCapstone;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.geometry.Point2D;

import java.util.*;

public class ChessGame extends Application {
    private static final int TILE_SIZE = 100;
    private static final int BOARD_SIZE = 8;
    private boolean isWhiteTurn = true;

    HashMap<String, Piece> pieces = new HashMap<>();
    private Board boardCurrent;
    private ArrayList<Tile> squaresThreatenedByWhite = new ArrayList<>();
    private ArrayList<Tile> squaresThreatenedByBlack = new ArrayList<>();


    private int initialPieceCoordinateROW;
    private int initialPieceCoordinateCOL;

    private ImageView selectedPiece = null;

    private GridPane gridPane;

    /////////////////////////////// GLOBAL VARIABLES ///////////////////////////////
    //variable to add new piece upon promotion, so that board state has unique value
    //if not 'queenwhite' will be made upon promotion instead of "queen3white"
    //problem occurs if multiple pawns promote to queen (ie, "queenwhite" then "queenwhite")
    //thus need global for now
    int globalCountForPromotion = 2;

    static boolean lastMoveWasDoublePawnMove = false;
    static String lastPawnMoved = "";
    static boolean EnPassantPossible = false;
    /////////////////////////////// GLOBAL VARIABLES ///////////////////////////////
    private Map<String, ImageView> imageViewMap = new HashMap<>();
    private King whiteKing;
    private King blackKing;
    private Tile enPassantTile = new Tile();
    private int halfMoveClock = 0, numberOfMoves = 1;

    @Override
    public void start(Stage primaryStage) {
        gridPane = new GridPane();
        boardCurrent = new Board(gridPane);
        setUpPieces(gridPane);

        // 배열 초기화
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (boardCurrent.get(col, row) == null) {
                    boardCurrent.set(col, row, "null");
                }
            }
        }

        //just to initialize kings to their respective variables in the boardCurrent
        initializeGame();

        gridPane.setOnMousePressed(event -> {
            int col = (int) (event.getSceneX() / TILE_SIZE);
            int row = (int) (event.getSceneY() / TILE_SIZE);
            initialPieceCoordinateCOL = col;
            initialPieceCoordinateROW = row;

            String typeOfPiece = boardCurrent.get(col, row);
            Piece piece = pieces.get(typeOfPiece);
            Piece playerKing;
            ArrayList<Tile> threatenedSquares;
            calculateThreatenedSquares();

            if ((isWhiteTurn && typeOfPiece.contains("white")) || (!isWhiteTurn && typeOfPiece.contains("black"))) {
                selectedPiece = imageViewMap.get(typeOfPiece);

                //System.out.println("    "+typeOfPiece);


                if (selectedPiece != null) {
                    // Store the initial mouse position
                    selectedPiece.setUserData(new Point2D(event.getSceneX(), event.getSceneY()));
                    // Bring the selected piece to the front
                    selectedPiece.toFront();
                }

                if(isWhiteTurn) {
                    playerKing = pieces.get("king1white");
                    threatenedSquares = squaresThreatenedByBlack;
                } else {
                    playerKing = pieces.get("king1black");
                    threatenedSquares = squaresThreatenedByWhite;
                }

                // if(!((King) playerKing).isInCheck(threatenedSquares)) {
                //     piece.highlightValidMoves(stiles, boardCurrent, threatenedSquares);
                // } else if (((King) playerKing).isInCheck(threatenedSquares) && playerKing.equals(piece)) {
                //     piece.highlightValidMoves(stiles, boardCurrent, threatenedSquares);
                // }

                piece.highlightValidMoves(boardCurrent.getStiles(), boardCurrent.getBoard(), threatenedSquares, this);

            }
        });

        Scene scene = new Scene(gridPane, TILE_SIZE * BOARD_SIZE, TILE_SIZE * BOARD_SIZE);

        scene.setOnMouseDragged(event -> {
            if (selectedPiece != null) {
                Point2D initialPos = (Point2D) selectedPiece.getUserData();
                double deltaX = event.getSceneX() - initialPos.getX();
                double deltaY = event.getSceneY() - initialPos.getY();
                selectedPiece.setTranslateX(deltaX);
                selectedPiece.setTranslateY(deltaY);
            }
        });

        scene.setOnMouseReleased(event -> {
            if (selectedPiece != null) {
                // Reset the translation
                selectedPiece.setTranslateX(0);
                selectedPiece.setTranslateY(0);

                String typeOfPiece = boardCurrent.get(initialPieceCoordinateCOL, initialPieceCoordinateROW);
                //checking whose turn, in case of wrong click
                if ((isWhiteTurn && typeOfPiece.contains("white")) || (!isWhiteTurn && typeOfPiece.contains("black"))) {

                    if (selectedPiece != null) {

                        selectedPiece.setLayoutX(0);
                        selectedPiece.setLayoutY(0);
                        Piece piece = pieces.get(boardCurrent.get(initialPieceCoordinateCOL, initialPieceCoordinateROW));
                        boolean validMove = false;
                        Piece playerKing;
                        ArrayList<Tile> threatenedSquares;

                        if (piece != null && piece.getColor().equals("white")) {
                            playerKing = pieces.get("king1white");
                            threatenedSquares = squaresThreatenedByBlack;
                        } else {
                            playerKing = pieces.get("king1black");
                            threatenedSquares = squaresThreatenedByWhite;
                        }


                        boardCurrent.resetTileColor();

                        // convert mouse coordinates to local Gridpane coordinates
                        Point2D localPoint = gridPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                        double x = localPoint.getX();
                        double y = localPoint.getY();

                        int col = (int) (x / TILE_SIZE);
                        int row = (int) (y / TILE_SIZE);

                        if (col >= 0 && col < BOARD_SIZE && row >= 0 && row < BOARD_SIZE) {
                            // check if the move is valid

                            String pieceType = typeOfPiece.replaceAll("\\d", ""); // rmove digit

                            System.out.println(piece);

                            //checks if any possible move is valid in regards to check
                            validMove = simulateMoveProtectKing(piece, col, row);

                            if (validMove) {
                                // Check for castling
                                if (pieceType.contains("king") && Math.abs(col - initialPieceCoordinateCOL) == 2) {
                                    King king = (King) piece;
                                    if (king.isCastlingValid(col, boardCurrent.getBoard(), isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite)) {
                                        handleCastling(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row);
                                    } else {
                                        // Invalid castling, return king to original position
                                        gridPane.getChildren().remove(selectedPiece);
                                        gridPane.add(selectedPiece, initialPieceCoordinateCOL, initialPieceCoordinateROW);
                                        return;
                                    }
                                }


                                //check for double pawn move (en passant)
                                if (pieceType.contains("pawn") && Math.abs(row - initialPieceCoordinateROW) == 2) {
                                    lastMoveWasDoublePawnMove = true;
                                    lastPawnMoved = boardCurrent.get(initialPieceCoordinateCOL, initialPieceCoordinateROW);
                                    enPassantTile.setCol(initialPieceCoordinateCOL);
                                    enPassantTile.setRow(initialPieceCoordinateROW + 2);
                                } else {
                                    lastMoveWasDoublePawnMove = false;
                                    enPassantTile.setCol(-1);
                                    enPassantTile.setRow(-1);
                                }

                                //check for en passant, if possible, remove captured pawn from board and GUI
                                if (pieceType.contains("pawn") && EnPassantPossible ) {

                                    int capturedPawnRow;
                                    if (pieceType.contains("white")) {
                                        capturedPawnRow = row + 1; //White pawn captures upward
                                    } else {
                                        capturedPawnRow = row - 1; //Black pawn captures downward
                                    }
                                    //Remove the captured pawn from the board
                                    boardCurrent.set(col, capturedPawnRow,"null");
                                    //Remove the captured pawn's image from the GUI
                                    gridPane.getChildren().remove(imageViewMap.get(lastPawnMoved));
                                    imageViewMap.remove(lastPawnMoved);

                                }

                                //Move from current position to new spot
                                gridPane.getChildren().remove(selectedPiece);
                                gridPane.add(selectedPiece, col, row);
                                piece.setCol(col);
                                piece.setRow(row);

                                // update boardCurrent
                                String currentPiece = boardCurrent.get(initialPieceCoordinateCOL, initialPieceCoordinateROW);
                                // String destinationPiece = boardCurrent[col][row];

                                //to capture piece.
                                String toBeRemoved = boardCurrent.get(col, row);
                                if (!toBeRemoved.equals("null")) {
                                    gridPane.getChildren().remove(imageViewMap.get(toBeRemoved));
                                    imageViewMap.remove(toBeRemoved);
                                }

                                boardCurrent.set(initialPieceCoordinateCOL, initialPieceCoordinateROW,"null");
                                boardCurrent.set(col, row, currentPiece);

                                // check and handle pawn promotion
                                if (currentPiece.contains("pawn")) {
                                    boolean isWhite = currentPiece.contains("white");
                                    if ((isWhite && row == 0) || (!isWhite && row == 7)) {
                                        handlePawnPromotion(currentPiece, col, row, isWhite);
                                    }
                                }

                                // Update piece's moved status
                                piece.setMoved(true);

                                if(!isWhiteTurn) {
                                    numberOfMoves++;
                                }

                                if(currentPiece.contains("pawn") || !toBeRemoved.equals("null")) {
                                    halfMoveClock = 0;
                                } else {
                                    halfMoveClock++;
                                }

                                switchTurn();

                                calculateThreatenedSquares();

                                if (isCheckmate()) {
                                    // Handle checkmate
                                    System.out.println(isWhiteTurn ? "Black wins by checkmate!" : "White wins by checkmate!");
                                }
                            }else {
                                // if move invalid, return to last position
                                gridPane.getChildren().remove(selectedPiece);
                                gridPane.add(selectedPiece, initialPieceCoordinateCOL, initialPieceCoordinateROW);
                            }
                        } else {
                            // if move outside the board, return to last position
                            gridPane.getChildren().remove(selectedPiece);
                            gridPane.add(selectedPiece, initialPieceCoordinateCOL, initialPieceCoordinateROW);
                        }

                        // reset initial coordinate
                        initialPieceCoordinateROW = -1;
                        initialPieceCoordinateCOL = -1;

                        // printBoardState();
                        calculateThreatenedSquares();
                        selectedPiece = null; // Reset selected piece

                    }


                }
            }
            System.out.println(boardCurrent.getFENNotation(pieces, isWhiteTurn, enPassantTile, halfMoveClock, numberOfMoves));
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("Chess Game");
        primaryStage.show();
    }

    private void switchTurn() {
        isWhiteTurn = !isWhiteTurn;
    }

    private void handlePawnPromotion(String currentPiece, int col, int row, boolean isWhite) {
        List<String> promotionOptions = Arrays.asList("Queen", "Rook", "Bishop", "Knight");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Queen", promotionOptions);
        dialog.setTitle("Pawn promotion");
        dialog.setHeaderText(null);
        dialog.setContentText("Choose piece you want to promote:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(choice -> {
            // remove the current pawn image
            ImageView pawnView = imageViewMap.get(currentPiece);
            gridPane.getChildren().remove(pawnView);
            imageViewMap.remove(currentPiece);

            globalCountForPromotion = globalCountForPromotion + 1;
            // create new piece
            String newPieceType = choice.toLowerCase();
            String newPieceName = newPieceType + globalCountForPromotion+ (isWhite ? "white" : "black");
            String newColor = (isWhite ? "white" : "black");
            Piece promotedPiece = createPiece(newPieceType, isWhite ? "white" : "black");
            ImageView promotedPieceView = promotedPiece.getPiece();


            pieces.put(promotedPiece.getType() +globalCountForPromotion+ newColor, promotedPiece);


            // add new piece
            gridPane.add(promotedPieceView, col, row);
            boardCurrent.set(col, row, newPieceName);
            promotedPiece.setCol(col);
            promotedPiece.setRow(row);
            imageViewMap.put(newPieceName, promotedPieceView);
        });
    }

    private void calculateThreatenedSquares() {
        squaresThreatenedByWhite.clear();
        squaresThreatenedByBlack.clear();

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = pieces.get(boardCurrent.get(col, row));
                if (piece != null) {
                    ArrayList<Tile> threatenedSquares = piece.findThreatenedSquares(boardCurrent.getBoard());
                    if (piece.getColor().equals("white")) {
                        squaresThreatenedByWhite.addAll(threatenedSquares);
                    } else {
                        squaresThreatenedByBlack.addAll(threatenedSquares);
                    }
                }
            }
        }
    }

    private void setUpPieces(GridPane gridPane) {


        String[] pieceList = {"rook1", "knight1", "bishop1", "queen1", "king1", "bishop2", "knight2", "rook2",
                "pawn0", "pawn1", "pawn2", "pawn3", "pawn4", "pawn5", "pawn6", "pawn7"};

        String[] colors = {"black", "white"};

        int row = 0, col = 0;

        for (String color : colors) {

            for (int y = 0; y < pieceList.length; y++) {

                if (col == 8) {
                    if (row == 1) {
                        row = 7;
                    } else if (row == 7) {
                        row--;
                    } else {
                        row++;
                    }
                    col = 0;
                }

                Piece nextPiece = createPiece(pieceList[y], color);
                String typeColor = pieceList[y] + color;
                pieces.put(typeColor, nextPiece);
                boardCurrent.set(col, row, typeColor);
                assert nextPiece != null;
                nextPiece.setCol(col);
                nextPiece.setRow(row);
                gridPane.add(nextPiece.getPiece(), col, row);
                imageViewMap.put(typeColor, nextPiece.getPiece());
                col++;

            }

        }

    }

    private Piece createPiece(String type, String color) {

        type = type.replaceAll("\\d", "");


        return switch (type) {
            case "king" -> new King(color);
            case "queen" -> new Queen(color);
            case "knight" -> new Knight(color);
            case "bishop" -> new Bishop(color);
            case "rook" -> new Rook(color);
            case "pawn" -> new Pawn(color);
            default -> null;
        };
    }

    private void handleCastling(int startCol, int startRow, int endCol, int endRow) {
        boolean isKingSide = endCol > startCol;
        int rookStartCol = isKingSide ? 7 : 0;
        int rookEndCol = isKingSide ? endCol - 1 : endCol + 1;

        // Move rook
        String rookPiece = boardCurrent.get(rookStartCol, startRow);
        ImageView rookView = imageViewMap.get(rookPiece);
        gridPane.getChildren().remove(rookView);
        gridPane.add(rookView, rookEndCol, startRow);
        boardCurrent.set(rookStartCol, startRow,"null");
        boardCurrent.set(rookEndCol, startRow, rookPiece);

        // Update rook's position and moved status
        Rook rook = (Rook) pieces.get(rookPiece);
        rook.handleCastlingMove(rookEndCol);
    }

    private void initializeGame() {

        whiteKing = (King) pieces.get("king1white");
        blackKing = (King) pieces.get("king1black");
        calculateThreatenedSquares();
    }

    public boolean simulateMoveProtectKing(Piece piece, int endCol, int endRow) {
        int startCol = piece.getCol();
        int startRow = piece.getRow();

        //Store the original state
        String movingPiece = boardCurrent.get(startCol, startRow);
        String capturedPiece = boardCurrent.get(endCol, endRow);

        //Simulate  move
        boardCurrent.set(endCol, endRow, movingPiece);
        boardCurrent.set(startCol, startRow,"null");
        piece.setCol(endCol);
        piece.setRow(endRow);

        calculateThreatenedSquares();

        //Check for check
        boolean kingInCheck = (isWhiteTurn ? whiteKing : blackKing).isInCheck(
                isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite);

        //Revert move
        boardCurrent.set(startCol, startRow, movingPiece);
        boardCurrent.set(endCol, endRow, capturedPiece);
        piece.setCol(startCol);
        piece.setRow(startRow);


        calculateThreatenedSquares();

        return !kingInCheck && piece.isValidMove(endCol, endRow, boardCurrent.getBoard(),
                isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite);
    }

    private boolean isCheckmate() {
        King currentKing = isWhiteTurn ? whiteKing : blackKing;
        if (!currentKing.isInCheck(isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite)) {
            return false;
        }

        //Check all possible moves for all pieces
        for (Piece piece : pieces.values()) {
            if (piece.getColor().equals(isWhiteTurn ? "white" : "black")) {
                for (int col = 0; col < BOARD_SIZE; col++) {
                    for (int row = 0; row < BOARD_SIZE; row++) {
                        if (simulateMoveProtectKing(piece, col, row)) {
                            return false; //found move, not checkmate
                        }
                    }
                }
            }
        }
        return true; //it's checkmate
    }

    public static void main(String[] args) {
        launch(args);
    }
}