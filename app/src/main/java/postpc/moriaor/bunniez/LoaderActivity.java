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
                case RequestTypes.PREPROCESS:
                    this.handlePreprocess();
                    break;
                case RequestTypes.UPLOAD:
                    this.handleUpload();
                    break;
                case RequestTypes.GET_PIC:
                    this.handleGetPic();
                    break;
                case RequestTypes.PROCESS:
                    this.handleProcess();
                    break;
                default:
                    Log.d(Bunniez.TAG, "Unknown Request Type");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            returnWResultWithDelay(RESULT_CANCELED, "Request Failed");
        }
    }

    private void returnWResultWithDelay(final int resultCode, final String text) {
        Runnable runnable = new Runnable() {
            public void run() {
                Log.i(Bunniez.TAG, "Runnable running!");
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
                    Log.i(Bunniez.TAG, requestName+ " request failed");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            returnWResultWithDelay(RESULT_CANCELED, "Request Failed");
                        }
                    });
                } else {
                    Log.i(Bunniez.TAG, requestName+ "request completed successfully");


                }
            }
        };
    }

    private void handleProcess() {

    }

    private void handleGetPic() {
        for (int i = 0; i < imagePaths.size(); i++) {
            File output = new File(imagePaths.get(i));
            client.do_get_pic(composeRunnable(RequestTypes.GET_PIC), i, output);
        }
    }


    private void handlePreprocess() {
        client.do_preprocess(composeRunnable(RequestTypes.PREPROCESS), baseIndex);
    }

    private void handleUpload() {
        for (int i = 0; i < imagePaths.size(); i++) {
            client.do_upload(composeRunnable(RequestTypes.UPLOAD), imagePaths.get(i), i);
        }

    }

    private void startSelectFacesActivity() {

    }
}

