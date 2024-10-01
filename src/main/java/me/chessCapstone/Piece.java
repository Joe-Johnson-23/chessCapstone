package me.chessCapstone;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;

public class Piece extends Node {

    protected String type; // e.g., "Pawn", "Knight", etc.
    protected String color; // e.g., "White", "Black"
    protected ImageView piece;

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

    public void setPiece() {
        String path = "/pngPiece/" + getColor() + "-" + getType() + ".png";
        Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
        piece = new ImageView(image);
        piece.setFitWidth(100);
        piece.setFitHeight(100);
    }



}