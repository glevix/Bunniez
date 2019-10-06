package postpc.moriaor.bunniez;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

public class SelectFacesActivity extends AppCompatActivity implements View.OnClickListener {

    final int THUMBSIZE = 264;

    static final String CHOSEN_IMAGES_KEY = "chosenImages";


    ArrayList<ImageView> thumbnailViews;
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
    ArrayList<Integer> chosenImagesForBoxes;
    ArrayList<Button> boxesButtons;


    Bitmap selected;
    int selectedImageIndex;
    boolean didInitBoxes;
    ArrayList<BoundingBox> currentBoxesList;

    int viewX;
    int viewY;
    int viewWidth;
    int viewHeight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_faces);
        imagePaths = getIntent().getStringArrayListExtra(Bunniez.IMAGE_PATHS_KEY);
        initInstances();
        initLayoutListener();
        setOnClickListeners();
        try {
            saveImagesBitmaps();
            loadImages();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(Bunniez.TAG, "Error in SelectFaces --> onCreate");
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        imagePaths = savedInstanceState.getStringArrayList(Bunniez.IMAGE_PATHS_KEY);
        chosenImagesForBoxes = savedInstanceState.getIntegerArrayList(CHOSEN_IMAGES_KEY);

    }

    private void initLayoutListener() {
        ViewTreeObserver vto = selectedImage.getViewTreeObserver();
        final ViewTreeObserver.OnGlobalLayoutListener listener = new ViewTreeObserver.OnGlobalLayoutListener(){
            @Override public void onGlobalLayout(){
                int [] xy = new int[2];
                selectedImage.getLocationOnScreen(xy);
                viewX = xy[0];
                viewY = xy[1];
                viewWidth = selectedImage.getWidth();
                viewHeight = selectedImage.getHeight();
                if(didInitBoxes) {
                    runOnUiThread(drawBoundingBoxes());
                }
                selectedImage.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        };
        vto.addOnGlobalLayoutListener(listener);
    }


    private void initInstances() {
        Bunniez bunniez = (Bunniez) getApplicationContext();
        client = bunniez.getClient();
        thumbnails = new ArrayList<>();
        fullSizeImages = new ArrayList<>();
        currentBoxesList = client.boxes.get(selectedImageIndex);

        rightArrow = findViewById(R.id.right_arrow);
        leftArrow = findViewById(R.id.left_arrow);
        selectedImage = findViewById(R.id.selectedImage);
        doneButton = findViewById(R.id.done_button);

        initThumbnailViews();
        initBoxesButtons();


    }

    private void initThumbnailViews() {
        thumbnailViews = new ArrayList<>();
        rightThumbnail = findViewById(R.id.rightImage);
        leftThumbnail = findViewById(R.id.leftImage);
        middleThumbnail = findViewById(R.id.middleImage);
        for(int i = 0; i< imagePaths.size(); i++) {
            ImageView thumb = mapIndexToImage(i);
            thumb.setOnClickListener(this);
            thumb.setClipToOutline(true);
            thumbnailViews.add(thumb);

        }
    }

    private void initBoxesButtons() {
        chosenImagesForBoxes = new ArrayList<>();
        boxesButtons = new ArrayList<>();
        for(int i = 0; i< currentBoxesList.size(); i++) {
            chosenImagesForBoxes.add(-1);
            Button box = new Button(this);
            box.setClickable(true);
            box.setOnClickListener(this);
            box.setId(i);
            boxesButtons.add(box);
        }
        didInitBoxes = true;
    }

    private void setOnClickListeners() {
        rightArrow.setOnClickListener(this);
        leftArrow.setOnClickListener(this);
        doneButton.setOnClickListener(this);

        selectedImage.setClipToOutline(true);
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
            ImageView thumbnail = thumbnailViews.get(i);
            if(thumbnail != null) {
                thumbnail.setImageBitmap(thumbnails.get(i));
            }
        }
    }


    private BoundingBox translateBoundingBox(BoundingBox box) {
        BoundingBox out = new BoundingBox();
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
                    if(chosenImagesForBoxes.get(i) == selectedImageIndex) {
                        box.setBackgroundResource(R.drawable.bounding_box);
                    } else {
                        box.setBackgroundResource(R.drawable.image_background);
                    }
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
        runOnUiThread(drawBoundingBoxes());
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

     private boolean validateSelection() {
        for(int i = 0; i < chosenImagesForBoxes.size(); i++) {
            if(chosenImagesForBoxes.get(i) == -1) {
                Toast.makeText(this, getString(R.string.select_all_toast), Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
     }

     private void onDone() {
        if(!validateSelection()) return;
         Intent loaderIntent = new Intent(this, LoaderActivity.class);
         loaderIntent.putExtra(LoaderActivity.DISPLAY_KEY, getString(R.string.loader_prepare));
         loaderIntent.putExtra(LoaderActivity.REQUEST_KEY, RequestTypes.PROCESS);
         loaderIntent.putStringArrayListExtra(Bunniez.IMAGE_PATHS_KEY, imagePaths);
         loaderIntent.putIntegerArrayListExtra(LoaderActivity.CHOSEN_KEY, chosenImagesForBoxes);
         if(loaderIntent.resolveActivity(getPackageManager()) != null) {
             startActivity(loaderIntent);
         }
     }



     private void onBoundingBoxPress(View v) {
         int boxIndex = boxesButtons.indexOf(v);
        if(boxIndex != -1 && boxIndex < chosenImagesForBoxes.size()) {
            v.setBackgroundResource(R.drawable.bounding_box);
            chosenImagesForBoxes.set(boxIndex, selectedImageIndex);
        }
     }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(Bunniez.IMAGE_PATHS_KEY, imagePaths);
        outState.putIntegerArrayList(CHOSEN_IMAGES_KEY, chosenImagesForBoxes);

        super.onSaveInstanceState(outState);
    }

}