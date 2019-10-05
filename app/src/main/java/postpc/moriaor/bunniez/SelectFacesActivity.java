package postpc.moriaor.bunniez;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ViewAnimator;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.PicassoProvider;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

public class SelectFacesActivity extends AppCompatActivity {

    private ImageView first;
    private Uri imageUri;
    private ArrayList<String> imagePaths;
    ArrayList<Bitmap> images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_faces);
        first = findViewById(R.id.image);
        imagePaths = getIntent().getStringArrayListExtra("imagePath");
        try {
//            FileInputStream is = this.openFileInput(filename);
//            bmp = BitmapFactory.decodeStream(is);
//            is.close();
//            if (bmp != null) {
//                first.setImageBitmap(bmp);
//            }
            saveImagesBitmaps();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initInstances() {

    }


    private void saveImagesBitmaps() {
        for(int i = 0; i < imagePaths.size(); i++) {
            images.add(BitmapFactory.decodeFile(imagePaths.get(i)));
        }

    }
}