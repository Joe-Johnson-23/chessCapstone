package me.chessCapstone;

//This class is meant to represent individual squares.
public class Tile {

    //Each Tile has it's own row and column.
    private int row, col;

    //Constructor for a Tile object. Requires two integer arguments for the column and row.
    public Tile(int col, int row) {
        this.col = col;
        this.row = row;
    }

    //Returns the column of the Tile.
    public int getCol() {
        return col;
    }

    //Set the column of the Tile.
    public void setCol(int col) {
        this.col = col;
    }

    //Return the row of the Tile.
    public int getRow() {
        return row;
    }

    //Set the row of the Tile.
    public void setRow(int row) {
        this.row = row;
    }

    @Override
    //Tiles are now considered equal if they share a row and column, not only a space in memory.
    public boolean equals(Object obj) {
        
        //Checks to see if they share a space in memory.
        if(this == obj) {
            return true;
        }

        //Checks to see if the passed object is not aninstance of the Tile class.
        if(!(obj instanceof Tile)) {
            return false;
        }

        //Object is downcasted into a Tile class
        Tile tile = (Tile) obj;

        //If the row and column of both Tiles are the same, they are equal and true is returned.
        return this.row == tile.row && this.col == tile.col;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(row);
        result = 31 * result + Integer.hashCode(col);
        return result;
    }
}
