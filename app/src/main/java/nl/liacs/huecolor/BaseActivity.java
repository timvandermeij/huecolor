package nl.liacs.huecolor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BaseActivity extends Activity {
    protected static final int MEDIA_TYPE_IMAGE = 1;

    /**
     * Create a file URI for saving an image or video
     */
    protected Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a file for saving an image or video
     */
    protected File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "HueColor");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                // Use an internal directory
                mediaStorageDir = getFilesDir();
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        if (type == MEDIA_TYPE_IMAGE) {
            return new File(mediaStorageDir.getPath() + File.separator +
                            "IMG_"+ timeStamp + ".jpg");
        }
        return null;
    }

    protected void addFileToGallery(File f) {
        Uri contentUri = Uri.fromFile(f);
        addFileUriToGallery(contentUri);
    }

    protected void addFileUriToGallery(Uri contentUri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
}
