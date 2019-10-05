package postpc.moriaor.bunniez;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


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

    private void runWithDelay(Runnable task, int delay) {
        Handler handler = new android.os.Handler();
        handler.postDelayed(task, delay);
    }

    private void returnWResultWithDelay(final int resultCode, final String text) {
        Runnable runnable = new Runnable() {
            public void run() {
                Intent data = new Intent();
                data.setData(Uri.parse(text));
                setResult(resultCode, data);
                finish();
            }
        };
        runWithDelay(runnable, 3000);
    }

    private Runnable composePreprocessRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if(client.error) {
                    Log.i(Bunniez.TAG, RequestTypes.PREPROCESS + " request failed");
                    onRequestFailure();
                } else {
                    Log.i(Bunniez.TAG, RequestTypes.PREPROCESS+ " request completed successfully");
                    handleGetPic();
                }
            }
        };
    }

    private void onRequestFailure() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                returnWResultWithDelay(RESULT_CANCELED, request + " Failed with Client Error");
            }
        });
    }

    private void onReceiveImages() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startSelectFacesActivity();
                    }
                });
            }
        };
//        runWithDelay(runnable, 3000);
        runnable.run();
    }


    private Runnable composeGetPicRunnable(final int index) {
        return new Runnable() {
            @Override
            public void run() {
                boolean[] arr = LoaderActivity.this.didUpload;
                if(client.error) {
                    arr[index] = false;
                    onRequestFailure();
                } else {
                    arr[index] = true;
                    if(arr[0] && arr[1] && arr[2]) {
                        Arrays.fill(didUpload, false);
                        onReceiveImages();
                    }
                }
            }
        };
    }

    private Runnable composeUploadRunnable(final int index){
        return new Runnable() {
            @Override
            public void run() {
                boolean[] arr = LoaderActivity.this.didUpload;
                if(client.error) {
                    arr[index] = false;
                    onRequestFailure();
                }
                else {
                    arr[index] = true;
                    if(arr[0] && arr[1] && arr[2]) {
                        handlePreprocess();
                        Arrays.fill(didUpload, false);
                    }
                }

            }
        };
    }

    private Runnable composeProcessRunnable(){
        return new Runnable() {
            @Override
            public void run() {
                if(client.error) {
                    onRequestFailure();
                }
                else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startDisplayResultActivity();
                        }
                    });
                }

            }
        };
    }


    private void handleProcess(int[] indexes) {
        File output = null;
        try {
            output = ImageUtils.createImageFile(this, "output");
        } catch (IOException e) {
            //TODO error
        }
        if (output == null || !output.exists()) {
            //TODO error
        }
        client.do_process(composeProcessRunnable(), indexes, output);
    }

    private void handleGetPic() {
        request= RequestTypes.GET_PIC;
        for (int i = 0; i < imagePaths.size(); i++) {
            File output = new File(imagePaths.get(i));
            client.do_get_pic(composeGetPicRunnable(i), i, output);
        }
    }


    private void handlePreprocess() {
        request = RequestTypes.PREPROCESS;
        client.do_preprocess(composePreprocessRunnable(), baseIndex);
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


    private void startDisplayResultActivity() {
        //TODO create activity and put intent here
    }
}

