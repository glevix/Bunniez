package postpc.moriaor.bunniez;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_GALLERY = 0;
    static final int HTTP_LOADER_REQUEST = 221;
    static final int EXTERNAL_STORAGE_PERMISSION_CODE = 122;
    static final int MY_CAMERA_PERMISSION_CODE = 0;
    static final int PIC_NUM_LIMIT = 1;
    private int PIC_NUM = 1;

    private Bitmap mImageBitmap;
    private Uri currentPhotoUri;

    private String firstImagePath;
    private String secondImagePath;
    private String thirdImagePath;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        else
        {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            File photoFile = null;
            try {
                photoFile = ImageUtils.createImageFile(this, PIC_NUM);
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.i("error", "IOException");
            }
            // Continue only if the File was successfully created
            if (photoFile != null && takePictureIntent.resolveActivity(getPackageManager()) != null) {
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                        BuildConfig.APPLICATION_ID + ".provider", photoFile);
//                getApplicationContext().grantUriPermission(getApplicationContext().getPackageName(), photoURI, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                currentPhotoUri = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
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

    private void loadImagesPathsToIntent(Intent intent) {
        intent.putExtra("firstImagePath", firstImagePath);
        intent.putExtra("secondImagePath", secondImagePath);
        intent.putExtra("thirdImagePath", thirdImagePath);
    }

    private void onDoneSelection() {
        this.startLoaderActivity("processing...", "preprocess");
    }

    private void startLoaderActivity(String display, String request) {
        Intent loaderIntent = new Intent(this, LoaderActivity.class);
        loaderIntent.putExtra("display", display);
        loaderIntent.putExtra("request", request);
        loadImagesPathsToIntent(loaderIntent);
        if(loaderIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(loaderIntent, HTTP_LOADER_REQUEST);
        }
    }


    private void onHttpResult() {

    }


    private void onGalleryResult() {

    }

    private void onCameraResult() {
        if(PIC_NUM >= PIC_NUM_LIMIT) {
            this.saveCapturedImagePath();
            this.onDoneSelection();
            PIC_NUM = 0;
        } else {
            this.saveCapturedImagePath();
            PIC_NUM ++;
            this.dispatchTakePictureIntent();
        }
    }

    private void saveCapturedImagePath() {
        switch (PIC_NUM) {
            case 0:
                firstImagePath = currentPhotoUri.getPath();
                break;
            case 1:
                secondImagePath = currentPhotoUri.getPath();
                break;
            case 2:
                thirdImagePath = currentPhotoUri.getPath();
                break;
        }
    }



    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            this.onCameraResult();
        }
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            this.onGalleryResult();
        }
        if (requestCode == HTTP_LOADER_REQUEST && resultCode == Activity.RESULT_OK) {
            this.onHttpResult();
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


