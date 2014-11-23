package nl.liacs.huecolor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

/**
 * Custom view for showing a grayscale image and drawing a path on the image.
 */
public class SelectionView extends View {
    protected class Distance {
        public Distance(float x, float y, float d) {
            this.x = x;
            this.y = y;
            this.distance = d;
        }
        public float x;
        public float y;
        public float distance;
    }

    protected class PointsList {
        private ArrayList<PointF> points;

        public PointsList() {
            points = new ArrayList<PointF>();
        }

        public PointsList(ArrayList<PointF> list) {
            points = list;
        }

        public int size() {
            return points.size();
        }

        public void add(PointF point) {
            points.add(point);
        }

        public boolean isEmpty() {
            return points.isEmpty();
        }

        public PointF get(int index) {
            return points.get(index);
        }

        public boolean has(PointF point) {
            return points.contains(point);
        }

        public ArrayList<PointF> getPoints() {
            return points;
        }

        public Distance findClosestPoint(PointF point, Distance minDistance) {
            float distance, x, y;

            // Find the edge with the least distance from the point.
            for (PointF edgePoint : points) {
                // Calculate the distance from the point to this edge point.
                x = edgePoint.x - point.x;
                y = edgePoint.y - point.y;
                distance = (float)Math.sqrt(x * x + y * y);

                // Use this edge point if it is closer than any other edge point.
                if (distance < minDistance.distance) {
                    minDistance.distance = distance;
                    minDistance.x = edgePoint.x;
                    minDistance.y = edgePoint.y;
                }
            }
            return minDistance;
        }
    }

    private Uri fileUri;

    // Canvas
    private Canvas canvas = null;
    private int canvasLeft = 0, canvasTop = 0;
    private Bitmap bitmap = null;
    private Bitmap alteredBitmap = null;
    private int currentFilter = 0;

    // Drawing
    private Paint paint;
    private Paint fillPaint;
    private Path path;
    private float touchStartX, touchStartY; // Used for closing an incomplete path
    private float startX, startY; // Used for calculating new point of line
    private static final float TOUCH_TOLERANCE = 4; // Defines how quickly we should draw a line
    private PointsList pointsList = new PointsList(); // List of points in the path

    // Edge detection constants
    private final static int KERNEL_WIDTH = 3;
    private final static int KERNEL_HEIGHT = 3;
    private final static int EDGE_THRESHOLD = 100;
    private final static int[][] kernel = {
        {0, -1, 0},
        {-1, 4, -1},
        {0, -1, 0}
    };

    // Geometric hashing
    private final static int BLOCK_SIZE = 10;
    private PointsList[][] edgePointBuckets = null;
    private int bucketHeight = 0;
    private int bucketWidth = 0;
    private final static int[][] neighbors = {
        {-1,-1}, {0,-1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}
    };

    // Path adjustment multithreading
    private Handler handler = new Handler();
    private Thread adjustPathThread = null;
    private boolean adjustDone = false;

    // Filters
    private Filters filters;
    public final static int INVERT_FILTER = 1;
    public final static int SEPIA_FILTER = 2;
    public final static int SNOW_FILTER = 3;
    public final static int GRAYSCALE_FILTER = 4;

    public SelectionView(Context context) {
        this(context, null, GRAYSCALE_FILTER);
    }

    public SelectionView(Context context, Uri fileUri, int filterOption) {
        super(context);
        this.fileUri = fileUri;
        this.currentFilter = filterOption;

        path = new Path();

        // Define the paint for the selection line.
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        // Set the line color to red.
        paint.setColor(Color.RED);
        // Also fill the area drawn.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(8);

        // Define the paint for the fill.
        fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setDither(true);
        // Set the fill color to transparent.
        fillPaint.setARGB(255, 0, 0, 0);
        // Also fill the area drawn.
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setStrokeJoin(Paint.Join.ROUND);
        fillPaint.setStrokeCap(Paint.Cap.ROUND);

        // Load the image
        loadImage();
    }

