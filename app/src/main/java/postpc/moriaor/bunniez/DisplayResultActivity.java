package postpc.moriaor.bunniez;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class DisplayResultActivity extends AppCompatActivity {

    static final String IMAGE_PATH_KEY = "imagePath";

    String imagePath;

    Button newButton, saveButton, editButton;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePath = getIntent().getStringExtra(IMAGE_PATH_KEY);
        setContentView(R.layout.activity_display_result);
        newButton = findViewById(R.id.newButton);
        saveButton = findViewById(R.id.saveButton);
        editButton = findViewById(R.id.editButton);
        imageView = findViewById(R.id.outputImage);
        File imgFile = new  File(imagePath);

        if(imgFile.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            imageView.setImageBitmap(myBitmap);
            imageView.setClipToOutline(true);
        } // TODO else error


        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bunniez app = (Bunniez) getApplicationContext();
                app.getClient().do_end();
//                app.initNewClient();
                Intent reset = new Intent(getApplicationContext(), MainActivity.class);
                reset.putExtra(MainActivity.TITLE_KEY, getString(R.string.restart_title));
                if(reset.resolveActivity(getPackageManager()) != null) {
                    startActivity(reset);
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DisplayResultActivity.this, getString(R.string.save_text), Toast.LENGTH_LONG).show();
                File f = new File(imagePath);
                ImageUtils.saveToGallery(getApplicationContext(), f);
                //TODO toast "image saved to gallery"
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        imagePath = savedInstanceState.getString(IMAGE_PATH_KEY);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(IMAGE_PATH_KEY, imagePath);

        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }



}
