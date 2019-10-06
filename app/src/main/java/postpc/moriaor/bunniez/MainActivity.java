package postpc.moriaor.bunniez;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    static final String CURRENT_PATH_KEY = "currentPath";
    static final String TITLE_KEY = "title";
    static final String PIC_COUNT_KEY = "picCount";

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_GALLERY = 0;
    static final int HTTP_LOADER_REQUEST = 221;
    static final int EXTERNAL_STORAGE_PERMISSION_CODE = 122;
    static final int MY_CAMERA_PERMISSION_CODE = 0;
    static final int ALL_PERMISSION_CODE = 47;
    static final int PIC_NUM_LIMIT = 3;


    private int currentPicCount = 1;
    Bunniez bunniez;
    BunniezClient client;
    private Uri currentPhotoUri;
    private ArrayList<String> imagePaths;
    MainUtils mainUtils;


    public static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bunniez = (Bunniez) getApplicationContext();
        bunniez.initNewClient();
        imagePaths = new ArrayList<>();
        setContentView(R.layout.activity_main);
        client = bunniez.getClient();
        mainUtils = new MainUtils(this);
        String title = getIntent().getStringExtra(TITLE_KEY);
        if(title != null) {
            setTitle(title);
        }
        Button cameraButton = findViewById(R.id.camera_button);
        Button galleryButton = findViewById(R.id.gallery_button);
        cameraButton.setOnClickListener(this);
        galleryButton.setOnClickListener(this);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        imagePaths = savedInstanceState.getStringArrayList(Bunniez.IMAGE_PATHS_KEY);
        String clientId = savedInstanceState.getString(Bunniez.CLIENT_ID_KET);
        String currentImagePath = savedInstanceState.getString(CURRENT_PATH_KEY);
        String title = savedInstanceState.getString(TITLE_KEY);
        currentPicCount = savedInstanceState.getInt(PIC_COUNT_KEY);
        currentPhotoUri = Uri.parse(currentImagePath);
        if(clientId != null) {
            bunniez.reinitClient(clientId);
        } else {
            bunniez.initNewClient();
        }
        if(title != null) {
            setTitle(title);
        }
    }

    private void setTitle(String title) {
        TextView titleView = findViewById(R.id.welcome_title);
        TextView subtitleView = findViewById(R.id.lets_start);
        titleView.setText(title);
        subtitleView.setVisibility(View.INVISIBLE);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.camera_button:
               dispatchTakePictureIntent();
                break;

            case R.id.gallery_button:
                dispatchSelectPictureIntent();
                break;
        }
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    private void doRequestPermissions() {
        ArrayList<String> required = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            required.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            required.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            required.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        String[] requests = new String[required.size()];
        for (int i = 0; i < required.size(); i++) {
            requests[i] = required.get(i);
        }
        ActivityCompat.requestPermissions(this,
                requests,
                ALL_PERMISSION_CODE);

    }

    private void dispatchTakePictureIntent() {
        if (checkPermissions()) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = ImageUtils.createImageFile(this, currentPicCount);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.d(Bunniez.TAG, "IOException");
                }

                if (photoFile != null && photoFile.exists()) {
                    try {
                        Uri photoURI = FileProvider.getUriForFile(this,
                                BuildConfig.APPLICATION_ID + ".provider",
                                photoFile);
                        grantUriPermission(getPackageName(), photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        currentPhotoUri = Uri.parse(photoFile.getAbsolutePath());

                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

                    } catch (Exception e) {
                        Log.d(Bunniez.TAG, "Exception was thrown while opening camera");
                    }


                }
            }
        } else {
            doRequestPermissions();
        }
    }


    private void dispatchSelectPictureIntent() {
        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        if (galleryIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(galleryIntent, "Select Picture"), REQUEST_GALLERY);
        }
    }


    private void onDoneSelection() {
        this.startLoaderActivity(getString(R.string.loader_upload), RequestTypes.UPLOAD);
    }

    private void startLoaderActivity(String display, String request) {
        Intent loaderIntent = new Intent(this, LoaderActivity.class);
        loaderIntent.putExtra(LoaderActivity.DISPLAY_KEY, display);
        loaderIntent.putExtra(LoaderActivity.REQUEST_KEY, request);
        loaderIntent.putStringArrayListExtra(Bunniez.IMAGE_PATHS_KEY, imagePaths);
        if(loaderIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(loaderIntent, HTTP_LOADER_REQUEST);
        }
    }


    private void onHttpResult(int resultCode, Intent data) {
        if(resultCode == RESULT_CANCELED) {
            String text = data.getDataString();
            Runnable retry = new Runnable() {
                @Override
                public void run() {
                    //
                }
            };
            mainUtils.popAlertDialog(retry, text,
                    getString(R.string.error_message),
                    "OK",
                    "Cancel");
        }

    }


    private void onGalleryResult(Intent data) {
        // https://stackoverflow.com/questions/39225901/how-to-open-gallery-to-select-multiple-image
        String imageEncoded;
        List<String> imagesEncodedList;
        ArrayList<File> imageFiles = new ArrayList<>();

        // Get the paths of the images chosen
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        imagesEncodedList = new ArrayList<String>();
        if(data.getData()!=null){
            // TODO: ERROR only 1 image, start again
        }else {
            if (data.getClipData() != null) {
                ClipData mClipData = data.getClipData();
                ArrayList<Uri> mArrayUri = new ArrayList<Uri>();
                for (int i = 0; i < mClipData.getItemCount(); i++) {

                    ClipData.Item item = mClipData.getItemAt(i);
                    Uri uri = item.getUri();
                    File f = null;
                    try {
                        f = FileUtil.from(MainActivity.this,uri);
                    } catch (IOException e) {
                        f= null;
                    }
                    if (f == null || !f.exists()) {
                        // TODO: error getting file from gallery
                    }
                    imageFiles.add(f);
                }
            }
        }

        // Make sure we got the right number of images
        if (imagesEncodedList.size() != 3) {
            // TODO: ERROR didnt choose 3 images, start again
        }

        // Copy the images into out working directory and add paths to imagePaths
        for (File fin : imageFiles) {
            File photoFile = null;
            try {
                photoFile = ImageUtils.createImageFile(this, currentPicCount);
                if (photoFile != null && photoFile.exists()) {
                    copyFile(fin, photoFile);
                } else {
                    //TODO: error
                }
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.i("error", "IOException");
            }
            imagePaths.add(photoFile.getAbsolutePath());
            currentPicCount++;
        }

        // Proceed to upload
        this.onDoneSelection();
    }

    private void onCameraResult() {
        if(currentPicCount >= PIC_NUM_LIMIT) {
            if(currentPhotoUri != null && currentPhotoUri.getPath() != null) {
                imagePaths.add(currentPhotoUri.getPath());
                this.onDoneSelection();
                currentPicCount = 0;
            }
        } else {
            imagePaths.add(currentPhotoUri.getPath());
            currentPicCount++;
            this.dispatchTakePictureIntent();
        }
    }



    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            this.onCameraResult();
        }
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            this.onGalleryResult(data);
        }
        if (requestCode == HTTP_LOADER_REQUEST) {
            this.onHttpResult(resultCode, data);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
//                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
        else if (requestCode == EXTERNAL_STORAGE_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "storage permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "storage permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(Bunniez.IMAGE_PATHS_KEY, imagePaths);
        if (client != null) {
            outState.putString(Bunniez.CLIENT_ID_KET, client.id);
        }

        outState.putInt(PIC_COUNT_KEY, currentPicCount);
        if (currentPhotoUri != null) {
            outState.putString(CURRENT_PATH_KEY, currentPhotoUri.getPath());
        }
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }
}


