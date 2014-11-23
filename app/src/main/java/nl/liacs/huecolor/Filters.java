package nl.liacs.huecolor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;

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

    // Possibility, but not within the purpose of this app, nor usable at the moment.
    public Bitmap reflectionFilter(Bitmap original) {
        // Gap space between the original and the reflected bitmap.
        final int gap = 4;
        int originalWidth = original.getWidth(), originalHeight = original.getHeight();
        // Matrix to flip on the Y axis.
        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);
        // Flipped lower half of the original Bitmap
        Bitmap reflection = Bitmap.createBitmap(original, 0, originalHeight/2, originalWidth, originalHeight/2, matrix, false);
        // Output bitmap, taller to also fit reflection
        Bitmap reflected = Bitmap.createBitmap(originalWidth, (int) (1.5 * originalHeight), Bitmap.Config.ARGB_8888);

        // Combine gap with reflection and original bitmap.
        Canvas canvas = new Canvas(reflected);
        canvas.drawBitmap(original, 0, 0, null);
        Paint firstPaint = new Paint();
        canvas.drawRect(0, originalHeight, originalWidth, originalHeight + gap, firstPaint);
        canvas.drawBitmap(reflection, 0, originalHeight + gap, null);
        Paint secondPaint = new Paint();
        LinearGradient gradient = new LinearGradient(0, originalHeight, 0, reflected.getHeight() + gap,
                                                     0x70ffffff, 0x00ffffff, Shader.TileMode.CLAMP);
        secondPaint.setShader(gradient);
        secondPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawRect(0, originalHeight, originalWidth, reflected.getHeight() + gap, secondPaint);
        return reflected;
    }
}
