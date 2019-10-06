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

    String imagePath;

    Button newButton, saveButton;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_result);

        imagePath = getIntent().getStringExtra("imagePath");

        newButton = findViewById(R.id.newButton);
        saveButton = findViewById(R.id.saveButton);
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
                app.initNewClient();
                Intent reset = new Intent(getApplicationContext(), MainActivity.class);
                reset.putExtra("title", "What would you like to do next?");
                if(reset.resolveActivity(getPackageManager()) != null) {
                    startActivity(reset);
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DisplayResultActivity.this, "Image was saved to your gallery", Toast.LENGTH_LONG).show();
                File f = new File(imagePath);
                ImageUtils.saveToGallery(getApplicationContext(), f);
                //TODO toast "image saved to gallery"
            }
        });

    }


}
