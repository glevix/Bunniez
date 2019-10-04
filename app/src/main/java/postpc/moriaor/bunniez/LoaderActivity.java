package postpc.moriaor.bunniez;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;


public class LoaderActivity extends AppCompatActivity {

    BunniezClient client;
    String request;
    String displayText;
    ArrayList<String> imagePaths;
    int baseIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loader);
        this.initInstances();

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

    private void initInstances() {
        Intent intentCreatedMe = getIntent();
        displayText = intentCreatedMe.getStringExtra("display");
        request = intentCreatedMe.getStringExtra("request");
        imagePaths = intentCreatedMe.getStringArrayListExtra("imagePaths");
        baseIndex = intentCreatedMe.getIntExtra("baseIndex", 0);

    }

    private void postRequest() {
        try {
            switch (request) {
                case "preprocess":
                    this.handlePreprocess();
                    break;
                case "upload":
                    this.handleUpload();
                    break;


            }
        } catch (Exception e) {
            e.printStackTrace();
            returnWithResult(RESULT_CANCELED, "Request Failed");
        }
    }

    private void returnWithResult(final int resultCode, final String text) {
        Runnable runnable = new Runnable() {
            public void run() {
                Log.i("Tag", "Runnable running!");
                Intent data = new Intent();
                data.setData(Uri.parse(text));
                setResult(resultCode, data);
                finish();
            }
        };
        Handler handler = new android.os.Handler();
        handler.postDelayed(runnable, 5000);
    }

    private Runnable composeRunnable(final String requestName) {
        return new Runnable() {
            @Override
            public void run() {
                if(client.error) {
                    Log.i("bunny is sad", requestName+ " request failed");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            returnWithResult(RESULT_CANCELED, "Request Failed");
                        }
                    });
                } else {
                    Log.i("bunny is happy", requestName+ "request completed successfully");

                }
            }
        };
    }

    private Runnable composeGetPicRunnable(int index) {
       return new Runnable() {
           @Override
           public void run() {
               if(client.error) {
                   Log.i("bunny is sad", "getPic request failed");
               } else {
                   Log.i("bunny is happy", "getPic request completed successfully");
                   // save file to bitmap and when done - start selectFaces activity

               }
           }
       };
    }

    private void handleGetPic() {
        for (int i = 0; i < imagePaths.size(); i++) {
            File output = new File(imagePaths.get(i));
            client.do_get_pic(composeGetPicRunnable(i), i, output);
        }
    }


    private void handlePreprocess() {
        client.do_preprocess(composeRunnable("do_preprocess"), baseIndex);
    }

    private void handleUpload() {
        for (int i = 0; i < imagePaths.size(); i++) {
            File source = new File(imagePaths.get(i));
            client.do_upload(composeRunnable("do_upload"), source, i);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

    }
}

