
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

                String typeOfPiece =  boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                highlightValidMoves(col, row, typeOfPiece);
//                if(selectedPiece != null) {
//
//                    selectedPiece.setOnMouseDragged(event2 -> {
//
//                        System.out.println("dragging mouse...");
//                        selectedPiece.setLayoutX(event2.getSceneX() - offsetX);
//                        selectedPiece.setLayoutY(event2.getSceneY() - offsetY);
//
//
//                    });
//
//                }

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
                    String typeOfPiece = boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                    boolean isValidMove = false;
                    //match the position between a non-digit and a digit without consuming any characters.
                    switch (typeOfPiece.split("(?<=\\D)(?=\\d)")[0]) {
                        case "queen":
                            isValidMove = isValidQueenMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
                            break;
                        case "bishop":
                            isValidMove = isValidBishopMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
                            break;
                        case "knight":
                            isValidMove = isValidKnightMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
                            break;
                        case "pawn":
                            isValidMove = isValidPawnMove(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row, boardCurrent);
                            break;
                        // Add cases for other piece types as needed
                    }

                    if (isValidMove) {

                        // 이동
                        gridPane.getChildren().remove(selectedPiece);
                        gridPane.add(selectedPiece, col, row);

                        // boardCurrent 업데이트
                        String temp = boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                        boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW] = "null";
                        boardCurrent[col][row] = temp;
                    }else {
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
                //pieces.put(typeColor, nextPiece);
                boardCurrent[col][row] = typeColor;
                gridPane.add(nextPiece.getPiece(), col, row);
                imageViewMap.put(typeColor, nextPiece.getPiece());
                col++;

            }

        }

    }

    private Piece createPiece(String type, String color) {

        if(type.substring(type.length() - 1).matches("\\d")) {
            type = type.substring(0, type.length() - 1);
        }

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

    private void highlightValidMoves(int startCol, int startRow, String typeOfPiece) {
        String[] typeColor =  boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW].split("_");

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {

                if (typeOfPiece.contains("queen") && isValidQueenMove(startCol, startRow, col, row, boardCurrent)) {
                    stiles[row][col].setStyle("-fx-background-color: LIMEGREEN;");
                }
                if (typeOfPiece.contains("bishop") && isValidBishopMove(startCol, startRow, col, row, boardCurrent)) {
                    stiles[row][col].setStyle("-fx-background-color: RED;");
                }
                if (typeOfPiece.contains("knight") && isValidKnightMove(startCol, startRow, col, row, boardCurrent)) {
                    stiles[row][col].setStyle("-fx-background-color: BLUE;");
                }
                if (typeOfPiece.contains("pawn") && isValidPawnMove(startCol, startRow, col, row, boardCurrent)) {
                    stiles[row][col].setStyle("-fx-background-color: PURPLE;");
                }
            }
        }
    }


    public boolean isValidQueenMove(int startCol, int startRow, int endCol, int endRow, String[][] boardCurrent) {
        int colDiff = Math.abs(endCol - startCol);
        int rowDiff = Math.abs(endRow - startRow);

        // 수직 이동
        if (startCol == endCol && startRow != endRow) {
            return isPathClear(startCol, startRow, endCol, endRow, boardCurrent);
        }
        // 수평 이동
        else if (startRow == endRow && startCol != endCol) {
            return isPathClear(startCol, startRow, endCol, endRow, boardCurrent);
        }
        // 대각선 이동
        else if (colDiff == rowDiff) {
            return isPathClear(startCol, startRow, endCol, endRow, boardCurrent);
        }

        return false;
    }

    public boolean isValidBishopMove(int startCol, int startRow, int endCol, int endRow, String[][] boardCurrent) {
        int colDiff = Math.abs(endCol - startCol);
        int rowDiff = Math.abs(endRow - startRow);

        if (colDiff == rowDiff) {
            return isPathClear(startCol, startRow, endCol, endRow, boardCurrent);
        }

        return false;
    }

    public boolean isValidKnightMove(int startCol, int startRow, int endCol, int endRow, String[][] boardCurrent) {
        int colDiff = Math.abs(endCol - startCol);
        int rowDiff = Math.abs(endRow - startRow);

        // Knight moves in an L-shape: 2 squares in one direction and 1 square perpendicular to that
        boolean isValidMove = (colDiff == 2 && rowDiff == 1) || (colDiff == 1 && rowDiff == 2);

        // For knights, we don't need to check if the path is clear because they can jump over other pieces
        // We only need to ensure the destination square is either empty or contains an opponent's piece
        if (isValidMove) {
            String destinationPiece = boardCurrent[endCol][endRow];
            String currentPiece = boardCurrent[startCol][startRow];

            // Check if the destination is empty or contains an opponent's piece
            return destinationPiece.equals("null") || (!destinationPiece.startsWith(currentPiece.split("_")[1]) && isPathClear(startCol, startRow, endCol, endRow, boardCurrent));
        }

        return false;
    }

    public boolean isValidPawnMove(int startCol, int startRow, int endCol, int endRow, String[][] boardCurrent) {


        // 수직 이동
        if (startCol == endCol && startRow != endRow) {
            return isPathClear(startCol, startRow, endCol, endRow, boardCurrent);
        }


        return false;
    }





    public boolean isPathClear(int startCol, int startRow, int endCol, int endRow, String[][] boardCurrent) {
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