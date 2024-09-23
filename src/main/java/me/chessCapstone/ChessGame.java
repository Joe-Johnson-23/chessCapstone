
package me.chessCapstone;

import javafx.application.Application;
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





    @Override
    public void start(Stage primaryStage) {
        GridPane gridPane = new GridPane();
        setupBoard(gridPane);
        setUpPieces(gridPane);


        selectedPiece.setOnMousePressed(event -> {
            mouseX = event.getSceneX() - selectedPiece.getLayoutX();
            mouseY = event.getSceneY() - selectedPiece.getLayoutY();
            int col = (int) (event.getSceneX() / TILE_SIZE);
            int row = (int) (event.getSceneY() / TILE_SIZE);
            initialPieceCoordinateCOL = col;
            initialPieceCoordinateROW = row;
        });

//        selectedPiece.setOnMouseDragged(event -> {
//            double offsetX = event.getSceneX() - mouseX;
//            double offsetY = event.getSceneY() - mouseY;
//            selectedPiece.setLayoutX(mouseX + offsetX);
//            selectedPiece.setLayoutY(mouseY + offsetY);
//        });

        selectedPiece.setOnMouseReleased(event -> {
            if (selectedPiece != null) {
                // Snap the piece to the nearest tile
                int col = (int) (event.getSceneX() / TILE_SIZE);
                int row = (int) (event.getSceneY() / TILE_SIZE);

                if (col >= 0 && col < BOARD_SIZE && row >= 0 && row < BOARD_SIZE) {
                    gridPane.getChildren().remove(selectedPiece);
                    gridPane.add(selectedPiece, col, row);
                }
                String temp = boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW];
                boardCurrent[initialPieceCoordinateCOL][initialPieceCoordinateROW] = "null";
                boardCurrent[col][row] = temp ;


                initialPieceCoordinateROW = 0;
                initialPieceCoordinateCOL = 0;

                //selectedPiece = null; // Reset selected piece
            }

            System.out.println("NEW BOARD");
            for(int i = 0; i < BOARD_SIZE; i++) {

                for(int j = 0; j < BOARD_SIZE; j++) {
                    System.out.println("col" + i +"  row: " + j + " = " + boardCurrent[i][j]);
                }
            }

        });



        for(int i = 0; i < BOARD_SIZE; i++) {

            for(int j = 0; j < BOARD_SIZE; j++) {
                System.out.println("col" + i +"  row: " + j + " = " + boardCurrent[i][j]);
            }
        }



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