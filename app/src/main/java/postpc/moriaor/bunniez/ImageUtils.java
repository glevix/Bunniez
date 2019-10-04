package postpc.moriaor.bunniez;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        String filename = "input" + index;
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir + File.separator + filename + ".jpg");
        if (!image.exists()) {
            try {
                if (image.createNewFile())
                    return image;
                else
                    return null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        File image = File.createTempFile(
//                filename,  /* prefix */
//                ".jpg",         /* suffix */
//                storageDir      /* directory */
//        );
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

}