    protected void loadImage() {
        // Set the image options.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = true;
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inPurgeable = true;

        // Load the image.
        if (fileUri != null) {
            try {
                ParcelFileDescriptor pfd = getContext().getContentResolver().openFileDescriptor(fileUri, "r");
                FileDescriptor fd = pfd.getFileDescriptor();
                bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);

            } catch (Throwable e) {
                bitmap = null;
            }
        }
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.example, options);
        }

        // Initialize the filters
        filters = new Filters(bitmap);
    }

    public void applyFilter(int filterOption) {
        this.currentFilter = filterOption;
        switch(filterOption) {
            case INVERT_FILTER:
                alteredBitmap = filters.invert();
                break;
            case SEPIA_FILTER:
                alteredBitmap = filters.sepia();
                break;
            case SNOW_FILTER:
                alteredBitmap = filters.snow();
                break;
            case GRAYSCALE_FILTER:
            default:
                alteredBitmap = filters.grayscale();
                break;
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        super.onSizeChanged(w, h, oldWidth, oldHeight);

        int oldBitmapWidth = bitmap.getWidth();
        int oldBitmapHeight = bitmap.getHeight();

        // Scale the bitmap to prevent memory issues during edge detection and to make it fit on the screen.
        bitmap = scaleToView(bitmap);

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        // Scale the image to the device by specifying its density.
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        bitmap.setDensity((int)(metrics.density * 160f));

        // Reinitialize the filters
        filters = new Filters(bitmap);

        // Copy the original bitmap into the altered Bitmap.
        alteredBitmap = Bitmap.createBitmap(bitmap);

        // Convert the image with the desired filter.
        applyFilter(currentFilter);

        // Initialize the BitmapShader with the Bitmap object and set the texture tile mode
        BitmapShader fillShader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Matrix m = new Matrix();
        m.postTranslate(canvasLeft, canvasTop);
        fillShader.setLocalMatrix(m);
        // Assign the shader to this paint
        fillPaint.setShader(fillShader);

        // Define the canvas and line path.
        canvas = new Canvas(bitmap.copy(Bitmap.Config.ARGB_8888, true));

        // Move the path to the correct location
        path.reset();
        if (pointsList != null && !pointsList.isEmpty()) {
            float ratioX = (float)bitmapWidth / (float)oldBitmapWidth;
            float ratioY = (float)bitmapHeight / (float)oldBitmapHeight;
            int pointListSize = pointsList.size();

            PointF first = pointsList.get(0);
            first.x *= ratioX;
            first.y *= ratioY;
            path.moveTo(first.x + canvasLeft, first.y + canvasTop);

            PointF prev = first;
            for (int i = 1; i < pointListSize; i++) {
                PointF point = pointsList.get(i);
                point.x *= ratioX;
                point.y *= ratioY;
                addPathSegment(prev.x + canvasLeft, prev.y + canvasTop, point.x + canvasLeft, point.y + canvasTop);
                prev = point;
            }
            path.lineTo(first.x + canvasLeft, first.y + canvasTop);
        }

        // Start edge detection as a background process.
        startEdgeDetection();
    }

    private Bitmap scaleToView(Bitmap bitmap) {
        // Scale the loaded image to fit each screen.
        // No need to check for orientation: the metrics will be
        // adjusted automatically in that case.
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int viewWidth = metrics.widthPixels;
        int viewHeight = metrics.heightPixels;
        int newWidth = -1;
        int newHeight = -1;
        float scaleFactor = -1.0F;

        // The metrics are of the entire screen. We want to remove the size of the
        // action bar and the status bar so we get the actual height of the view.

        // Remove action bar height
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        viewHeight -= getResources().getDimensionPixelSize(tv.resourceId);

        // Remove status bar height
        final int LOW_DPI_STATUS_BAR_HEIGHT = 19;
        final int MEDIUM_DPI_STATUS_BAR_HEIGHT = 25;
        final int HIGH_DPI_STATUS_BAR_HEIGHT = 38;

        switch (metrics.densityDpi) {
            case DisplayMetrics.DENSITY_HIGH:
                viewHeight -= HIGH_DPI_STATUS_BAR_HEIGHT;
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                viewHeight -= MEDIUM_DPI_STATUS_BAR_HEIGHT;
                break;
            case DisplayMetrics.DENSITY_LOW:
                viewHeight -= LOW_DPI_STATUS_BAR_HEIGHT;
                break;
            default:
                viewHeight -= HIGH_DPI_STATUS_BAR_HEIGHT;
        }

        if (originalHeight > originalWidth) {
            scaleFactor = (float)originalWidth / (float)originalHeight;
            newHeight = viewHeight;
            newWidth = (int) (newHeight * scaleFactor);
        } else if (originalWidth > originalHeight) {
            scaleFactor = (float)originalHeight / (float)originalWidth;
            newWidth = viewWidth;
            newHeight = (int) (newWidth * scaleFactor);
        } else if (originalHeight == originalWidth) {
            newHeight = (viewHeight > viewWidth ? viewWidth : viewHeight);
            newWidth = (viewHeight > viewWidth ? viewWidth : viewHeight);
        }

        // Make sure the image is not larger than the view width: sometimes the scaling above
        // makes the image a bit larger than that.
        if (newHeight > viewHeight) {
            newWidth *= (float)viewHeight / (float)newHeight;
            newHeight = viewHeight;
        } else if (newWidth > viewWidth) {
            newHeight *= (float)viewWidth / (float)newWidth;
            newWidth = viewWidth;
        }

        canvasLeft = (viewWidth - newWidth) / 2;
        canvasTop = (viewHeight - newHeight) / 2;

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Update the previous path and draw the new path.
        if (alteredBitmap != null) {
            canvas.drawBitmap(alteredBitmap, canvasLeft, canvasTop, null);
            canvas.drawPath(path, paint);
            if (adjustDone) {
                canvas.drawPath(path, fillPaint);
            }
        }
    }

    private void addPathSegment(float x1, float y1, float x2, float y2) {
        path.quadTo(x1, y1, (x2 + x1) / 2, (y2 + y1) / 2);
    }

    private void touchStart(float x, float y) {
        // Start a new path when the user taps on the screen.
        // Clear any old path from the canvas.
        if (adjustPathThread != null) {
            adjustPathThread.interrupt();
            adjustPathThread = null;
        }
        adjustDone = false;
        path.reset();
        path.moveTo(x, y);
        pointsList = new PointsList();
        pointsList.add(new PointF(x-canvasLeft,y-canvasTop));
        touchStartX = startX = x;
        touchStartY = startY = y;
    }

    private void touchMove(float x, float y) {
        // Add a point to the path when the user moves his or her finger.
        // Sensitivity is added for the sake of performance.
        float dx = Math.abs(x - startX);
        float dy = Math.abs(y - startY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            addPathSegment(startX, startY, x, y);
            pointsList.add(new PointF(x - canvasLeft, y - canvasTop));
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
        bucketWidth = (int)Math.ceil(sourceWidth / (double)BLOCK_SIZE);
        bucketHeight = (int)Math.ceil(sourceHeight / (double)BLOCK_SIZE);
        edgePointBuckets = new PointsList[bucketWidth][bucketHeight];

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
                    PointF p = new PointF(i,j);
                    PointsList bucket = edgePointBuckets[i / BLOCK_SIZE][j / BLOCK_SIZE];
                    if (bucket == null) {
                        bucket = edgePointBuckets[i / BLOCK_SIZE][j / BLOCK_SIZE] = new PointsList();
                    }
                    bucket.add(p);
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
                        adjustPathThread = null;
                    }
                });
            }
        };
        adjustPathThread = new Thread(runnable);
        adjustPathThread.start();
    }

    private void adjustPath() {
        try {
            // We want to move each point in the points list to the nearest edge pixel.
            int pointListSize = pointsList.size();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            for (PointF point : pointsList.getPoints()) {
                Distance minDistance = new Distance(0.0f, 0.0f, Float.POSITIVE_INFINITY);
                point.x = (point.x < 0 ? 0 : (point.x > width ? width : point.x));
                point.y = (point.y < 0 ? 0 : (point.y > height ? height : point.y));

                int i = (int)(point.x / BLOCK_SIZE);
                int j = (int)(point.y / BLOCK_SIZE);

                PointsList bucket = edgePointBuckets[i][j];
                // Check if the point already happens to be on an edge.
                if (bucket != null) {
                    if (bucket.has(point)) {
                        minDistance = new Distance(point.x, point.y, 0.0f);
                    } else {
                        bucket.findClosestPoint(point, minDistance);
                    }
                }
                if (minDistance.distance > 0.0f) {
                    // Check the neighbors since they might be closer
                    for (int[] neighbor : neighbors) {
                        if (i + neighbor[0] < 0 || i + neighbor[0] > bucketWidth - 1 ||
                            j + neighbor[1] < 0 || j + neighbor[1] > bucketHeight - 1) {
                            continue;
                        }
                        float xD = neighbor[0] * (i-minDistance.x) + (neighbor[0] == -1 ? BLOCK_SIZE : 0.0f);
                        float yD = neighbor[1] * (j-minDistance.y) + (neighbor[1] == -1 ? BLOCK_SIZE : 0.0f);
                        if (xD * xD + yD * yD < minDistance.distance * minDistance.distance) {
                            // Check neighbor since we're close to it
                            bucket = edgePointBuckets[i + neighbor[0]][j + neighbor[1]];
                            if (bucket != null) {
                                bucket.findClosestPoint(point, minDistance);
                            }
                        }
                    }
                }

                // We have found the nearest edge point. Move the point towards that edge point.
                if (minDistance.distance < Float.POSITIVE_INFINITY) {
                    point.set(minDistance.x, minDistance.y);
                }
            }

            // Draw the first point of the new path.
            path.reset();
            float x, y;
            PointF point = pointsList.get(0);
            startX = x = point.x + canvasLeft;
            startY = y = point.y + canvasTop;
            path.moveTo(x, y);

            // Draw the rest of the points of the new path.
            for (int k = 1; k < pointListSize; k++) {
                point = pointsList.get(k);
                addPathSegment(x, y, point.x + canvasLeft, point.y + canvasTop);
                x = point.x + canvasLeft;
                y = point.y + canvasTop;
            }

            // Finish drawing the line.
            path.lineTo(startX, startY);
            adjustDone = true;
        } catch (ConcurrentModificationException e) {
            // Ignore the exception and let the concurrent access handle everything.
        }
    }
}
