
package me.chessCapstone;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.geometry.Point2D;



import java.util.HashMap;
import java.util.Map;

public class ChessGame extends Application {
    private static final int TILE_SIZE = 100;
    private static final int BOARD_SIZE = 8;

    private StackPane[][] stiles = new StackPane[BOARD_SIZE][BOARD_SIZE];
    private String[][] boardCurrent = new String[BOARD_SIZE][BOARD_SIZE];


    private int initialPieceCoordinateROW;
    private int initialPieceCoordinateCOL;



    ImageView queenBlack = new Queen("black").getPiece();
    ImageView queenWhite = new Queen("white").getPiece();
    ImageView bishopBlackLeft = new Bishop("black").getPiece();
    ImageView bishopBlackRight = new Bishop("black").getPiece();
    ImageView bishopWhiteLeft = new Bishop("white").getPiece();
    ImageView bishopWhiteRight = new Bishop("white").getPiece();

    ImageView knightBlackLeft = new Knight("black").getPiece();
    ImageView knightBlackRight = new Knight("black").getPiece();
    ImageView knightWhiteLeft = new Knight("white").getPiece();
    ImageView knightWhiteRight = new Knight("white").getPiece();




    private ImageView selectedPiece = null;

    private Map<String, ImageView> imageViewMap = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        GridPane gridPane = new GridPane();
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

                selectedPiece = imageViewMap.get(boardCurrent[col][row]);


                double offsetX = event.getSceneX() - selectedPiece.getLayoutX();
                double offsetY = event.getSceneY() - selectedPiece.getLayoutY();


                //to be able to get type of piece pressed
                String[] typeColor =  boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW].split("_");
                highlightValidMoves(col, row, typeColor[0]);
                if(selectedPiece != null) {

                    selectedPiece.setOnMouseDragged(event2 -> {

                        System.out.println("dragging mouse...");
                        selectedPiece.setLayoutX(event2.getSceneX() - offsetX);
                        selectedPiece.setLayoutY(event2.getSceneY() - offsetY);


                    });

                }

            }

        });

        gridPane.setOnMouseReleased(event -> {
            if (selectedPiece != null) {

                selectedPiece.setLayoutX(0);
                selectedPiece.setLayoutY(0);

                resetTileColor();

                // 마우스 좌표를 GridPane의 로컬 좌표로 변환
                Point2D localPoint = gridPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                double x = localPoint.getX();
                double y = localPoint.getY();

                int col = (int) (x / TILE_SIZE);
                int row = (int) (y / TILE_SIZE);

                if (col >= 0 && col < BOARD_SIZE && row >= 0 && row < BOARD_SIZE) {
                    // 이동 가능 여부 체크
                    String[] typeColor =  boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW].split("_");
                    //this Piece is to be able to call isValidQueenMove
                    Piece piece = new Piece(typeColor[0], typeColor[1]);
                    if (typeColor[0].equals("queen")  && piece.isValidQueenMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent)) {

                        // 이동
                        gridPane.getChildren().remove(selectedPiece);
                        gridPane.add(selectedPiece, col, row);

                        // boardCurrent 업데이트
                        String temp = boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                        boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW] = "null";
                        boardCurrent[col][row] = temp;
                    } else if (typeColor[0].equals("bishop")  && piece.isValidBishopMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent)) {
                        // 이동
                        gridPane.getChildren().remove(selectedPiece);
                        gridPane.add(selectedPiece, col, row);

                        // boardCurrent 업데이트
                        String temp = boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                        boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW] = "null";
                        boardCurrent[col][row] = temp;
                    }else if (typeColor[0].equals("knight")  && piece.isValidKnightMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent)) {

                        // 이동
                        gridPane.getChildren().remove(selectedPiece);
                        gridPane.add(selectedPiece, col, row);

                        // boardCurrent 업데이트
                        String temp = boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                        boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW] = "null";
                        boardCurrent[col][row] = temp;

                    } else if (typeColor[0].equals("bishop")  && piece.isValidBishopMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent)) {
                        // 이동
                        gridPane.getChildren().remove(selectedPiece);
                        gridPane.add(selectedPiece, col, row);

                        // boardCurrent 업데이트
                        String temp = boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                        boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW] = "null";
                        boardCurrent[col][row] = temp;
                    }else if (typeColor[0].equals("knight")  && piece.isValidKnightMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent)) {
                        // 이동
                        gridPane.getChildren().remove(selectedPiece);
                        gridPane.add(selectedPiece, col, row);

                        // boardCurrent 업데이트
                        String temp = boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                        boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW] = "null";
                        boardCurrent[col][row] = temp;

                    }
                    else {
                        // 이동 불가능한 경우 원래 위치로 돌아감
                        gridPane.getChildren().remove(selectedPiece);
                        gridPane.add(selectedPiece, initialPieceCoordinateCOL, initialPieceCoordinateROW);
                    }
                } else {
                    // 체스판 밖으로 나간 경우 원래 위치로 돌아감
                    gridPane.getChildren().remove(selectedPiece);
                    gridPane.add(selectedPiece, initialPieceCoordinateCOL, initialPieceCoordinateROW);
                }

                // 초기 좌표 리셋
