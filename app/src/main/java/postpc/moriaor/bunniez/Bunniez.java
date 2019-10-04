package postpc.moriaor.bunniez;

import android.app.Application;

public class Bunniez extends Application {
    static final String SERVER_URL = "url";

    BunniezClient client;
    String clientId;

    @Override
    public void onCreate() {
        super.onCreate();
        client =  new BunniezClient(SERVER_URL);
        client.do_init();
        clientId = client.getId();
    }

    public BunniezClient getClient() {
        if(client == null) {
            client = new BunniezClient(clientId, SERVER_URL);
        }
        return client;
    }
}
