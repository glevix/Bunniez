package postpc.moriaor.bunniez;

import android.app.Application;
import android.util.Log;

public class Bunniez extends Application {
    static final String SERVER_URL = "http://192.168.43.154:8080";
    static final String TAG = "bunbun";

    BunniezClient client;
    boolean didInit;
    boolean hasSetupConnection;

    @Override
    public void onCreate() {
        super.onCreate();
        client =  new BunniezClient(SERVER_URL);
        try {
            client.do_init(new Runnable() {
                @Override
                public void run() {
                    didInit = true;
                    if (client.error) {
                        Log.i(Bunniez.TAG, RequestTypes.INIT + " request failed");
                        hasSetupConnection = false;
                    } else {
                        Log.d(TAG, client.id);
                        hasSetupConnection = true;
                    }
                }
            });
        } catch (Exception e) {
            didInit = true;
            hasSetupConnection = false;
        }
    }


    public BunniezClient getClient() {
        return client;
    }


}
