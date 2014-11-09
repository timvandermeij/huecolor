package nl.liacs.huecolor;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

/*
 * Based on http://android-coding.blogspot.nl/2012/05/android-image-processing-edge-detect.html
 */
public class EdgeDetectionActivity extends Activity {
    final static int KERNEL_WIDTH = 3;
    final static int KERNEL_HEIGHT = 3;

    int[][] kernel = {
        {0, -1, 0},
        {-1, 4, -1},
        {0, -1, 0}
    };

    Bitmap source;
    ImageView imageView;
    private ProgressBar progressBar;

    private Handler handler;
    Bitmap afterProcess;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edgedetect);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        source = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setImageBitmap(source);

        handler = new Handler();
        startBackgroundProcess();
    }

    private void startBackgroundProcess() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                afterProcess = detectEdges(kernel);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        imageView.setImageBitmap(afterProcess);
                    }
                });
            }
        };
        new Thread(runnable).start();
    }

    private Bitmap detectEdges(int[][] knl) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        int WIDTH_MINUS_2 = sourceWidth - 2;
        int HEIGHT_MINUS_2 = sourceHeight - 2;

        int i, j, k, l; // Iterators
        int subSumR = 0, subSumG = 0, subSumB = 0;
        int pixel = 0, knlCache = 0;
        int x = 0, y = 0;

        // Destination bitmap
        Bitmap dest = Bitmap.createBitmap(sourceWidth, sourceHeight, source.getConfig());

        for (i = 1; i <= WIDTH_MINUS_2; i++) {
            for (j = 1; j <= HEIGHT_MINUS_2; j++) {
                // Calculate subSumRGB = subSrc[][] * knl[][]
                subSumR = subSumG = subSumB = 0;
                for (k = 0; k < KERNEL_WIDTH; k++) {
                    x = i - 1 + k;
                    for (l = 0; l < KERNEL_HEIGHT; l++) {
                        y = j - 1 + l;
                        pixel = source.getPixel(x, y);
                        knlCache = knl[k][l];
                        subSumR += Color.red(pixel) * knlCache;
                        subSumG += Color.green(pixel) * knlCache;
                        subSumB += Color.blue(pixel) * knlCache;
                    }
                }

                subSumR = clamp(subSumR, 0, 255);
                subSumG = clamp(subSumG, 0, 255);
                subSumB = clamp(subSumB, 0, 255);

                dest.setPixel(i, j, Color.argb(
                    Color.alpha(source.getPixel(i, j)),
                    subSumR,
                    subSumG,
                    subSumB)
                );
            }
        }
        return dest;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}