package nl.liacs.huecolor;

import android.app.Activity;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.Toast;

public class SelectionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        // Show the image.
        ImageView image = (ImageView)findViewById(R.id.imageView);
        Drawable drawable = getResources().getDrawable(R.drawable.example);
        image.setImageDrawable(drawable);

        // Convert the image to grayscale.
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        image.setColorFilter(filter);
    }

    public boolean onTouchEvent(MotionEvent e)
    {
        int x = (int) e.getX();
        int y = (int) e.getY();
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            Toast.makeText(SelectionActivity.this, "X: " + x + ", Y: " + y, Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}