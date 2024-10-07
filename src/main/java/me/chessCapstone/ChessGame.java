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
    private StackPane[][] stiles = new StackPane[BOARD_SIZE][BOARD_SIZE];
    private String[][] boardCurrent = new String[BOARD_SIZE][BOARD_SIZE];


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

    static boolean globalEnPassant = false;

    /////////////////////////////// GLOBAL VARIABLES ///////////////////////////////
    private Map<String, ImageView> imageViewMap = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        gridPane = new GridPane();
        setupBoard(gridPane);
        setUpPieces(gridPane);


        // 배열 초기화
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (boardCurrent[col][row] == null) {
                    boardCurrent[col][row] = "null";
                }
            }
        }


        gridPane.setOnMousePressed(event -> {

            int col = (int) (event.getSceneX() / TILE_SIZE);
            int row = (int) (event.getSceneY() / TILE_SIZE);
            initialPieceCoordinateCOL = col;
            initialPieceCoordinateROW = row;
            if(selectedPiece == null) {


                String typeOfPiece =  boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                Piece piece =  pieces.get(boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW]);





                if ((isWhiteTurn && typeOfPiece.contains("white")) || (!isWhiteTurn && typeOfPiece.contains("black"))) {
                    selectedPiece = imageViewMap.get(boardCurrent[col][row]);
                    piece.highlightValidMoves(col, row, stiles, boardCurrent, typeOfPiece);
                }





            }

        });



        gridPane.setOnMouseReleased(event -> {

            String typeOfPiece = boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
            //checking whose turn, in case of wrong click
            if ((isWhiteTurn && typeOfPiece.contains("white")) || (!isWhiteTurn && typeOfPiece.contains("black"))) {

                if (selectedPiece != null) {

                    selectedPiece.setLayoutX(0);
                    selectedPiece.setLayoutY(0);

                    resetTileColor();

                    // convert mouse coordinates to local Gridpane coordinates
                    Point2D localPoint = gridPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                    double x = localPoint.getX();
                    double y = localPoint.getY();

                    int col = (int) (x / TILE_SIZE);
                    int row = (int) (y / TILE_SIZE);

                    if (col >= 0 && col < BOARD_SIZE && row >= 0 && row < BOARD_SIZE) {
                        // check if the move is valid

                        String pieceType = typeOfPiece.replaceAll("\\d", ""); // rmove digit


                        Piece piece = pieces.get(boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW]);

                        System.out.println(piece);
                        if (piece.isValidMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent)) {
                            // Check for castling
                            if (pieceType.contains("king") && Math.abs(col - initialPieceCoordinateCOL) == 2) {
                                if (isCastlingValid(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row)) {
                                    // Perform castling
                                    boolean isKingSide = col > initialPieceCoordinateCOL;
                                    int rookStartCol = isKingSide ? 7 : 0;
                                    int rookEndCol = isKingSide ? col - 1 : col + 1;

                                    // Move rook
                                    String rookPiece = boardCurrent[rookStartCol][row];
                                    ImageView rookView = imageViewMap.get(rookPiece);
                                    gridPane.getChildren().remove(rookView);
                                    gridPane.add(rookView, rookEndCol, row);
                                    boardCurrent[rookStartCol][row] = "null";
                                    boardCurrent[rookEndCol][row] = rookPiece;

                                    // Update rook's moved status
                                    pieces.get(rookPiece).setMoved(true);
                                } else {
                                    // Invalid castling, return king to original position
                                    gridPane.getChildren().remove(selectedPiece);
                                    gridPane.add(selectedPiece, initialPieceCoordinateCOL, initialPieceCoordinateROW);
                                    return;
                                }

                            }


                            //doesnt disallow multiple en passant captures with same pawn
                            //nor if the pawn has already captured.
                            //TODO: fix
                            if (globalEnPassant) {
                                // Determine the position of the pawn to be captured
                                int capturedPawnRow = typeOfPiece.contains("white") ? row + 1 : row - 1;
                                int capturedPawnCol = col;

                                // Remove the captured pawn from the board
                                String capturedPawn = boardCurrent[capturedPawnCol][capturedPawnRow];
                                boardCurrent[capturedPawnCol][capturedPawnRow] = "null";

                                // Remove the captured pawn's image from the GUI
                                gridPane.getChildren().remove(imageViewMap.get(capturedPawn));
                                imageViewMap.remove(capturedPawn);

                                globalEnPassant = false;
                            }



                            //check for king
                            String destinationPiece = boardCurrent[col][row];
                            if(destinationPiece.contains("king")){
                                initialPieceCoordinateROW = -1;
                                initialPieceCoordinateCOL = -1;
                                selectedPiece = null;
                                return;
                            }

                            // move from current position to new spot
                            gridPane.getChildren().remove(selectedPiece);
                            gridPane.add(selectedPiece, col, row);

                            // update boardCurrent
                            String currentPiece = boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                            // String destinationPiece = boardCurrent[col][row];

                            //to capture piece.
                            String toBeRemoved = boardCurrent[col][row];
                            if (!toBeRemoved.equals("null")) {
                                gridPane.getChildren().remove(imageViewMap.get(toBeRemoved));
                                imageViewMap.remove(toBeRemoved);
                            }

                            boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW] = "null";
                            boardCurrent[col][row] = currentPiece;

                            // check and handle pawn promotion
                            if (currentPiece.contains("pawn")) {
                                boolean isWhite = currentPiece.contains("white");
                                if ((isWhite && row == 0) || (!isWhite && row == 7)) {
                                    handlePawnPromotion(currentPiece, col, row, isWhite);
                                }
                            }

                            // Update piece's moved status
                            piece.setMoved(true);
                            switchTurn();

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
                    // print the current board
                    printBoardState();
                    selectedPiece = null; // Reset selected piece

                }


            }});



        Scene scene = new Scene(gridPane, TILE_SIZE * BOARD_SIZE, TILE_SIZE * BOARD_SIZE);
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


            //FIXED problem

            pieces.put(promotedPiece.getType() +globalCountForPromotion+ newColor, promotedPiece);


            // add new piece
            gridPane.add(promotedPieceView, col, row);
            boardCurrent[col][row] = newPieceName;
            imageViewMap.put(newPieceName, promotedPieceView);
        });
    }




    private void setupBoard(GridPane gridPane) {

        for (int row = 0; row < BOARD_SIZE; row++) {

            for (int col = 0; col < BOARD_SIZE; col++) {


                StackPane stile = new StackPane();
                stile.setPrefWidth(200);
                stile.setPrefHeight(200);

                stile.setStyle((row + col) % 2 == 0 ? "-fx-background-color:WHITE" : "-fx-background-color:GRAY");

                stiles[row][col] = stile;


                gridPane.add(stile, col, row);

            }
        }
        System.out.println();

    }

    private void setUpPieces(GridPane gridPane) {


        String[] pieceList = {"rook1", "knight1", "bishop1", "king1", "queen1", "bishop2", "knight2", "rook2",
                "pawn1", "pawn2", "pawn3", "pawn4", "pawn5", "pawn6", "pawn7", "pawn8"};

        String[] colors = {"black", "white"};

        int row = 0, col = 0;

        for(int x = 0; x < colors.length; x++) {

            for(int y = 0; y < pieceList.length; y++) {

                if(col == 8) {
                    if(row == 1) {
                        row = 7;
                        pieceList[3] = "queen1";
                        pieceList[4] = "king1";
                    } else if(row == 7) {
                        row--;
                    } else {
                        row++;
                    }
                    col = 0;
                }

                Piece nextPiece = createPiece(pieceList[y], colors[x]);
                String typeColor = pieceList[y] + colors[x];
                pieces.put(typeColor, nextPiece);
                boardCurrent[col][row] = typeColor;
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


    private void resetTileColor() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if ((row + col) % 2 == 0) {
                    stiles[row][col].setStyle("-fx-background-color: WHITE;");
                } else {
                    stiles[row][col].setStyle("-fx-background-color: GRAY;");
                }
            }
        }
    }



    private void printBoardState() {
        System.out.println("NEW BOARD");
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                System.out.println("col " + col + " row " + row + " = " + boardCurrent[col][row]);
            }
        }

    }

    private boolean hasKingMoved(String color) {
        String kingKey = "king1" + color;
        return pieces.get(kingKey).hasMoved();
    }

    private boolean hasRookMoved(String color, String side) {
        String rookKey = "rook" + (side.equals("kingside") ? "2" : "1") + color;
        return pieces.get(rookKey).hasMoved();
    }


    private boolean isCastlingValid(int startCol, int startRow, int endCol, int endRow) {
        String color = boardCurrent[startCol][startRow].contains("white") ? "white" : "black";
        boolean isKingSide = endCol > startCol;

        // Check if king and rook have moved
        if (hasKingMoved(color) || hasRookMoved(color, isKingSide ? "kingside" : "queenside")) {
            return false;
        }

        // Check if path is clear
        int direction = isKingSide ? 1 : -1;
        for (int col = startCol + direction; col != endCol; col += direction) {
            if (!boardCurrent[col][startRow].equals("null")) {
                return false;
            }
        }

        // TODO: Check if king passes through check

        return true;
    }




    public static void main(String[] args) {
        try {
            launch(args);
            System.exit(0);
        } catch (Exception error) {
            error.printStackTrace();
            System.exit(0);
        }
    }
}