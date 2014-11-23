package nl.liacs.huecolor;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.Random;

/**
 * Based on https://github.com/kpbird/Android-Image-Filters/blob/master/ImageEffect/src/main/java/com/kpbird/imageeffect/ImageFilters.java
 */
public class Filters {
    private Bitmap sourceBitmap;
    private int sourceWidth = 0;
    private int sourceHeight = 0;

    public Filters(Bitmap bitmap) {
        sourceBitmap = bitmap;
        sourceWidth = bitmap.getWidth();
        sourceHeight = bitmap.getHeight();
    }

    public Bitmap invert() {
        int[] pixels = new int[sourceWidth * sourceHeight];
        int red, green, blue, pixel;
        Bitmap inverted = Bitmap.createBitmap(sourceWidth, sourceHeight, sourceBitmap.getConfig());

        // Get the pixel array of the original bitmap.
        sourceBitmap.getPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);

        // Apply the invert effect.
        for(int i = 0; i < sourceWidth; i++) {
            for(int j = 0; j < sourceHeight; j++) {
                pixel = pixels[i + sourceWidth * j];
                red = 255 - (pixel >> 16) & 0xFF; // Red component
                green = 255 - (pixel >> 8) & 0xFF; // Green component
                blue = 255 - pixel & 0xFF; // Blue component
                pixels[i + sourceWidth * j] = Color.argb(pixel >>> 24, red, green, blue);
            }
        }
        inverted.setPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);
        return inverted;
    }

    public Bitmap grayscale() {
        // Create an output Bitmap with the settings of the original
        Bitmap grayScaled = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888);
        int alpha, red, green, blue, pixel;
        // Scan through the original Bitmap
        for(int i = 0; i < sourceWidth; i++) {
            for(int j = 0; j < sourceHeight; j++) {
                pixel = sourceBitmap.getPixel(i,j);
                alpha = Color.alpha(pixel);
                // Apply grayscale
                red = green = blue = (int) (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel));
                grayScaled.setPixel(i, j, Color.argb(alpha, red, green, blue));
            }
        }
        return grayScaled;
    }

    public Bitmap sepia() {
        // Create an output Bitmap with the settings of the original
        Bitmap sepiaApplied = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888);
        int alpha, red, green, blue, pixel;
        for(int i = 0; i < sourceWidth; i++) {
            for(int j = 0; j < sourceHeight; j++) {
                pixel = sourceBitmap.getPixel(i,j);
                alpha = Color.alpha(pixel);
                // Apply grayscale
                red = green = blue = (int) (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel));
                // Apply the sepia effect
                red += 15;
                green += 6;
                blue += 1;
                if (red > 255) {red = 255;}
                if (green > 255) {green = 255;}
                if (blue > 255) {blue = 255;}
                sepiaApplied.setPixel(i, j, Color.argb(alpha, red, green, blue));
            }
        }
        return sepiaApplied;
    }

    public Bitmap snow() {
        int[] pixels = new int[sourceWidth * sourceHeight];
        int red, green, blue, index = 0, threshold = 50;
        // Create an output Bitmap with the settings of the original
        Bitmap snowApplied = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.RGB_565);
        // Get the pixel array of the original bitmap.
        sourceBitmap.getPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);
        // Commence randomness.
        Random random = new Random();
        // Scan through the original Bitmap
        for(int i = 0; i < sourceWidth; i++) {
            for(int j = 0; j < sourceHeight; j++) {
                // Get current index in the 2D-matrix.
                index = i + sourceWidth * j;
                red = Color.red(pixels[index]);
                green = Color.green(pixels[index]);
                blue = Color.blue(pixels[index]);
                threshold = random.nextInt(0xFF);
                if(red > threshold && green > threshold && blue > threshold) {
                    pixels[index] = Color.rgb(0xFF, 0xFF, 0xFF);
                }
            }
        }
        snowApplied.setPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);
        return snowApplied;
    }
}
