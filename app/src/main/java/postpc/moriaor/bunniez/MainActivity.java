package postpc.moriaor.bunniez;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_GALLERY = 0;
    static final int HTTP_LOADER_REQUEST = 221;
    static final int EXTERNAL_STORAGE_PERMISSION_CODE = 122;
    static final int MY_CAMERA_PERMISSION_CODE = 0;
    static final int PIC_NUM_LIMIT = 3;
    private int PIC_NUM = 1;

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
        setContentView(R.layout.activity_main);
        Bunniez bunniez = (Bunniez) getApplicationContext();
        bunniez.initNewClient();
        BunniezClient client = bunniez.getClient();
        mainUtils = new MainUtils(this);
        imagePaths = new ArrayList<>();
        Button cameraButton = findViewById(R.id.camera_button);
        Button galleryButton = findViewById(R.id.gallery_button);
        cameraButton.setOnClickListener(this);
        galleryButton.setOnClickListener(this);
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

    private void dispatchTakePictureIntent() {
        this.requestStoragePermission();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
        }
        else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = ImageUtils.createImageFile(this, PIC_NUM);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.i("error", "IOException");
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
                        Log.d(Bunniez.TAG, "oh no");
                    }


                }
            }
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
        loaderIntent.putExtra("display", display);
        loaderIntent.putExtra("request", request);
        loaderIntent.putStringArrayListExtra("imagePaths", imagePaths);
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
                    "Something went wrong. Please try again soon.",
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

//                    mArrayUri.add(uri);
//                    // Get the cursor
//                    Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
//                    // Move to first row
//                    cursor.moveToFirst();
//
//                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//                    imageEncoded = cursor.getString(columnIndex);
//                    imagesEncodedList.add(imageEncoded);
//                    cursor.close();
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
                photoFile = ImageUtils.createImageFile(this, PIC_NUM);
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
            PIC_NUM++;
        }

        // Proceed to upload
        this.onDoneSelection();
    }

    private void onCameraResult() {
        if(PIC_NUM >= PIC_NUM_LIMIT) {
            if(currentPhotoUri != null && currentPhotoUri.getPath() != null) {
                imagePaths.add(currentPhotoUri.getPath());
                this.onDoneSelection();
                PIC_NUM = 0;
            }
        } else {
            imagePaths.add(currentPhotoUri.getPath());
            PIC_NUM ++;
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



    private void requestStoragePermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_CODE);
            }
        }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
        else if (requestCode == EXTERNAL_STORAGE_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "storage permission granted", Toast.LENGTH_LONG).show();
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "storage permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }
    }


