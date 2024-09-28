
package me.chessCapstone;

import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class ChessGame extends Application {
    private static final int TILE_SIZE = 100;
    private static final int BOARD_SIZE = 8;

    private StackPane[][] stiles = new StackPane[BOARD_SIZE][BOARD_SIZE];
    private String[][] boardCurrent = new String[BOARD_SIZE][BOARD_SIZE];


    private double mouseX;
    private double mouseY;

    private int initialPieceCoordinateROW;
    private int initialPieceCoordinateCOL;


    ImageView queenBlack = new Queen("black","queen").addQueen("black");
    ImageView queenWhite = new Queen("white","queen").addQueen("white");
    private ImageView selectedPiece = queenBlack;

    //may help with implementing a 'generic' selected piece... not being using as of now
    private Map<String, ImageView> imageViewMap = new HashMap<>();



    // 내가 고친.
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

        // 검은색 퀸 이벤트 핸들러 설정
        setPieceEventHandlers(queenBlack, gridPane);

        Scene scene = new Scene(gridPane, TILE_SIZE * BOARD_SIZE, TILE_SIZE * BOARD_SIZE);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Chess Game");
        primaryStage.show();

        printBoardState();
    }


    private void setPieceEventHandlers(ImageView piece, GridPane gridPane) {
        piece.setOnMousePressed(event -> {
            selectedPiece = piece;
            mouseX = event.getSceneX() - piece.getLayoutX();
            mouseY = event.getSceneY() - piece.getLayoutY();
            int col = GridPane.getColumnIndex(piece);
            int row = GridPane.getRowIndex(piece);
            initialPieceCoordinateCOL = col;
            initialPieceCoordinateROW = row;

            highlightValidMoves(col, row);
        });

//        piece.setOnMouseDragged(event -> {
//            double offsetX = event.getSceneX() - mouseX;
//            double offsetY = event.getSceneY() - mouseY;
//            piece.setLayoutX(offsetX);
//            piece.setLayoutY(offsetY);
//        });

        piece.setOnMouseReleased(event -> {
            if (selectedPiece != null) {
                // 말의 Layout 위치를 원래대로 초기화
                piece.setLayoutX(0);
                piece.setLayoutY(0);

                resetTileColor();

                // 마우스 좌표를 GridPane의 로컬 좌표로 변환
                Point2D localPoint = gridPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                double x = localPoint.getX();
                double y = localPoint.getY();

                int col = (int) (x / TILE_SIZE);
                int row = (int) (y / TILE_SIZE);

                if (col >= 0 && col < BOARD_SIZE && row >= 0 && row < BOARD_SIZE) {
                    // 이동 가능 여부 체크
                    if (isValidQueenMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row)) {
                        // 이동
                        gridPane.getChildren().remove(selectedPiece);
                        gridPane.add(selectedPiece, col, row);

                        // boardCurrent 업데이트
                        String temp = boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                        boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW] = "null";
                        boardCurrent[col][row] = temp;
                    } else {
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
                initialPieceCoordinateROW = -1;
                initialPieceCoordinateCOL = -1;
                selectedPiece = null;

                // 체스판 상태 출력
                printBoardState();
            }
        });
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

    private void highlightValidMoves(int startCol, int startRow) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (isValidQueenMove(startCol, startRow, col, row)) {
                    stiles[row][col].setStyle("-fx-background-color: LIMEGREEN;");
                }
            }
        }
    }


    private void setupBoard(GridPane gridPane) {

        for (int row = 0; row < BOARD_SIZE; row++) {

            for (int col = 0; col < BOARD_SIZE; col++) {


                StackPane stile = new StackPane();
                stile.setPrefWidth(200);
                stile.setPrefHeight(200);

                stile.setStyle((row + col) % 2 == 0 ? "-fx-background-color:WHITE": "-fx-background-color:GRAY");

                stiles[row][col] = stile;

                gridPane.add(stile, col, row);

            }
        }
        System.out.println();

    }
    private void setUpPieces(GridPane gridPane) {



        for (int row = 0; row < BOARD_SIZE; row++) {

            for (int col = 0; col < BOARD_SIZE; col++) {

                if(row == 0 && col == 3){

                    boardCurrent[col][row] = "queenBlack";
                        gridPane.add(queenBlack, col, row);

                }
                if(row == 7 && col == 3){

                    boardCurrent[col][row] = "queenWhite";
                    gridPane.add(queenWhite, col, row);

                }
            }

            }

        imageViewMap.put("queenBlack",queenBlack);
        imageViewMap.put("queenWhite",queenWhite);


    }


    private boolean isValidQueenMove(int startCol, int startRow, int endCol, int endRow) {
        int colDiff = Math.abs(endCol - startCol);
        int rowDiff = Math.abs(endRow - startRow);

        // 수직 이동
        if (startCol == endCol && startRow != endRow) {
            return isPathClear(startCol, startRow, endCol, endRow);
        }
        // 수평 이동
        else if (startRow == endRow && startCol != endCol) {
            return isPathClear(startCol, startRow, endCol, endRow);
        }
        // 대각선 이동
        else if (colDiff == rowDiff) {
            return isPathClear(startCol, startRow, endCol, endRow);
        }

        return false;
    }


    private boolean isPathClear(int startCol, int startRow, int endCol, int endRow) {
        int colDirection = Integer.compare(endCol, startCol);
        int rowDirection = Integer.compare(endRow, startRow);

        int currentCol = startCol + colDirection;
        int currentRow = startRow + rowDirection;

        while (currentCol != endCol || currentRow != endRow) {
            if (!"null".equals(boardCurrent[currentCol][currentRow])) {
                return false; // 경로에 다른 말이 있음
            }
            currentCol += colDirection;
            currentRow += rowDirection;
        }

        // 목적지에 아군 말이 있는지 확인
        if (!"null".equals(boardCurrent[endCol][endRow])) {
            return false;
        }

        return true;
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