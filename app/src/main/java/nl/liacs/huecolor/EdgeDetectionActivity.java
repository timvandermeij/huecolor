package nl.liacs.huecolor;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

/*
 * Based on http://android-coding.blogspot.nl/2012/05/android-image-processing-edge-detect.html
 */

public class EdgeDetectionActivity extends Activity {

    final static int KERNEL_WIDTH = 3;
    final static int KERNEL_HEIGHT = 3;

    int[][] kernel ={
            {0, -1, 0},
            {-1, 4, -1},
            {0, -1, 0}
    };

    Bitmap bitmap_Source;
    ImageView imageView;
    private ProgressBar progressBar;

    private Handler handler;
    Bitmap afterProcess;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //This is very, very bad.
        //Would like here the view this activity is called from
        //setContentView(new SelectionView(this));
        setContentView(R.layout.activity_edgedetect);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        bitmap_Source = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setImageBitmap(bitmap_Source);

        handler = new Handler();
        startBackgroundProcess();
    }

    private void startBackgroundProcess() {

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                afterProcess = processingBitmap(bitmap_Source, kernel);

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

    private Bitmap processingBitmap(Bitmap src, int[][] knl){
        // Destination bitmap
        Bitmap dest = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());

        int bmWidth = src.getWidth();
        int bmHeight = src.getHeight();
        int bmWidth_MINUS_2 = bmWidth - 2;
        int bmHeight_MINUS_2 = bmHeight - 2;

        for (int i = 1; i <= bmWidth_MINUS_2; i++) {
            for (int j = 1; j <= bmHeight_MINUS_2; j++) {
                // Get the surrounding 3*3 pixels of current src[i][j] pixel in a matrix subSrc[][]
                int[][] subSrc = new int[KERNEL_WIDTH][KERNEL_HEIGHT];
                for (int k = 0; k < KERNEL_WIDTH; k++) {
                    for (int l = 0; l < KERNEL_HEIGHT; l++) {
                        subSrc[k][l] = src.getPixel(i - 1 + k, j - 1 + l);
                    }
                }

                // Calculate subSumRGB = subSrc[][] * knl[][]
                int subSumR = 0;
                int subSumG = 0;
                int subSumB = 0;

                for (int k = 0; k < KERNEL_WIDTH; k++) {
                    for (int l = 0; l < KERNEL_HEIGHT; l++) {
                        subSumR += Color.red(subSrc[k][l]) * knl[k][l];
                        subSumG += Color.green(subSrc[k][l]) * knl[k][l];
                        subSumB += Color.blue(subSrc[k][l]) * knl[k][l];
                    }
                }

                int subSumA = Color.alpha(src.getPixel(i, j));

                if (subSumR <0) {
                    subSumR = 0;
                }
                else if (subSumR > 255) {
                    subSumR = 255;
                }

                if (subSumG <0) {
                    subSumG = 0;
                }
                else if (subSumG > 255) {
                    subSumG = 255;
                }

                if (subSumB <0) {
                    subSumB = 0;
                }
                else if (subSumB > 255) {
                    subSumB = 255;
                }

                dest.setPixel(i, j, Color.argb(
                        subSumA,
                        subSumR,
                        subSumG,
                        subSumB));
            }
        }

        return dest;
    }

}