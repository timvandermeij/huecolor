package nl.liacs.huecolor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Custom view for showing a grayscale image and drawing a path on the image.
 */
public class SelectionView extends View {
    // Canvas and drawing constants
    private Paint paint;
    private Bitmap bitmap;
    private Canvas canvas;
    private Path path;
    private Paint bitmapPaint;
    private float touchStartX, touchStartY;
    private float startX, startY; // Used for calculating new point of line
    private static final float TOUCH_TOLERANCE = 4; // Defines how quickly we should draw a line
    private ArrayList<PointF> pointsList = new ArrayList<PointF>();

    // Edge detection constants
    private Bitmap edgeBitmap;
    private final static int KERNEL_WIDTH = 3;
    private final static int KERNEL_HEIGHT = 3;
    private final static int[][] kernel = {
        {0, -1, 0},
        {-1, 4, -1},
        {0, -1, 0}
    };

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

        // Start edge detection in the background
        startEdgeDetection();
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
        pointsList.clear();
        pointsList.add(new PointF(x,y));
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
            pointsList.add(new PointF(x,y));
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

    private void startEdgeDetection() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                edgeBitmap = detectEdges(kernel);
            }
        };
        new Thread(runnable).start();
    }

    /*
     * Based on http://android-coding.blogspot.nl/2012/05/android-image-processing-edge-detect.html
     */
    private Bitmap detectEdges(int[][] knl) {
        int sourceWidth = bitmap.getWidth();
        int sourceHeight = bitmap.getHeight();
        int WIDTH_MINUS_2 = sourceWidth - 2;
        int HEIGHT_MINUS_2 = sourceHeight - 2;

        int i, j, k, l; // Iterators
        int subSumR = 0, subSumG = 0, subSumB = 0;
        int pixel = 0, knlCache = 0;
        int x = 0, y = 0;

        // Source bitmap pixels
        int[] sourcePixels = new int[sourceWidth * sourceHeight];
        bitmap.getPixels(sourcePixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);

        // Destination bitmap
        Bitmap destination = Bitmap.createBitmap(sourceWidth, sourceHeight, bitmap.getConfig());
        int[] destinationPixels = new int[sourceWidth * sourceHeight];
        destination.getPixels(destinationPixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);

        for (i = 1; i <= WIDTH_MINUS_2; i++) {
            for (j = 1; j <= HEIGHT_MINUS_2; j++) {
                subSumR = subSumG = subSumB = 0;
                for (k = 0; k < KERNEL_WIDTH; k++) {
                    x = i - 1 + k;
                    for (l = 0; l < KERNEL_HEIGHT; l++) {
                        y = j - 1 + l;
                        pixel = sourcePixels[y * sourceWidth + x];
                        knlCache = knl[k][l];
                        subSumR += ((pixel >> 16) & 0xFF) * knlCache; // Red component
                        subSumG += ((pixel >> 8) & 0xFF) * knlCache; // Green component
                        subSumB += (pixel & 0xFF) * knlCache; // Blue component
                    }
                }
                destinationPixels[j * sourceWidth + i] = Color.argb(
                    sourcePixels[j * sourceWidth + i] >>> 24, // Alpha component
                    subSumR < 0 ? 0 : (subSumR > 255 ? 255 :subSumR),
                    subSumG < 0 ? 0 : (subSumG > 255 ? 255 :subSumG),
                    subSumB < 0 ? 0 : (subSumB > 255 ? 255 :subSumB)
                );
            }
        }
        destination.setPixels(destinationPixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);
        sourcePixels = destinationPixels = null; // Free memory directly instead of relying on GC.
        return destination;
    }
}