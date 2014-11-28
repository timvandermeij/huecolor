package nl.liacs.huecolor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends BaseActivity {
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int REQUEST_IMAGE_GET = 101;
    private Uri fileUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String[] content = {
            getString(R.string.take_camera_photo),
            getString(R.string.browse_photo)
        };
        final Integer[] icons = {
            R.drawable.ic_camera,
            R.drawable.ic_browse_photo
        };
        IconListView adapter = new IconListView(MainActivity.this, content, icons);
        ListView list = (ListView)findViewById(R.id.listView);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent;

                switch ((int)id) {
                    case 0: // Take camera photo
                        // Create Intent to take a picture and return control to the calling application
                        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                        try {
                            fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // Create a file to save the image
                        } catch (Exception ignored) {
                        }
                        if (fileUri == null) {
                            Toast.makeText(MainActivity.this, "Could not create an image file for photo", Toast.LENGTH_LONG).show();
                            return;
                        }

                        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // Set the image file name

                        // Start the image capture intent
                        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                        break;

                    case 1: // Browse photo
                        intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(intent, REQUEST_IMAGE_GET);
                        }
                        break;

                    default:
                        break;
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("fileUri", (fileUri == null ? null : fileUri.toString()));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String uri = savedInstanceState.getString("fileUri");
        fileUri = (uri == null ? null : Uri.parse(uri));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Handle the saved camera photo file
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Image captured and saved to file URI specified in the intent
                addFileUriToGallery(fileUri);
                Intent intent = new Intent("nl.liacs.huecolor.select", fileUri, MainActivity.this, SelectionActivity.class);
                startActivity(intent);
            } else if (resultCode != RESULT_CANCELED) {
                // Image capture failed
                Toast.makeText(this, "Image capture failed", Toast.LENGTH_LONG).show();
            }
        }
        else if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK) {
            fileUri = data.getData();
            Intent intent = new Intent("nl.liacs.huecolor.select", fileUri, MainActivity.this, SelectionActivity.class);
            startActivity(intent);
        }
    }
}