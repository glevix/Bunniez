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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.io.File;
import java.util.ArrayList;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class SelectFacesActivity extends AppCompatActivity implements View.OnClickListener {

    final int THUMBSIZE = 264;


    private ImageView rightThumbnail;
    private ImageView leftThumbnail;
    private ImageView middleThumbnail;
    private ImageView selectedImage;
    private Button rightArrow;
    private Button leftArrow;
    private Button doneButton;
    private View leftBox;

    BunniezClient client;
    ArrayList<String> imagePaths;
    ArrayList<Bitmap> thumbnails;
    ArrayList<Bitmap> fullSizeImages;
    ArrayList<Integer> chosenBoxes;
    Bitmap selected;
    int selectedImageIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_faces);
        initInstances();
        setOnClickListenters();
        imagePaths = getIntent().getStringArrayListExtra("imagePaths");
        Bunniez bunniez = (Bunniez) getApplicationContext();
        client = bunniez.getClient();
        thumbnails = new ArrayList<>();
        fullSizeImages = new ArrayList<>();
        try {
            saveImagesBitmaps();
            loadImages();
//            processBoundingBoxes();
            drawBoundingBox();

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
        doneButton = findViewById(R.id.done_button);
        leftBox = findViewById(R.id.left_box);
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


    private void drawBoundingBox() {
        ArrayList<ArrayList<BoundingBox>> boxes = client.boxes;
        ArrayList<BoundingBox> currentBox = boxes.get(selectedImageIndex);
        int origHeight = selected.getHeight();
        int origWidth = selected.getWidth();
//        int imViewHeight = selectedImage.getHeight();
//        int imViewWidth = selectedImage.getWidth();
        int x = currentBox.get(selectedImageIndex).x;
        int y = currentBox.get(selectedImageIndex).y;
        int h = currentBox.get(selectedImageIndex).h;
        int w = currentBox.get(selectedImageIndex).w;
        int yOffset = selectedImage.getTop();
        int xOffset = selectedImage.getLeft();
        int xScroll = selectedImage.getScrollX();
        int yScroll = selectedImage.getScrollY();
        int[] location = new int[2];
        selectedImage.getLocationOnScreen(location);

        int xLoc = location[0];
        int yLoc = location[1];

        ConstraintLayout container = findViewById(R.id.container);

        Button btn1 = new Button(this);
        btn1.setBackgroundResource(R.drawable.bounding_box);
        btn1.setX(x + xLoc);
        btn1.setY(y + yLoc);
        btn1.setHeight(h);
        btn1.setWidth(w);
        container.addView(btn1);
        btn1.setVisibility(View.VISIBLE);
        btn1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                // put code on click operation
            }
        });
    }

    private void processBoundingBoxes() {
        ArrayList<ArrayList<BoundingBox>> boxes = client.boxes;
        ArrayList<BoundingBox> currentBox = boxes.get(selectedImageIndex);
        int origHeight = selected.getHeight();
        int origWidth = selected.getWidth();
        int imViewHeight = selectedImage.getHeight();
        int imViewWidth = selectedImage.getWidth();
        int x = currentBox.get(0).x;
        int y = currentBox.get(0).y;
        float newAspectFactor = ImageUtils.resolveAspectFactor(imViewHeight, imViewWidth, origHeight, origWidth);
        Matrix m = selectedImage.getImageMatrix();

//        leftBox.setLayoutParams(layoutParams);


        RectF drawableRect = new RectF(x,
                y,
                ((x * newAspectFactor) + imViewHeight) / newAspectFactor,
                origHeight);
        RectF viewRect = new RectF(0,
                0,
                imViewWidth,
                imViewHeight);
        m.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.FILL);
        selectedImage.setImageMatrix(m);
        selectedImage.setScaleType(ImageView.ScaleType.MATRIX);
        selectedImage.setImageBitmap(selected);

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
//        FrameLayout frame = findViewById(R.id.image_frame);
//        frame.setPadding(5, 5, 5, 5);
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


}