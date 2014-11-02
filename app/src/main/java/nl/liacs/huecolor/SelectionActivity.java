package nl.liacs.huecolor;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;

public class SelectionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        ImageView image = (ImageView)findViewById(R.id.imageView);
        Drawable drawable = getResources().getDrawable(R.drawable.example);
        image.setImageDrawable(drawable);
    }
}