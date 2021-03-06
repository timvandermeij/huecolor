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
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

/**
 * Custom view for showing a grayscale image and drawing a path on the image.
 */
public class SelectionView extends View {
    protected class Distance {
        public float x;
        public float y;
        public float distance;

        public Distance(float x, float y, float d) {
            this.x = x;
            this.y = y;
            this.distance = d;
        }
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
    private int canvasLeft = 0, canvasTop = 0;
    private Bitmap bitmap = null;
    private int bitmapWidth = 0, bitmapHeight = 0;
    private Bitmap alteredBitmap = null;
    private int currentFilter = 0;
    private int currentColor = Color.RED;

    // Drawing
    private Paint paint;
    private Paint fillPaint;
    private Path path;
    private Path canvasPath;
    private boolean drawPath = true;
    private boolean invertSelection = false;
    private float touchStartX, touchStartY; // Used for closing an incomplete path
    private float startX, startY; // Used for calculating new point of line
    private static final float TOUCH_TOLERANCE = 4; // Defines how quickly we should draw a point
    private PointsList pointsList = new PointsList(); // List of points in the path
    private boolean drawDone = false;

    // Edge detection constants
    private final static int KERNEL_WIDTH = 3;
    private final static int KERNEL_HEIGHT = 3;
    private final static int EDGE_THRESHOLD = 60;
    private final static int[][] kernel = {
        {0, -1, 0},
        {-1, 4, -1},
        {0, -1, 0}
    };
    private boolean detectDone = false;

    // Geometrical hashing for edge detection and path adjustment
    private final static int BLOCK_SIZE = 100;
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
    public final static int COLORIZE_FILTER = 5;

    public SelectionView(Context context) {
        this(context, null, GRAYSCALE_FILTER);
    }

    public SelectionView(Context context, Uri fileUri, int filterOption) {
        super(context);
        this.fileUri = fileUri;
        this.currentFilter = filterOption;

        initializeDraw();

        // Load the image
        loadImage();
    }

    public void initializeDraw() {
        path = new Path();
        canvasPath = new Path();
        canvasPath.setFillType(Path.FillType.EVEN_ODD);

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

                // First decode with inJustDecodeBounds=true to check dimensions
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFileDescriptor(fd, null, options);
                options.inJustDecodeBounds = false;

                // Sample the image while reading for memory constraints. We scale it down correctly later.
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                options.inSampleSize = Math.max(options.outHeight / metrics.heightPixels, options.outWidth / metrics.widthPixels);

                pfd = getContext().getContentResolver().openFileDescriptor(fileUri, "r");
                fd = pfd.getFileDescriptor();
                bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
            } catch (Throwable e) {
                bitmap = null;
                options.inSampleSize = 1;
                options.inJustDecodeBounds = false;
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
            case COLORIZE_FILTER:
                ColorPickerDialog dialog = new ColorPickerDialog(getContext(), new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void colorChanged(int color) {
                        currentColor = color;
                        alteredBitmap = filters.colorize(color);
                        invalidate();
                    }
                }, currentColor);
                dialog.show();
                break;
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

    public void invertSelection() {
        invertSelection = !invertSelection;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        super.onSizeChanged(w, h, oldWidth, oldHeight);

        initializeScale();
    }

