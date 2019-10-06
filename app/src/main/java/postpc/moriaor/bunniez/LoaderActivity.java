package postpc.moriaor.bunniez;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public class LoaderActivity extends AppCompatActivity {
    static final String REQUEST_KEY = "request";
    static final String DISPLAY_KEY = "display";
    static final String DID_UPLOAD_KEY = "didUpload";
    static final String BASE_INDEX_KEY = "baseIndex";
    static final String CHOSEN_KEY = "indices";

    Bunniez bunniez;
    BunniezClient client;
    String request;
    String displayText;
    ArrayList<String> imagePaths;
    ArrayList<Integer> chosenIndices;
    int baseIndex;
    boolean[] didUpload;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bunniez bunniez = (Bunniez) getApplicationContext();
        imagePaths = new ArrayList<>();
        this.initInstances();
        setContentView(R.layout.activity_loader);
        client = bunniez.getClient();

        TextView loadingDisplay = findViewById(R.id.display_text);
        loadingDisplay.setText(displayText);
        try {
            postRequest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        imagePaths = savedInstanceState.getStringArrayList(Bunniez.IMAGE_PATHS_KEY);
        String clientId = savedInstanceState.getString(Bunniez.CLIENT_ID_KET);
        request = savedInstanceState.getString(REQUEST_KEY);
        didUpload = savedInstanceState.getBooleanArray(DID_UPLOAD_KEY);
        baseIndex = savedInstanceState.getInt(BASE_INDEX_KEY);
        chosenIndices = savedInstanceState.getIntegerArrayList(CHOSEN_KEY);
        displayText = savedInstanceState.getString(DISPLAY_KEY);
        if(clientId != null) {
            bunniez.reinitClient(clientId);
        } else {
            bunniez.initNewClient();
        }
    }

    private void initInstances() {
        Intent intentCreatedMe = getIntent();
        if(intentCreatedMe != null) {
            displayText = intentCreatedMe.getStringExtra(DISPLAY_KEY);
            request = intentCreatedMe.getStringExtra(REQUEST_KEY);
            imagePaths = intentCreatedMe.getStringArrayListExtra(Bunniez.IMAGE_PATHS_KEY);
            baseIndex = intentCreatedMe.getIntExtra(BASE_INDEX_KEY, 0);
            chosenIndices = intentCreatedMe.getIntegerArrayListExtra(CHOSEN_KEY);
            didUpload = new boolean[3];
        }


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
                    this.handleProcess(chosenIndices);
                    break;
                default:
                    Log.d(Bunniez.TAG, "Unknown Request Type");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(Bunniez.TAG, getString(R.string.on_exception));
            returnWResultWithDelay(RESULT_CANCELED, request + getString(R.string.on_exception));
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
                        finish();
                        startSelectFacesActivity();
                    }
                });
            }
        };
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
                            finish();
                            startDisplayResultActivity();
                        }
                    });
                }

            }
        };
    }


    private void handleProcess(ArrayList<Integer> indexes) {
        File output = null;
        try {
            output = ImageUtils.createImageFile(this, "output");
        } catch (IOException e) {
            Log.i(Bunniez.TAG, getString(R.string.process_exception));
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
        selectFacesIntent.putStringArrayListExtra(Bunniez.IMAGE_PATHS_KEY, imagePaths);
        if(selectFacesIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(selectFacesIntent);
        }

    }


    private void startDisplayResultActivity() {
        Intent displayResultIntent = new Intent(this, DisplayResultActivity.class);
        String imagePath = client.outputPath;
        if (imagePath == null) {
            //TODO error
        }
        displayResultIntent.putExtra(DisplayResultActivity.IMAGE_PATH_KEY, imagePath);
        if(displayResultIntent.resolveActivity(getPackageManager()) != null) {
            finish();
            startActivity(displayResultIntent);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(Bunniez.IMAGE_PATHS_KEY, imagePaths);
        outState.putString(Bunniez.CLIENT_ID_KET, client.id);
        outState.putString(REQUEST_KEY, request);
        outState.putBooleanArray(DID_UPLOAD_KEY, didUpload);
        outState.putInt(BASE_INDEX_KEY, baseIndex);
         outState.putIntegerArrayList(CHOSEN_KEY, chosenIndices);
        super.onSaveInstanceState(outState);
    }
}

