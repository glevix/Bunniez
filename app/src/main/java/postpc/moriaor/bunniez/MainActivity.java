package postpc.moriaor.bunniez;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_GALLERY = 0;
    static final int EXTERNAL_STORAGE_PERMISSION_CODE = 122;
    static final int MY_CAMERA_PERMISSION_CODE = 0;
    static final int PIC_NUM_LIMIT = 1;
    private int PIC_NUM = 1;

    private Bitmap mImageBitmap;
    private Uri currentPhotoUri;




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
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.i("error", "IOException");
            }
            // Continue only if the File was successfully created
            if (photoFile != null && takePictureIntent.resolveActivity(getPackageManager()) != null) {
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                        BuildConfig.APPLICATION_ID + ".provider", photoFile);
                getApplicationContext().grantUriPermission(getApplicationContext().getPackageName(), photoURI, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                currentPhotoUri = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }

    }


    private void dispatchSelectPictureIntent() {
        Intent galleryIntent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (galleryIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(galleryIntent, REQUEST_GALLERY);

        }
    }

    private void startLoaderActivity(String display, String postUrl) {
        Intent loaderIntent = new Intent(this, LoaderActivity.class);
        loaderIntent.putExtra("display", display);
        loaderIntent.putExtra("postUrl", postUrl);
        if(loaderIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(loaderIntent);
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




    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
                try {
                    mImageBitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), currentPhotoUri);
                    if(mImageBitmap != null) {
                        this.processImage(mImageBitmap);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (PIC_NUM < PIC_NUM_LIMIT) {
                PIC_NUM++;
                dispatchTakePictureIntent();
            } else {
                PIC_NUM = 0;
                // process images
            }
        }
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {

            // process images
        }
    }


    private void processImage(Bitmap bmp) {
        try {
            //Write file
            String filename = PIC_NUM + ".jpg";
            FileOutputStream stream = this.openFileOutput(filename, Context.MODE_PRIVATE);
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream);

            //Cleanup
            stream.close();
            bmp.recycle();

            //Pop intent
            Intent selectFacesIntent = new Intent(this, SelectFacesActivity.class);
            selectFacesIntent.putExtra("image", filename);
            selectFacesIntent.putExtra("imageUri", currentPhotoUri.getPath());
            startActivity(selectFacesIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File getOutputMediaFile() {
        File mediaStorageDir = new File(getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), getString(R.string.app_name));

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        return null;

//        return new File(mediaStorageDir.getPath() + File.separator +
//                PROFILE_PIC + getString(R.string.pic_type));
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    private void requestStoragePermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_CODE);
            }
        }
    }


