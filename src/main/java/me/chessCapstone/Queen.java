package me.chessCapstone;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;

public class Queen extends Piece {


    ImageView piece;

    public Queen(String type, String color) {
        super(type, color);
    }

    public ImageView addQueen(String color){
        if(color.equals("black")){
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/pngPiece/black-queen.png")));
            piece = new ImageView(image);
            piece.setFitWidth(100);  // Set the size of the piece
            piece.setFitHeight(100);
            return piece;
        } else {
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/pngPiece/white-queen.png")));
            piece = new ImageView(image);
            piece.setFitWidth(100);  // Set the size of the piece
            piece.setFitHeight(100);
            return piece;

        }


    }


}
