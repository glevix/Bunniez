package postpc.moriaor.bunniez;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;

public class SelectFacesActivity extends AppCompatActivity implements View.OnClickListener {

    final int THUMBSIZE = 264;


    private ImageView rightThumbnail;
    private ImageView leftThumbnail;
    private ImageView middleThumbnail;
    private ImageView selectedImage;
    private Button rightArrow;
    private Button leftArrow;

    private ArrayList<String> imagePaths;
    ArrayList<Bitmap> thumbnails;
    ArrayList<Bitmap> fullSizeImages;
    Bitmap selected;
    int selectedImageIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_faces);
        initInstances();
        setOnClickListenters();
        imagePaths = getIntent().getStringArrayListExtra("imagePaths");
        thumbnails = new ArrayList<>();
        fullSizeImages = new ArrayList<>();
        try {
            saveImagesBitmaps();
            loadImages();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initInstances() {
        rightArrow = findViewById(R.id.right_arrow);
        leftArrow = findViewById(R.id.left_arrow);
        selectedImage = findViewById(R.id.selectedImage);
        rightThumbnail = findViewById(R.id.rightImage);
        leftThumbnail = findViewById(R.id.leftImage);
        middleThumbnail = findViewById(R.id.middleImage);
    }

    private void setOnClickListenters() {
        rightArrow.setOnClickListener(this);
        leftArrow.setOnClickListener(this);
        selectedImage.setOnClickListener(this);
        rightThumbnail.setOnClickListener(this);
        leftThumbnail.setOnClickListener(this);
        middleThumbnail.setOnClickListener(this);

        selectedImage.setClipToOutline(true);
        rightThumbnail.setClipToOutline(true);
        leftThumbnail.setClipToOutline(true);
        middleThumbnail.setClipToOutline(true);

    }


    private void saveImagesBitmaps() {
        for(int i = 0; i < imagePaths.size(); i++) {
            fullSizeImages.add(BitmapFactory.decodeFile(imagePaths.get(i)));
            thumbnails.add(ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imagePaths.get(i)), THUMBSIZE, THUMBSIZE));
        }
        selected = fullSizeImages.get(0);

    }

    private void loadImages() {
        selectedImage.setImageBitmap(selected);
        for(int i = 0; i < imagePaths.size(); i++) {
            ImageView thumbnail = mapIndexToImage(i);
            if(thumbnail != null) {
                thumbnail.setImageBitmap(thumbnails.get(i));
            }
        }
    }

    @Override
    public void onClick(View v) {
        ImageView prev = mapIndexToImage(selectedImageIndex);
        prev.setBackgroundResource(R.drawable.image_background);
        switch (v.getId()) {
            case R.id.right_arrow:
                onRightArrowPress();
                break;
            case R.id.left_arrow:
                onLeftArrowPress();
                break;
            case R.id.rightImage:
            case R.id.middleImage:
            case R.id.leftImage:
                onThumbnailPress(v);
                break;
        }
        handleIndexChange();
    }

    private ImageView mapIndexToImage(int index) throws IndexOutOfBoundsException {
        switch (index) {
            case 0:
                return leftThumbnail;
            case 1:
                return middleThumbnail;
            case 2:
                return rightThumbnail;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    private int getThumbnailIndex(View v) {
        switch (v.getId()) {
            case R.id.rightImage:
                return 2;
            case R.id.middleImage:
                return 1;
            case R.id.leftImage:
                return 0;
            default:
                return -1;
        }
    }

    private void handleIndexChange() {
        ImageView currentThumbnail = mapIndexToImage(selectedImageIndex);
        currentThumbnail.setBackgroundResource(R.drawable.image_border);
        selected = fullSizeImages.get(selectedImageIndex);
        selectedImage.setImageBitmap(selected);

    }

    private void onRightArrowPress() {
        selectedImageIndex = selectedImageIndex == 2 ? 0 : selectedImageIndex + 1;
    }

    private void onLeftArrowPress() {
        selectedImageIndex = selectedImageIndex == 0 ? 2 : selectedImageIndex - 1;
    }

     private void onThumbnailPress(View thumbnail) {
        selectedImageIndex = getThumbnailIndex(thumbnail);
     }


}