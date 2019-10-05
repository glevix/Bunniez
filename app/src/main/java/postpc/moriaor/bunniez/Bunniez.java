package postpc.moriaor.bunniez;

import android.app.Application;
import android.util.Log;

public class Bunniez extends Application {
    static final String SERVER_URL = "http://192.168.56.1:8080";

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
                        Log.d("bunbun", "faild");
                        hasSetupConnection = false;
                    } else {
                        Log.d("bunbun", client.id);
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
