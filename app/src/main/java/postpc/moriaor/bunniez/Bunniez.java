package postpc.moriaor.bunniez;

import android.app.Application;
import android.util.Log;

public class Bunniez extends Application {
    static final String SERVER_URL = "http://192.168.56.1:8080";

    BunniezClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        client =  new BunniezClient(SERVER_URL);
        client.do_init(new Runnable() {
            @Override
            public void run() {
                if(client.error) {
                    Log.d("bunbun", "faild");
                }
                else {
                    Log.d("bunbun", client.id);
                }
            }
        });
    }

    public BunniezClient getClient() {
        return client;
    }


}
