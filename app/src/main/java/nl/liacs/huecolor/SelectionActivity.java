package nl.liacs.huecolor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class SelectionActivity extends Activity {
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
        buildView(fileUri, savedInstanceState);
    }

    private void buildView(Uri fileUri, Bundle savedInstanceState) {
        if (savedInstanceState != null && fileUri == null) {
            String uri = savedInstanceState.getString("fileUri");
            fileUri = (uri == null ? null : Uri.parse(uri));
        }
        initialView = new SelectionView(this, fileUri, SelectionView.GRAYSCALE_FILTER);
        if (savedInstanceState != null) {
            initialView.restoreState(savedInstanceState);
        }
        setContentView(initialView);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        initialView.saveState(outState);
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        initialView = null;
        buildView(null, savedInstanceState);
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
                initialView.applyFilter(SelectionView.INVERT_FILTER);
                break;
            case R.id.sepiaFilter:
                initialView.applyFilter(SelectionView.SEPIA_FILTER);
                break;
            case R.id.snowFilter:
                initialView.applyFilter(SelectionView.SNOW_FILTER);
                break;
            case R.id.grayscaleFilter:
                initialView.applyFilter(SelectionView.GRAYSCALE_FILTER);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
}