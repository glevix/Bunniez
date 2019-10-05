package postpc.moriaor.bunniez;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class ImageUtils {

    static Bitmap getBitmapFromUri(Context context, Uri imageUri) {
        Bitmap bmp = null;
        try {
            bmp = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bmp;
    }

    static File createImageFile(Context context, int index) throws IOException {
        // Create an image file name
        String filename = "input" + index + ".jpg";
        String dirPath = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        File image = new File(dirPath + File.separator + filename);
        if (image.exists())
            image.delete();
        image.createNewFile();
        if (!image.exists())
            return null;
        return image;
    }

    static boolean saveToGallery(Context context, File file) {
        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        return true;
    }

    static File createImageFile(Context context, String name) throws IOException {
        // Create an image file name
        String filename = name + ".jpg";
        String dirPath = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        File image = new File(dirPath + File.separator + filename);
        if (image.exists())
            image.delete();
        image.createNewFile();
        if (!image.exists())
            return null;
        return image;
    }

    static void processImage(String filename, Context context, Bitmap bmp) {
        try {
            //Write file
            FileOutputStream stream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);

            //Cleanup
            stream.close();
            bmp.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static float resolveAspectFactor(int imageViewHeight, int imageViewWidth, int origHeight, int origWidth){
        float newAspectFactor = 0;


        if (imageViewHeight > imageViewWidth) {
            //Aspect factor for Height
            newAspectFactor = imageViewHeight / origHeight;

        } else {
            //Aspect factor for Width
            newAspectFactor = imageViewWidth / origWidth;
        }

        float imgNewHeight = newAspectFactor * origHeight;
        float imgNewWidth = newAspectFactor * origWidth;
        Log.d(Bunniez.TAG, " Image New Height : " + imgNewHeight);
        Log.d(Bunniez.TAG, " Image New Width : " + imgNewWidth);

        if (imgNewHeight < imageViewHeight) {
            newAspectFactor = imageViewHeight / origHeight;
        } else if (imgNewWidth < imageViewWidth) {
            newAspectFactor = imgNewWidth / origWidth;
        }
        return newAspectFactor;

    }



}

class MainUtils {

    private Context context;
    Bunniez bunniez;
    private BunniezClient client;
    AlertDialog.Builder builder;


    MainUtils(Context context){
        this.context = context;
        bunniez = (Bunniez) context.getApplicationContext();
        client = bunniez.getClient();
        builder = new AlertDialog.Builder(context);
    }

    private boolean didConnectionFailed() {
        return bunniez.didInit && !bunniez.hasSetupConnection;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    void runWithNetworkConnection(final Runnable task) {
        if(isNetworkAvailable()) {
            task.run();
        } else {
            popAlertDialog(task, "No Internet", "Internet is required. Please Retry.", "Retry", "Cancel");
        }
    }

    void popAlertDialog(final Runnable onPositive, String title, String message,
                        String positiveButtonText, String negativeButtonText) {
        builder.setCancelable(false);
        builder.setTitle(title);
        builder.setMessage(message);

        builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                onPositive.run();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }



}
