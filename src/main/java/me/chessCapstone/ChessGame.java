
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

    HashMap<String, Piece> pieces = new HashMap<>();
    private StackPane[][] stiles = new StackPane[BOARD_SIZE][BOARD_SIZE];
    private String[][] boardCurrent = new String[BOARD_SIZE][BOARD_SIZE];
    private ArrayList<Tile> squaresThreatenedByWhite = new ArrayList<>();
    private ArrayList<Tile> squaresThreatenedByBlack = new ArrayList<>();


    private int initialPieceCoordinateROW;
    private int initialPieceCoordinateCOL;

    private ImageView selectedPiece = null;

    private GridPane gridPane;

    //variable to add new piece upon promotion, so that board state has unique value
    //if not 'queenwhite' will be made upon promotion instead of "queen3white"
    //problem occurs if multiple pawns promote to queen (ie, "queenwhite" then "queenwhite")
    //thus need global for now
    int countForPromotion = 2;
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

            calculateThreatenedSquares();

            int col = (int) (event.getSceneX() / TILE_SIZE);
            int row = (int) (event.getSceneY() / TILE_SIZE);
            initialPieceCoordinateCOL = col;
            initialPieceCoordinateROW = row;
            if(selectedPiece == null) {
                selectedPiece = imageViewMap.get(boardCurrent[col][row]);

                String typeOfPiece =  boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
//              highlightValidMoves(col, row, typeOfPiece);
                Piece piece =  pieces.get(boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW]);
                if(piece != null) {
                    if(piece.getColor().equals("white")) {
                        piece.highlightValidMoves(col, row, stiles, boardCurrent, squaresThreatenedByBlack);
                    } else {
                        piece.highlightValidMoves(col, row, stiles, boardCurrent, squaresThreatenedByWhite);
                    }
                }

            }

        });

        gridPane.setOnMouseReleased(event -> {
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
                    String typeOfPiece = boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                    String pieceType = typeOfPiece.replaceAll("\\d", ""); // rmove digit
                    pieceType = pieceType.replace("Promoted", "");        // remove "Promoted"

                    String color = "";
                    if (pieceType.endsWith("white")) {
                        pieceType = pieceType.substring(0, pieceType.length() - 5); // remove "white"
                        color = "white";
                    } else if (pieceType.endsWith("black")) {
                        pieceType = pieceType.substring(0, pieceType.length() - 5); // remove "black"
                        color = "black";
                    }

                    boolean isValidMove = false;
                    //match the position between a non-digit and a digit without consuming any characters.
//                    switch (typeOfPiece.split("(?<=\\D)(?=\\d)")[0]) {
//                    switch (pieceType) {
//                        case "queen":
//                            isValidMove = isValidQueenMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
//                            break;
//                        case "bishop":
//                            isValidMove = isValidBishopMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
//                            break;
//                        case "knight":
//                            isValidMove = isValidKnightMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
//                            break;
//                        case "pawn":
//                            isValidMove = isValidPawnMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
//                            break;
//                        case "rook":
//                            isValidMove = isValidRookMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
//                            break;
//                        case "king":
//                            isValidMove = isValidKingMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
//                            break;
//                        // Add cases for other piece types as needed
//                    }

                    Piece piece = pieces.get(boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW]);

                    boolean validMove;

                    if(piece.getColor().equals("white")) {
                        validMove = piece.isValidMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent, squaresThreatenedByBlack);
                    } else {
                        validMove = piece.isValidMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent, squaresThreatenedByWhite);
                    }

                    System.out.println(piece);
                    if (validMove) {

                        // move
                        gridPane.getChildren().remove(selectedPiece);
                        gridPane.add(selectedPiece, col, row);

                        // update boardCurrent
                        String currentPiece = boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                        String destinationPiece = boardCurrent[col][row];

                        //to capture piece.
                        String toBeRemoved = boardCurrent[col][row];
                        if (!toBeRemoved.equals("null")) {
                            ImageView capturedPiece = imageViewMap.get(destinationPiece);
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
                selectedPiece = null;


                // print the current board
                printBoardState();
                initialPieceCoordinateROW = 0;
                initialPieceCoordinateCOL = 0;

                selectedPiece = null; // Reset selected piece

            }

        });



        Scene scene = new Scene(gridPane, TILE_SIZE * BOARD_SIZE, TILE_SIZE * BOARD_SIZE);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Chess Game");
        primaryStage.show();
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

            countForPromotion = countForPromotion + 1;
            // create new piece
            String newPieceType = choice.toLowerCase();
            String newPieceName = newPieceType + countForPromotion+ (isWhite ? "white" : "black");
            String newColor = (isWhite ? "white" : "black");
            Piece promotedPiece = createPiece(newPieceType, isWhite ? "white" : "black");
            ImageView promotedPieceView = promotedPiece.getPiece();


            //FIXED problem

            pieces.put(promotedPiece.getType() +countForPromotion+ newColor, promotedPiece);


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

        for (String color : colors) {

            for (int y = 0; y < pieceList.length; y++) {

                if (col == 8) {
                    if (row == 1) {
                        row = 7;
                        pieceList[3] = "queen1";
                        pieceList[4] = "king1";
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
                boardCurrent[col][row] = typeColor;
                assert nextPiece != null;
                gridPane.add(nextPiece.getPiece(), col, row);
                imageViewMap.put(typeColor, nextPiece.getPiece());
                col++;

            }

        }

    }

    private Piece createPiece(String type, String color) {
//         remvoe "Promoted"
        type = type.replaceAll("\\d", "");

//        if(type.substring(type.length() - 1).matches("\\d")) {
//            type = type.substring(0, type.length() - 1);
//        }

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

//    private void highlightValidMoves(int startCol, int startRow, String typeOfPiece) {
//        String pieceType = typeOfPiece.replaceAll("\\d", ""); // remove digit
//
//        String color = "";
//        if (pieceType.endsWith("white")) {
//            pieceType = pieceType.substring(0, pieceType.length() - 5); // remove "white"
//            color = "white";
//        } else if (pieceType.endsWith("black")) {
//            pieceType = pieceType.substring(0, pieceType.length() - 5); //  remove "black"
//            color = "black";
//        }
//
//        for (int row = 0; row < BOARD_SIZE; row++) {
//            for (int col = 0; col < BOARD_SIZE; col++) {
//
//                //changed typeOfPiece to  pieceType
//                if (pieceType.contains("queen") && isValidQueenMove(startCol, startRow, col, row, boardCurrent)) {
//                    stiles[row][col].setStyle("-fx-background-color: LIMEGREEN;");
//                }
//                if (pieceType.contains("bishop") && isValidBishopMove(startCol, startRow, col, row, boardCurrent)) {
//                    stiles[row][col].setStyle("-fx-background-color: RED;");
//                }
//                if (pieceType.contains("knight") && isValidKnightMove(startCol, startRow, col, row, boardCurrent)) {
//                    stiles[row][col].setStyle("-fx-background-color: BLUE;");
//                }
//                if (pieceType.contains("pawn") && isValidPawnMove(startCol, startRow, col, row, boardCurrent)) {
//                    stiles[row][col].setStyle("-fx-background-color: PURPLE;");
//                }
//                if (pieceType.contains("rook") && isValidRookMove(startCol, startRow, col, row, boardCurrent)) {
//                    stiles[row][col].setStyle("-fx-background-color: YELLOW;");
//                }
//                if (pieceType.contains("king") && isValidKingMove(startCol, startRow, col, row, boardCurrent)) {
//                    stiles[row][col].setStyle("-fx-background-color: PINK;");
//                }
//            }
//        }
//    }

    private void calculateThreatenedSquares() {

        squaresThreatenedByWhite.clear();
        squaresThreatenedByBlack.clear();

        for(int row = 0; row < BOARD_SIZE; row++) {
            for(int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = pieces.get(boardCurrent[col][row]);
                if(piece != null) {
                    if(piece.getColor().equals("white")) {
                        squaresThreatenedByWhite.addAll(piece.findThreatenedSquares(col, row, boardCurrent));
                        Set<Tile> set = new HashSet<>(squaresThreatenedByWhite);
                        squaresThreatenedByWhite = new ArrayList<>(set);
                    } else {
                        squaresThreatenedByBlack.addAll(piece.findThreatenedSquares(col, row, boardCurrent));
                        Set<Tile> set = new HashSet<>(squaresThreatenedByBlack);
                        squaresThreatenedByBlack = new ArrayList<>(set);
                    }
                }
            }
        }

        System.out.println("Threatened squares by White:" + squaresThreatenedByWhite.size());
    }

    private void printBoardState() {
        System.out.println("NEW BOARD");
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                System.out.println("col " + col + " row " + row + " = " + boardCurrent[col][row]);
            }
        }
//        System.out.println(imageViewMap);
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