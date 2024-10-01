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


}