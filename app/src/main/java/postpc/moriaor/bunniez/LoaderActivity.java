package postpc.moriaor.bunniez;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;


public class LoaderActivity extends AppCompatActivity {

    BunniezClient client;
    String request;
    String displayText;
    ArrayList<String> imagePaths;
    int baseIndex;
    boolean[] didUpload;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loader);
        this.initInstances();

        Bunniez bunniez = (Bunniez) getApplicationContext();
        client = bunniez.getClient();
        didUpload = new boolean[3];



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
            returnWResultWithDelay(RESULT_CANCELED, "Request Failed With Exception");
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
        handler.postDelayed(runnable, 3000);
    }

    private Runnable composeRunnable(final String requestName) {
        return new Runnable() {
            @Override
            public void run() {
                if(client.error) {
                    Log.i(Bunniez.TAG, requestName + " request failed");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            returnWResultWithDelay(RESULT_CANCELED, "Request Failed with Client Error");
                        }
                    });
                } else {
                    Log.i(Bunniez.TAG, requestName+ " request completed successfully");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startSelectFacesActivity();
                        }
                    });

                }
            }
        };
    }

    private Runnable composeUploadRunnable(final int index){
        return new Runnable() {
            @Override
            public void run() {
                if(client.error) {
                    LoaderActivity.this.didUpload[index] = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            returnWResultWithDelay(RESULT_CANCELED, "Request Failed with Client Error");
                        }
                    });

                }
                boolean[] arr = LoaderActivity.this.didUpload;
                arr[index] = true;
                if(arr[0] && arr[1] && arr[2]) {
                    handlePreprocess();
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
            client.do_upload(composeUploadRunnable(i), imagePaths.get(i), i);
        }

    }




    private void startSelectFacesActivity() {
        Intent selectFacesIntent = new Intent(this, SelectFacesActivity.class);
        selectFacesIntent.putStringArrayListExtra("imagePaths", imagePaths);
        if(selectFacesIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(selectFacesIntent);
        }

    }
}

