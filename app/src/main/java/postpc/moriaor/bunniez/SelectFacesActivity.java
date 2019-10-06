package postpc.moriaor.bunniez;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;

public class SelectFacesActivity extends AppCompatActivity implements View.OnClickListener {

    final int THUMBSIZE = 264;


    private ImageView rightThumbnail;
    private ImageView leftThumbnail;
    private ImageView middleThumbnail;
    private ImageView selectedImage;
    private Button rightArrow;
    private Button leftArrow;
    private Button doneButton;


    BunniezClient client;
    ArrayList<String> imagePaths;
    ArrayList<Bitmap> thumbnails;
    ArrayList<Bitmap> fullSizeImages;
    ArrayList<Integer> chosenBoxes;
    ArrayList<Button> boxesButtons;

    ViewTreeObserver.OnGlobalLayoutListener listener;

    Bitmap selected;
    int selectedImageIndex;

    ArrayList<BoundingBox> currentBoxesList;

    int xOffset;
    int yOffset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_faces);
        initInstances();
        setOnClickListenters();
        imagePaths = getIntent().getStringArrayListExtra("imagePaths");

        try {
            saveImagesBitmaps();
            loadImages();
            ViewTreeObserver vto = selectedImage.getViewTreeObserver();
            listener = new ViewTreeObserver.OnGlobalLayoutListener(){
                @Override public void onGlobalLayout(){
                    int [] location = new int[2];
                    selectedImage.getLocationOnScreen(location);
                    xOffset = location[0];
                    yOffset = location[1];
                    Log.i("hell", "main image x offset: " + xOffset);
                    Log.i("hell", "main image y offset: " + yOffset);
                    runOnUiThread(drawBoundingBoxes());
                }
            };
            vto.addOnGlobalLayoutListener(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initInstances() {
        Bunniez bunniez = (Bunniez) getApplicationContext();
        client = bunniez.getClient();
        thumbnails = new ArrayList<>();
        fullSizeImages = new ArrayList<>();
        currentBoxesList = client.boxes.get(selectedImageIndex);
        xOffset = 0;
        yOffset = 0;

        rightArrow = findViewById(R.id.right_arrow);
        leftArrow = findViewById(R.id.left_arrow);
        selectedImage = findViewById(R.id.selectedImage);
        rightThumbnail = findViewById(R.id.rightImage);
        leftThumbnail = findViewById(R.id.leftImage);
        middleThumbnail = findViewById(R.id.middleImage);
        doneButton = findViewById(R.id.done_button);

        initBoxesButtons();
    }

    private void initBoxesButtons() {
        boxesButtons = new ArrayList<>();
        for(int i = 0; i< currentBoxesList.size(); i++) {
            Button box = new Button(this);
            box.setOnClickListener(this);
            box.setId(i);
            boxesButtons.add(box);
        }
    }

    private void setOnClickListenters() {
        rightArrow.setOnClickListener(this);
        leftArrow.setOnClickListener(this);
        selectedImage.setOnClickListener(this);
        rightThumbnail.setOnClickListener(this);
        leftThumbnail.setOnClickListener(this);
        middleThumbnail.setOnClickListener(this);
        doneButton.setOnClickListener(this);

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


    private BoundingBox translateBoundingBox(BoundingBox box) {
        BoundingBox out = new BoundingBox();
        int [] xy = new int[2];
        selectedImage.getLocationOnScreen(xy);
        int viewX = xy[0];
        int viewY = xy[1];
        int viewWidth = selectedImage.getWidth();
        int viewHeight = selectedImage.getHeight();
        int imWidth = selected.getWidth();
        int imHeight = selected.getHeight();
        float wScaleFactor = (float)viewWidth / (float)imWidth;
        float hScaleFactor = (float)viewHeight / (float)imHeight;
        out.x = (int) (viewX + (box.x * wScaleFactor));
        out.y = (int) (viewY / 2 + (box.y * hScaleFactor));
        out.w = (int) (box.w * wScaleFactor);
        out.h = (int) (box.h * hScaleFactor);
        return out;
    }

    private Runnable drawBoundingBoxes() {
        return new Runnable() {
            @Override
            public void run() {
                ConstraintLayout container = SelectFacesActivity.this.findViewById(R.id.container);
                removeButtons(container);
                for(int i = 0; i < boxesButtons.size(); i++) {
                    BoundingBox translatedBox = translateBoundingBox(currentBoxesList.get(i));
                    int x = translatedBox.x;
                    int y = translatedBox.y;
                    int h = translatedBox.h;
                    int w = translatedBox.w;
                    Button box = boxesButtons.get(i);
                    box.setBackgroundResource(R.drawable.bounding_box);
                    box.setX(x);
                    box.setY(y);
                    box.setHeight(h);
                    box.setWidth(w);
                    container.addView(box);
                    box.setVisibility(View.VISIBLE);
                }
            }
        };
    }

    private void removeButtons(ConstraintLayout container) {
        for(int i = 0; i < boxesButtons.size(); i++) {
            container.removeView(boxesButtons.get(i));
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
            case R.id.done_button:
                onDone();
                break;
            case 0:
            case 1:
            case 2:
                onBoundingBoxPress(v);
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
        currentBoxesList = client.boxes.get(selectedImageIndex);
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

     private void onDone() {
         Intent loaderIntent = new Intent(this, LoaderActivity.class);
         loaderIntent.putExtra("display", getString(R.string.loader_prepare));
         loaderIntent.putExtra("request", RequestTypes.PROCESS);
         loaderIntent.putStringArrayListExtra("imagePaths", imagePaths);
         loaderIntent.putIntegerArrayListExtra("indices", chosenBoxes);
         if(loaderIntent.resolveActivity(getPackageManager()) != null) {
             startActivityForResult(loaderIntent, MainActivity.HTTP_LOADER_REQUEST);
         }
     }

     private void onBoundingBoxPress(View v) {
        int boxIndex = boxesButtons.indexOf(v);
        chosenBoxes.set(boxIndex, selectedImageIndex);
     }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        selectedImage.getViewTreeObserver().removeOnGlobalLayoutListener(listener);

    }
}