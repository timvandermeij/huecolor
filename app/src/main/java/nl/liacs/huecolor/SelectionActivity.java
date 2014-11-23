package nl.liacs.huecolor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;

/**
 * Based on https://code.google.com/p/apidemos/source/browse/trunk/ApiDemos/src/com/example/android/apis/graphics/FingerPaint.java
 */
public class SelectionActivity extends Activity {
    private Button filterButton;
    private SelectionView initialView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = this.getIntent();
        String action = intent.getAction();
        Uri fileUri = null;
        if (action.equals("nl.liacs.huecolor.select")) {
            fileUri = intent.getData();
        }

        // Use a custom view for the image.
        initialView = new SelectionView(this, fileUri, 0);
        setContentView(initialView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.filter_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invertFilter:
                initialView.applyFilter(1);
                break;
            case R.id.sepiaFilter:
                initialView.applyFilter(2);
                break;
            case R.id.grayscaleFilter:
                initialView.applyFilter(3);
                break;
            case R.id.snowFilter:
                initialView.applyFilter(4);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
}