package nl.liacs.huecolor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the button for taking a camera photo and add a listener to open the camera
        Button camera = (Button)findViewById(R.id.camera);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            // Create Intent to take a picture and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            try {
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // Create a file to save the image
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Could not create an image file for photo", Toast.LENGTH_LONG).show();
                return;
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // Set the image file name

            // Start the image capture intent
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        });

        Button browse = (Button)findViewById(R.id.browse);
        browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File mPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString());
                FileDialog fileDialog = new FileDialog(MainActivity.this, mPath);
                fileDialog.setFileEndsWith(".jpg");
                fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
                    public void fileSelected(File file) {
                    Log.d("HueColor", "selected file " + file.toString());
                    fileUri = Uri.fromFile(file);
                    }
                });
                fileDialog.showDialog();
            }
        });
    }

    /*
     * Create a file URI for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /*
     * Create a file for saving an image or video
     */
    private static File getOutputMediaFile(int type){
        // TODO: check that the SDCard is mounted using Environment.getExternalStorageState()
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES), "HueColor");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("HueColor", "Failed to create directory");
                return null;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Handle the saved camera photo file
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Image captured and saved to file URI specified in the intent
                Toast.makeText(this, "Image saved to:\n" + fileUri, Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                // Image capture failed
                Toast.makeText(this, "Image capture failed", Toast.LENGTH_LONG).show();
            }
        }
    }
}