//                initialPieceCoordinateROW = -1;
//                initialPieceCoordinateCOL = -1;
//                selectedPiece = null;


                // 체스판 상태 출력
                printBoardState();
                initialPieceCoordinateROW = 0;
                initialPieceCoordinateCOL = 0;

                selectedPiece = null; // Reset selected piece

            }

            //Console log to show current board of pieces.
            System.out.println("NEW BOARD");
            for(int i = 0; i < BOARD_SIZE; i++) {

                for(int j = 0; j < BOARD_SIZE; j++) {
                    System.out.println("col" + i +"  row: " + j + " = " + boardCurrent[i][j]);
                }
            }

        });


        //Console log to show current board of pieces.
        for(int i = 0; i < BOARD_SIZE; i++) {

            for(int j = 0; j < BOARD_SIZE; j++) {
                System.out.println("col" + i +"  row: " + j + " = " + boardCurrent[i][j]);
            }
        }


//        // 검은색 퀸 이벤트 핸들러 설정
//        setPieceEventHandlers(queenBlack, gridPane);

        Scene scene = new Scene(gridPane, TILE_SIZE * BOARD_SIZE, TILE_SIZE * BOARD_SIZE);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Chess Game");
        primaryStage.show();
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

        //HashMap<String, Piece> pieces = new HashMap<>();

        //both queens
        boardCurrent[3][0] = "queen_black";
        gridPane.add(queenBlack, 3, 0);
        boardCurrent[3][7] = "queen_white";
        gridPane.add(queenWhite, 3, 7);
        imageViewMap.put("queen_black",queenBlack);
        imageViewMap.put("queen_white",queenWhite);



        //black bishops
        boardCurrent[2][0] = "bishop_black_left";
        gridPane.add(bishopBlackLeft, 2, 0);
        imageViewMap.put("bishop_black_left",bishopBlackLeft);
        boardCurrent[5][0] = "bishop_black_right";
        gridPane.add(bishopBlackRight, 5, 0);
        imageViewMap.put("bishop_black_right",bishopBlackRight);

        //white bishops
        boardCurrent[2][7] = "bishop_white_left";
        gridPane.add(bishopWhiteLeft, 2, 7);
        imageViewMap.put("bishop_white_left",bishopWhiteLeft);
        boardCurrent[5][7] = "bishop_white_right";
        gridPane.add(bishopWhiteRight, 5, 7);
        imageViewMap.put("bishop_white_right",bishopWhiteRight);

        //black knights
        boardCurrent[1][0] = "knight_black_left";
        gridPane.add(knightBlackLeft, 1, 0);
        imageViewMap.put("knight_black_left",knightBlackLeft);
        boardCurrent[6][0] = "knight_black_right";
        gridPane.add(knightBlackRight, 6, 0);
        imageViewMap.put("knight_black_right",knightBlackRight);

        //white knights
        boardCurrent[1][7] = "knight_white_left";
        gridPane.add(knightWhiteLeft, 1, 7);
        imageViewMap.put("knight_white_left",knightWhiteLeft);
        boardCurrent[6][7] = "knight_white_right";
        gridPane.add(knightWhiteRight, 6, 7);
        imageViewMap.put("knight_white_right",knightWhiteRight);

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

    private void highlightValidMoves(int startCol, int startRow, String typeOfPiece) {
        String[] typeColor =  boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW].split("_");
        Piece piece = new Piece(typeColor[0], typeColor[1]);
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {

                if (typeOfPiece.equals("queen") && piece.isValidQueenMove(startCol, startRow, col, row, boardCurrent)) {
                    stiles[row][col].setStyle("-fx-background-color: LIMEGREEN;");
                }
                if (typeOfPiece.equals("bishop") && piece.isValidBishopMove(startCol, startRow, col, row, boardCurrent)) {
                    stiles[row][col].setStyle("-fx-background-color: RED;");
                }
                if (typeOfPiece.equals("knight") && piece.isValidKnightMove(startCol, startRow, col, row, boardCurrent)) {
                    stiles[row][col].setStyle("-fx-background-color: BLUE;");
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