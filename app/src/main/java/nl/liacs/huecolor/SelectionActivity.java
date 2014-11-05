package nl.liacs.huecolor;

import android.app.Activity;
import android.os.Bundle;

/**
 * Based on https://code.google.com/p/apidemos/source/browse/trunk/ApiDemos/src/com/example/android/apis/graphics/FingerPaint.java
 */
public class SelectionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use a custom view for the image.
        SelectionView initialView = new SelectionView(this);
        setContentView(initialView);
    }
}