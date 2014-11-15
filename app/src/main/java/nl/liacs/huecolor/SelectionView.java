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
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Custom view for showing a grayscale image and drawing a path on the image.
 */
public class SelectionView extends View {
    // Canvas
    private Canvas canvas;
    private Bitmap bitmap;
    private Paint grayscaleFilter;

    // Drawing
    private Paint paint;
    private Path path;
    private float touchStartX, touchStartY; // Used for closing an incomplete path
    private float startX, startY; // Used for calculating new point of line
    private static final float TOUCH_TOLERANCE = 4; // Defines how quickly we should draw a line
    private ArrayList<PointF> pointsList = new ArrayList<PointF>(); // List of points in the path

    // Edge detection constants
    private ArrayList<PointF> edgePointsList = new ArrayList<PointF>();
    private final static int KERNEL_WIDTH = 3;
    private final static int KERNEL_HEIGHT = 3;
    private final static int EDGE_THRESHOLD = 8;
    private final static int[][] kernel = {
        {0, -1, 0},
        {-1, 4, -1},
        {0, -1, 0}
    };

    private Handler handler = new Handler();

    public SelectionView(Context context) {
        super(context);

        // Set the image options.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = true;
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inPurgeable = true;

        // Load the image and define the canvas on top of it.
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.example2, options);

        // Scale the image to the device by specifying its density.
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        bitmap.setDensity((int)(metrics.density * 160f));

        // Define the canvas and line path.
        canvas = new Canvas(bitmap.copy(Bitmap.Config.ARGB_8888, true));
        path = new Path();

        // Define the paint for the selection line.
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(8);

        // Convert the image to grayscale.
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        grayscaleFilter = new Paint(Paint.DITHER_FLAG);
        grayscaleFilter.setColorFilter(filter);

        // Start edge detection as a background process.
        startEdgeDetection();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        super.onSizeChanged(w, h, oldWidth, oldHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Update the previous path and draw the new path.
        canvas.drawBitmap(bitmap, 0, 0, grayscaleFilter);
        canvas.drawPath(path, paint);
    }

    private void touchStart(float x, float y) {
        // Start a new path when the user taps on the screen.
        // Clear any old path from the canvas.
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
        // Finish drawing the line and connect an unclosed path.
        // Adjust the path using the edge detection data afterward.
        path.lineTo(touchStartX, touchStartY);
        canvas.drawPath(path, paint);
        startAdjustPath();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: // Finger tap
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE: // Finger move
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP: // Finger up
                touchUp();
                invalidate();
                break;
            default:
                break;
        }
        return true;
    }

    private void startEdgeDetection() {
        // Run edge detection on the bitmap as a background process.
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                detectEdges(kernel);
            }
        };
        new Thread(runnable).start();
    }

    /*
     * Based on http://android-coding.blogspot.nl/2012/05/android-image-processing-edge-detect.html
     */
    @SuppressWarnings("UnusedAssignment")
    private void detectEdges(final int[][] knl) {
        int sourceWidth = bitmap.getWidth();
        int sourceHeight = bitmap.getHeight();
        int WIDTH_MINUS_2 = sourceWidth - 2;
        int HEIGHT_MINUS_2 = sourceHeight - 2;

        int i, j, k, l; // Iterators
        int subSumR = 0, subSumG = 0, subSumB = 0; // Color components
        int pixel = 0, knlCache = 0;
        int x = 0, y = 0;

        // Source bitmap pixels
        int[] sourcePixels = new int[sourceWidth * sourceHeight];
        bitmap.getPixels(sourcePixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight);

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
                subSumR = (subSumR < 0 ? 0 : (subSumR > 255 ? 255 : subSumR));
                subSumG = (subSumG < 0 ? 0 : (subSumG > 255 ? 255 : subSumG));
                subSumB = (subSumB < 0 ? 0 : (subSumB > 255 ? 255 : subSumB));

                // Keep track of the points that are on edges.
                if (subSumR > EDGE_THRESHOLD || subSumB > EDGE_THRESHOLD || subSumG > EDGE_THRESHOLD) {
                    edgePointsList.add(new PointF(i,j));
                }
            }
        }
        sourcePixels = null; // Free memory directly instead of relying on GC.
    }

    private void startAdjustPath() {
        // Run path adjust using edge detection as a background process.
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                adjustPath();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        invalidate();
                    }
                });
            }
        };
        new Thread(runnable).start();
    }

    private void adjustPath() {
        // We want to move each point in the points list to the nearest edge pixel.
        float x = 0, y = 0, minX = 0, minY = 0;
        double distance = Double.POSITIVE_INFINITY, minDistance = Double.POSITIVE_INFINITY;
        int pointListSize = pointsList.size();

        for (PointF point : pointsList) {
            // Check if the point already happens to be on an edge.
            if (edgePointsList.contains(point)) {
                continue;
            }

            // Otherwise find the edge with the least distance from the point.
            for (PointF edgePoint : edgePointsList) {
                // Calculate the distance from the point to this edge point.
                x = edgePoint.x - point.x;
                y = edgePoint.y - point.y;
                distance = Math.sqrt(x * x + y * y);

                // Use this edge point if it is closer than any other edge point.
                if (distance < minDistance) {
                    minDistance = distance;
                    minX = edgePoint.x;
                    minY = edgePoint.y;
                }
            }

            // We have found the nearest edge point. Move the point towards that edge point.
            point.set(minX, minY);

            // Reset the values for the next iteration.
            minX = minY = 0;
            minDistance = Double.POSITIVE_INFINITY;
        }

        // Draw the first point of the new path.
        path.reset();
        PointF point = pointsList.get(0);
        startX = x = point.x;
        startY = y = point.y;
        path.moveTo(x, y);

        // Draw the rest of the points of the new path.
        for (int k = 1; k < pointListSize; k++) {
            point = pointsList.get(k);
            path.quadTo(x, y, (point.x + x) / 2, (point.y + y) / 2);
            x = point.x;
            y = point.y;
        }

        // Finish drawing the line.
        path.lineTo(startX, startY);
    }
}