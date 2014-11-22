package nl.liacs.huecolor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

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
        Canvas canvas = new Canvas(grayScaled);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter gray = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(gray);
        canvas.drawBitmap(original, 0, 0, paint);
        return grayScaled;
    }
}