    public void initializeScale() {
        int oldBitmapWidth = bitmapWidth;
        int oldBitmapHeight = bitmapHeight;

        // Scale the bitmap to prevent memory issues during edge detection and to make it fit on the screen.
        bitmap = scaleToView(bitmap);

        bitmapWidth = bitmap.getWidth();
        bitmapHeight = bitmap.getHeight();

        // Scale the image to the device by specifying its density.
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        bitmap.setDensity((int)(metrics.density * 160f));

        // Reinitialize the filters
        filters = new Filters(bitmap);

        // Convert the image with the desired filter.
        applyFilter(currentFilter);

        // Initialize the BitmapShader with the Bitmap object and set the texture tile mode
        BitmapShader fillShader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Matrix m = new Matrix();
        m.postTranslate(canvasLeft, canvasTop);
        fillShader.setLocalMatrix(m);
        // Assign the shader to this paint
        fillPaint.setShader(fillShader);

        movePath(oldBitmapWidth, oldBitmapHeight);

        canvasPath.reset();
        canvasPath.addRect(canvasLeft, canvasTop, canvasLeft + bitmapWidth, canvasTop + bitmapHeight, Path.Direction.CW);

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

    public void saveState(Bundle outState) {
        outState.putString("fileUri", (fileUri == null ? null : fileUri.toString()));
        outState.putInt("currentFilter", currentFilter);
        outState.putParcelableArrayList("pointsList", pointsList.getPoints());
        outState.putBoolean("invertSelection", invertSelection);
        outState.putBoolean("adjustDone", adjustDone);
        outState.putInt("bitmapWidth", bitmap.getWidth());
        outState.putInt("bitmapHeight", bitmap.getHeight());
    }

    public void restoreState(Bundle savedInstanceState) {
        String uri = savedInstanceState.getString("fileUri");
        fileUri = (uri == null ? null : Uri.parse(uri));
        currentFilter = savedInstanceState.getInt("currentFilter");
        pointsList = new PointsList(savedInstanceState.<PointF>getParcelableArrayList("pointsList"));
        invertSelection = savedInstanceState.getBoolean("invertSelection");
        adjustDone = savedInstanceState.getBoolean("adjustDone");
        bitmapWidth = savedInstanceState.getInt("bitmapWidth");
        bitmapHeight = savedInstanceState.getInt("bitmapHeight");
        loadImage();
    }

    /* Move the path to the correct location according to changed bitmap size and canvas locations. */
    private void movePath(int oldBitmapWidth, int oldBitmapHeight) {
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
            drawDone = true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Update the previous path and draw the new path.
        performDraw(canvas);
    }

    protected void performDraw(Canvas canvas) {
        if (alteredBitmap != null) {
            if (invertSelection && !adjustDone && drawPath) {
                canvas.drawBitmap(bitmap, canvasLeft, canvasTop, null);
            } else {
                canvas.drawBitmap(alteredBitmap, canvasLeft, canvasTop, null);
            }
            if (drawPath) {
                canvas.drawPath(path, paint);
            }
            if (adjustDone || !drawPath) {
                if (invertSelection) {
                    Path p = new Path(canvasPath);
                    p.addPath(path);
                    canvas.drawPath(p, fillPaint);
                } else {
                    canvas.drawPath(path, fillPaint);
                }
            }
        }
    }

    /* Save the altered bitmap to a file */
    protected void saveCanvas(File file) {
        Bitmap diskBitmap = alteredBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(diskBitmap);
        // Set a bunch of canvas-specific variables to default, since we only draw the bitmap,
        // not the whole view. This is very hacky.
        int left = canvasLeft;
        int top = canvasTop;
        canvasLeft = 0;
        canvasTop = 0;
        movePath(bitmapWidth, bitmapHeight);
        Shader fillShader = fillPaint.getShader();
        Matrix m = new Matrix();
        fillShader.getLocalMatrix(m);
        fillShader.setLocalMatrix(null);
        drawPath = false;
        performDraw(canvas);
        drawPath = true;
        fillShader.setLocalMatrix(m);
        canvasLeft = left;
        canvasTop = top;
        movePath(bitmapWidth, bitmapHeight);
        try {
            diskBitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
        } catch (FileNotFoundException ignored) {
        }
    }

    public void release() {
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        if (alteredBitmap != null) {
            alteredBitmap.recycle();
            alteredBitmap = null;
        }
        filters = null;
        if (adjustPathThread != null) {
            adjustPathThread.interrupt();
            adjustPathThread = null;
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
        drawDone = false;
        path.reset();
        path.moveTo(x, y);
        pointsList = new PointsList();
        pointsList.add(new PointF(x - canvasLeft, y - canvasTop));
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
        drawDone = true;
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
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Make sure path is adjusted if it was rotated during draw
                        if (drawDone) {
                            startAdjustPath();
                        }
                    }
                });
            }
        };
        new Thread(runnable).start();
    }

    /*
     * Based on http://android-coding.blogspot.nl/2012/05/android-image-processing-edge-detect.html
     */
    @SuppressWarnings("UnusedAssignment")
    private void detectEdges(final int[][] knl) {
        int WIDTH_MINUS_2 = bitmapWidth - 2;
        int HEIGHT_MINUS_2 = bitmapHeight - 2;

        int i, j, k, l; // Iterators
        int subSumR = 0, subSumG = 0, subSumB = 0; // Color components
        int pixel = 0, knlCache = 0;
        int x = 0, y = 0;

        // Source bitmap pixels
        int[] sourcePixels = new int[bitmapWidth * bitmapHeight];
        bitmap.getPixels(sourcePixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);

        bucketWidth = (int)Math.ceil(bitmapWidth / (double)BLOCK_SIZE);
        bucketHeight = (int)Math.ceil(bitmapHeight / (double)BLOCK_SIZE);
        edgePointBuckets = new PointsList[bucketWidth][bucketHeight];

        for (i = 1; i <= WIDTH_MINUS_2; i++) {
            for (j = 1; j <= HEIGHT_MINUS_2; j++) {
                subSumR = subSumG = subSumB = 0;
                for (k = 0; k < KERNEL_WIDTH; k++) {
                    x = i - 1 + k;
                    for (l = 0; l < KERNEL_HEIGHT; l++) {
                        y = j - 1 + l;
                        pixel = sourcePixels[y * bitmapWidth + x];
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
                if (subSumR > EDGE_THRESHOLD || subSumG > EDGE_THRESHOLD || subSumB > EDGE_THRESHOLD) {
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
        detectDone = true;
    }

    private void startAdjustPath() {
        // Don't perform path adjustment if it would not have any effect.
        if (!detectDone || adjustDone) {
            return;
        }
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

    private boolean snapToBoundary(PointF point, float xD, float yD, Distance minDistance) {
        boolean snap = false;
        float snapX = point.x + xD;
        float snapY = point.y + yD;

        if (snapX <= 0.0f) {
            snap = true;
            xD += snapX;
            snapX = 0.0f;
        } else if (snapX > bitmapWidth) {
            snap = true;
            xD -= snapX - bitmapWidth;
            snapX = bitmapWidth;
        } else {
            snapX = point.x;
            xD = 0.0f;
        }

        if (snapY <= 0.0f) {
            snap = true;
            yD += snapY;
            snapY = 0.0f;
        } else if (snapY > bitmapHeight) {
            snap = true;
            yD -= snapY - bitmapHeight;
            snapY = bitmapHeight;
        } else {
            snapY = point.y;
            yD = 0.0f;
        }

        // Correct the distance to the snapped point.
        if (snap && xD * xD + yD * yD < minDistance.distance * minDistance.distance) {
            minDistance.x = snapX;
            minDistance.y = snapY;
            minDistance.distance = (float) Math.sqrt(xD * xD + yD * yD);
        }
        return snap;
    }

    private void adjustPath() {
        try {
            // We want to move each point in the points list to the nearest edge pixel.
            int pointListSize = pointsList.size();

            for (PointF point : pointsList.getPoints()) {
                Distance minDistance = new Distance(0.0f, 0.0f, Float.POSITIVE_INFINITY);
                if (snapToBoundary(point, 0.0f, 0.0f, minDistance)) {
                    // If we were outside the bounds already, just snap to the boundary and ignore other edges.
                    point.set(minDistance.x, minDistance.y);
                    continue;
                }

                int i = (int) (point.x / BLOCK_SIZE);
                int j = (int) (point.y / BLOCK_SIZE);

                PointsList bucket = edgePointBuckets[i][j];
                if (bucket != null) {
                    bucket.findClosestPoint(point, minDistance);
                }
                // Check if the point already happens to be on an edge (distance is 0), then we
                // don't need to do anything.
                if (minDistance.distance > 0.0f) {
                    // Otherwise, check the neighbors since they might have closer edge points
                    for (int[] neighbor : neighbors) {
                        // Calculate distance to the block border with this neighbor
                        float xD = (neighbor[0] == 0 ? 0.0f : (i * BLOCK_SIZE - point.x) + (neighbor[0] == 1 ? BLOCK_SIZE : 0.0f));
                        float yD = (neighbor[1] == 0 ? 0.0f : (j * BLOCK_SIZE - point.y) + (neighbor[1] == 1 ? BLOCK_SIZE : 0.0f));
                        if (i + neighbor[0] < 0 || i + neighbor[0] > bucketWidth - 1 ||
                            j + neighbor[1] < 0 || j + neighbor[1] > bucketHeight - 1) {
                            // Potentially snap to boundary of the bitmap, but only in the correct axis. Also fix the distances.
                            snapToBoundary(point, xD, yD, minDistance);
                        } else if (xD * xD + yD * yD < minDistance.distance * minDistance.distance) {
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
        } catch (IndexOutOfBoundsException e) {
            // Ignore the exception since the points list was probably changed.
        } catch (ConcurrentModificationException e) {
            // Ignore the exception and let the concurrent access handle everything.
        }
    }
}
