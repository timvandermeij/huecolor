package nl.liacs.huecolor;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

/**
 * Somewhat based on https://stackoverflow.com/questions/18520287/draw-a-circle-on-an-existing-image.
 */
public class SelectionActivity extends Activity implements View.OnTouchListener {
    private Canvas canvas;
    private Paint paint;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        // Load the original image.
        BitmapFactory.Options myOptions = new BitmapFactory.Options();
        myOptions.inDither = true;
        myOptions.inScaled = false;
        myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        myOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.example, myOptions);
        Bitmap workingBitmap = Bitmap.createBitmap(bitmap);
        Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);

        imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setImageBitmap(mutableBitmap);

        // Convert the image to grayscale.
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        imageView.setColorFilter(filter);

        // Define the paint for the selection circles.
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);

        // Define the canvas to draw the selection on.
        canvas = new Canvas(mutableBitmap);

        // Set the touch listener.
        imageView.setOnTouchListener(this);
    }

    public boolean onTouch(View v, MotionEvent e) {
        int action = e.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            // Get coordinates of imageView as offset.
            int[] coordinates = new int[2];
            imageView.getLocationOnScreen(coordinates);

            // Draw a circle at the tapped position.
            float x = e.getX() - coordinates[0];
            float y = e.getY() - coordinates[1];
            canvas.drawCircle(x, y, 5, paint);
            imageView.invalidate();
        }
        return true;
    }
}