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
        Bitmap target = Bitmap.createBitmap(sourceWidth, sourceHeight, sourceBitmap.getConfig());

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
        target.setPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);
        return target;
    }

    public Bitmap grayscale() {
        int[] pixels = new int[sourceWidth * sourceHeight];
        int red, green, blue, pixel;
        Bitmap target = Bitmap.createBitmap(sourceWidth, sourceHeight, sourceBitmap.getConfig());

        // Get the pixel array of the original bitmap.
        sourceBitmap.getPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);

        // Apply the grayscale effect.
        for(int i = 0; i < sourceWidth; i++) {
            for(int j = 0; j < sourceHeight; j++) {
                pixel = pixels[i + sourceWidth * j];
                red = (pixel >> 16) & 0xFF; // Red component
                green = (pixel >> 8) & 0xFF; // Green component
                blue = pixel & 0xFF; // Blue component
                red = green = blue = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                pixels[i + sourceWidth * j] = Color.argb(pixel >>> 24, red, green, blue);
            }
        }
        target.setPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);
        return target;
    }

    public Bitmap sepia() {
        int[] pixels = new int[sourceWidth * sourceHeight];
        int red, green, blue, pixel;
        Bitmap target = Bitmap.createBitmap(sourceWidth, sourceHeight, sourceBitmap.getConfig());

        // Get the pixel array of the original bitmap.
        sourceBitmap.getPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);

        // Apply the sepia effect.
        for(int i = 0; i < sourceWidth; i++) {
            for(int j = 0; j < sourceHeight; j++) {
                pixel = pixels[i + sourceWidth * j];
                red = (pixel >> 16) & 0xFF; // Red component
                green = (pixel >> 8) & 0xFF; // Green component
                blue = pixel & 0xFF; // Blue component

                // Grayscale
                red = green = blue = (int) (0.299 * red + 0.587 * green + 0.114 * blue);

                // Sepia
                red += 15;
                green += 6;
                blue += 1;
                red = (red < 0 ? 0 : (red > 255 ? 255 : red));
                green = (green < 0 ? 0 : (green > 255 ? 255 : green));
                blue = (blue < 0 ? 0 : (blue > 255 ? 255 : blue));
                pixels[i + sourceWidth * j] = Color.argb(pixel >>> 24, red, green, blue);
            }
        }
        target.setPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);
        return target;
    }

    public Bitmap snow() {
        int[] pixels = new int[sourceWidth * sourceHeight];
        int red, green, blue, pixel, threshold;
        Bitmap target = Bitmap.createBitmap(sourceWidth, sourceHeight, sourceBitmap.getConfig());
        Random random = new Random();

        // Get the pixel array of the original bitmap.
        sourceBitmap.getPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);

        // Apply the snow effect.
        for(int i = 0; i < sourceWidth; i++) {
            for(int j = 0; j < sourceHeight; j++) {
                pixel = pixels[i + sourceWidth * j];
                red = (pixel >> 16) & 0xFF; // Red component
                green = (pixel >> 8) & 0xFF; // Green component
                blue = pixel & 0xFF; // Blue component
                threshold = random.nextInt(0xFF);
                if(red > threshold && green > threshold && blue > threshold) {
                    pixels[i + sourceWidth * j] = Color.argb(pixel >>> 24, 0xff, 0xff, 0xff);
                }
            }
        }
        target.setPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);
        return target;
    }
}
