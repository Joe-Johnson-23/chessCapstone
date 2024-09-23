package me.demo1;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImageViewWithCoordinates {
    private final ImageView imageView;
    private double x;
    private double y;

    //NOT FUNCTIONAL , NOT BEING USED, but probably should be integrated at some point
    public ImageViewWithCoordinates(ImageView img, double x, double y) {
        this.imageView = img;
        this.x = x;
        this.y = y;
        updatePosition();
    }

    public ImageView getImageView() {
        return imageView;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
        updatePosition();
    }

    public void setY(double y) {
        this.y = y;
        updatePosition();
    }

    private void updatePosition() {
        imageView.setX(x);
        imageView.setY(y);
    }
}
