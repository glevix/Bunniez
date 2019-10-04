package postpc.moriaor.bunniez;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;


public class LoaderActivity extends AppCompatActivity {

    BunniezClient client;
    String request;
    ArrayList<String> imagePaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loader);
        Intent intentCreatedMe = getIntent();
        String displayText = intentCreatedMe.getStringExtra("display");
        request = intentCreatedMe.getStringExtra("request");
        imagePaths = intentCreatedMe.getStringArrayListExtra("imagePaths");

        Bunniez bunniez = (Bunniez) getApplicationContext();
        client = bunniez.getClient();


        TextView loadingDisplay = findViewById(R.id.display_text);
        loadingDisplay.setText(displayText);
        try {
            postRequest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void postRequest() {
        switch(request) {
            case "preprocess":

                Runnable callback = new Runnable() {
                    @Override
                    public void run() {
                        //
                    }
                };


        }
    }

    private void handlePreprocess() {

    }

    private void handleUpload() {
        File output = new File(imagePaths.get(0));
        Runnable callback = new Runnable() {
            @Override
            public void run() {
                //
            }
        };

    }
}

