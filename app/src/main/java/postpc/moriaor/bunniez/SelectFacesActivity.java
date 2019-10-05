package postpc.moriaor.bunniez;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
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

    final int THUMBSIZE = 128;


    private ImageView rightThumbnail;
    private ImageView leftThumbnail;
    private ImageView middleThumbnail;
    private ImageView selectedImage;

    private ArrayList<String> imagePaths;
    ArrayList<Bitmap> images;
    Bitmap selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_faces);
        initInstances();
        imagePaths = getIntent().getStringArrayListExtra("imagePaths");
        images = new ArrayList<>();
        try {
            saveImagesBitmaps();
            loadImages();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initInstances() {
        selectedImage = findViewById(R.id.selectedImage);
        rightThumbnail = findViewById(R.id.rightImage);
        leftThumbnail = findViewById(R.id.leftImage);
        middleThumbnail = findViewById(R.id.middleImage);
    }


    private void saveImagesBitmaps() {
        selected = BitmapFactory.decodeFile(imagePaths.get(0));
        for(int i = 0; i < imagePaths.size(); i++) {
            images.add(ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imagePaths.get(i)), THUMBSIZE, THUMBSIZE));
        }
    }

    private void loadImages() {
        selectedImage.setImageBitmap(selected);
        for(int i = 0; i < imagePaths.size(); i++) {
            ImageView thumbnail = mapIndexToImage(i);
            if(thumbnail != null) {
                thumbnail.setImageBitmap(images.get(i));
            }
        }
    }

    private ImageView mapIndexToImage(int index) {
        switch (index) {
            case 0:
                return leftThumbnail;
            case 1:
                return middleThumbnail;
            case 2:
                return rightThumbnail;
            default:
                return null;
        }
    }

}