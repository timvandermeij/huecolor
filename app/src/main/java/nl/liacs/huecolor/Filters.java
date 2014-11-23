package nl.liacs.huecolor;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.Random;

/**
 * Based on https://github.com/kpbird/Android-Image-Filters/blob/master/ImageEffect/src/main/java/com/kpbird/imageeffect/ImageFilters.java
 */
public class Filters {
    public Bitmap invertFilter(Bitmap original) {
        // Create an output Bitmap with the settings of the original
        Bitmap inverted = Bitmap.createBitmap(original.getWidth(), original.getHeight(), original.getConfig());
        // Some variables which will serve temporarily later on
        int alpha, red, green, blue, pixel;
        // Scan through the original Bitmap
        for(int i = 0; i < original.getWidth(); i++) {
            for(int j = 0; j < original.getHeight(); j++) {
                pixel = original.getPixel(i,j);
                alpha = Color.alpha(pixel);
                red = 255 - Color.red(pixel);
                green = 255 - Color.green(pixel);
                blue = 255 - Color.blue(pixel);
                inverted.setPixel(i, j, Color.argb(alpha, red, green, blue));
            }
        }
        return inverted;
    }

    public Bitmap grayScaleFilter(Bitmap original) {
        // Create an output Bitmap with the settings of the original
        Bitmap grayScaled = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        int alpha, red, green, blue, pixel;
        // Scan through the original Bitmap
        for(int i = 0; i < original.getWidth(); i++) {
            for(int j = 0; j < original.getHeight(); j++) {
                pixel = original.getPixel(i,j);
                alpha = Color.alpha(pixel);
                // Apply grayscale
                red = green = blue = (int) (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel));
                grayScaled.setPixel(i, j, Color.argb(alpha, red, green, blue));
            }
        }
        return grayScaled;
    }

    public Bitmap sepiaFilter(Bitmap original) {
        // Create an output Bitmap with the settings of the original
        Bitmap sepiaApplied = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        int alpha, red, green, blue, pixel;
        for(int i = 0; i < original.getWidth(); i++) {
            for(int j = 0; j < original.getHeight(); j++) {
                pixel = original.getPixel(i,j);
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

    public Bitmap snowFilter(Bitmap original) {
        // Some initializations
        int originalWidth = original.getWidth(), originalHeight = original.getHeight();
        int[] pixels = new int[originalWidth * originalHeight];
        int red, green, blue, index = 0, threshold = 50;
        // Create an output Bitmap with the settings of the original
        Bitmap snowApplied = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.RGB_565);
        // Get the pixel array of the original bitmap.
        original.getPixels(pixels, 0, originalWidth, 0, 0, originalWidth, originalHeight);
        // Commence randomness.
        Random random = new Random();
        // Scan through the original Bitmap
        for(int i = 0; i < originalWidth; i++) {
            for(int j = 0; j < originalHeight; j++) {
                // Get current index in the 2D-matrix.
                index = i + originalWidth * j;
                red = Color.red(pixels[index]);
                green = Color.green(pixels[index]);
                blue = Color.blue(pixels[index]);
                threshold = random.nextInt(0xFF);
                if(red > threshold && green > threshold && blue > threshold) {
                    pixels[index] = Color.rgb(0xFF, 0xFF, 0xFF);
                }
            }
        }
        snowApplied.setPixels(pixels, 0, originalWidth, 0, 0, originalWidth, originalHeight);
        return snowApplied;
    }
}
