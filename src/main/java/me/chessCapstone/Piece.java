package me.chessCapstone;

import javafx.scene.Node;

public class Piece extends Node {

    protected String type; // e.g., "Pawn", "Knight", etc.
    protected String color; // e.g., "White", "Black"

    public Piece(String type, String color) {
        this.type = type;
        this.color = color;
    }




}
