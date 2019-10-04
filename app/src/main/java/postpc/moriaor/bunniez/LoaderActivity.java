package postpc.moriaor.bunniez;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;



public class LoaderActivity extends AppCompatActivity {

    BunniezClient client;
    String request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loader);
        Intent intentCreatedMe = getIntent();
        String displayText = intentCreatedMe.getStringExtra("display");
        request = intentCreatedMe.getStringExtra("request");

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

    }

}

