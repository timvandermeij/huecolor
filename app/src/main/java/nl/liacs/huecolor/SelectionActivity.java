package nl.liacs.huecolor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

/**
 * Based on https://code.google.com/p/apidemos/source/browse/trunk/ApiDemos/src/com/example/android/apis/graphics/FingerPaint.java
 */
public class SelectionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use a custom view for the image.
        setContentView(new SelectionView(this));
    }

    /**
     * Custom view for showing a grayscale image and drawing a path on the image.
     */
    public class SelectionView extends View {
        private Paint paint;
        private Bitmap bitmap;
        private Canvas canvas;
        private Path path;
        private Paint bitmapPaint;
        private float touchStartX, touchStartY;
        private float startX, startY; // Used for calculating new point of line
        private static final float TOUCH_TOLERANCE = 4; // Defines how quickly we should draw a line

        public SelectionView(Context context) {
            super(context);

            // Set the image options.
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = true;
            options.inScaled = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inPurgeable = true;

            // Load the image and define the canvas on top of it.
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.example, options);

            // Scale the image to the device by specifying its density.
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            bitmap.setDensity((int)(metrics.density * 160f));

            // Define the canvas and line path.
            canvas = new Canvas(bitmap.copy(Bitmap.Config.ARGB_8888, true));
            path = new Path();
            bitmapPaint = new Paint(Paint.DITHER_FLAG);

            // Define the paint for the selection line.
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(10);

            // Convert the image to grayscale.
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            bitmapPaint.setColorFilter(filter);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
            super.onSizeChanged(w, h, oldWidth, oldHeight);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // Update the previous path and draw the new path.
            canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
            canvas.drawPath(path, paint);
        }

        private void touchStart(float x, float y) {
            // Start a new path when the user taps on the screen.
            // Clear any old paths from the canvas.
            path.reset();
            path.moveTo(x, y);
            touchStartX = startX = x;
            touchStartY = startY = y;
        }

        private void touchMove(float x, float y) {
            // Add a point to the path when the user moves his or her finger.
            // Sensitivity is added for the sake of performance.
            float dx = Math.abs(x - startX);
            float dy = Math.abs(y - startY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                path.quadTo(startX, startY, (x + startX) / 2, (y + startY) / 2);
                startX = x;
                startY = y;
            }
        }

        private void touchUp() {
            // Finish drawing the line.
            path.lineTo(touchStartX, touchStartY);
            canvas.drawPath(path, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            // Handle touch events: tap, move and finger up.
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStart(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touchMove(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touchUp();
                    invalidate();
                    break;
                default:
                    break;
            }
            return true;
        }
    }
}