package postpc.moriaor.bunniez;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class BoundingBox {
    int x, y, w, h;
}

class RequestTypes  {
    static final String INIT = "init";
    static final String PREPROCESS = "preprocess";
    static final String UPLOAD = "upload";
    static final String GET_PIC = "get_pic";
    static final String PROCESS = "process";
    static final String END = "end";
}

class BunniezClient {

    String endpoint;
    String id;
    OkHttpClient client;
    ArrayList<ArrayList<BoundingBox>> boxes;
    String outputPath;
    boolean error;


    BunniezClient(String url) {
        this.endpoint = url;
        this.client = new OkHttpClient();
    }


    void parseBoundingBoxes(String str) {
        str = str.replaceAll("\\s+",""); // remove all whitespace
        if (str == null || str.startsWith("error") || str.charAt(0) != '[') {
            error = true;
            return;
        }
        int c = 1;
        ArrayList<ArrayList<BoundingBox>> boxListList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (str.charAt(c) != '[') {
                error = true;
                return;
            }
            c++;
            ArrayList<BoundingBox> boxlist = new ArrayList<>();
            while (str.charAt(c) != ']') {
                if (str.charAt(c) == ',')
                    c++;
                if (str.charAt(c) != '(') {
                    error = true;
                    return;
                }
                BoundingBox box = new BoundingBox();
                c++;
                int start = c;
                while (str.charAt(c) != ')')
                    c++;
                int end = c;
                c++;

                String boxString = str.substring(start, end);
                String[] strInts = boxString.split(",");
                box.x = Integer.parseInt(strInts[0]);
                box.y = Integer.parseInt(strInts[1]);
                box.w = Integer.parseInt(strInts[2]);
                box.h = Integer.parseInt(strInts[3]);
                boxlist.add(box);
            }
            c += 2;
            boxListList.add(boxlist);
        }
        boxes = boxListList;
    }

    String buildIndexString(ArrayList<Integer> indexes) {
        String str = "";
        for (Integer index : indexes) {
            str = str + index.toString(index);
            str = str + ",";
        }
        return str.substring(0, str.length() - 1);
    }

    /**
     * Send HEAD "init" request, setting id
     *
     * @param runnable To run on completion. Should check for error
     */
    void do_init(final Runnable runnable) {
        Request request = new Request.Builder()
                .head()
                .header("request", "init")
                .header("id", "")
                .header("params", "")
                .url(endpoint)
                .build();

        Call call = client.newCall(request);
        Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                error = true;
                e.printStackTrace();
                runnable.run();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.header("params", "").equals("ok")) {
                    id = response.header("id", "");
                    error = false;
                } else {
                    error = true;
                }
                runnable.run();
            }
        };
        call.enqueue(callback);
    }

    /**
     * Send PUT "upload" request, with file
     *
     * @param runnable To run on completion. Should check for error
     * @param path Path to the file to send
     * @param index The index of the file (0,1,...)
     */
    void do_upload(final Runnable runnable, String path, int index) {
        MediaType mt = MediaType.parse("application/octet-stream");
        File file = new File(path);
        Request request = new Request.Builder()
                .put(RequestBody.create(mt, file))
                .header("request", "upload")
                .header("id", id)
                .header("params", Integer.toString(index))
                .url(endpoint)
                .build();
        Call call = client.newCall(request);
        Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                error = true;
                runnable.run();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                error = !response.header("params", "").equals("ok");
                runnable.run();
            }
        };
        call.enqueue(callback);
    }

    /**
     * Send HEAD "preprocess" request
     * Populates this.boxes with an array of bounding boxes for each image
     *
     * @param runnable To run on completion. Should check this.error
     * @param baseIndex Index of image to be used as base
     */
    void do_preprocess(final Runnable runnable, int baseIndex) {
        Request request = new Request.Builder()
                .head()
                .header("request", "preprocess")
                .header("id", id)
                .header("params", Integer.toString(baseIndex))
                .url(endpoint)
                .build();

        Call call = client.newCall(request);
        Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                error = true;
                runnable.run();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String text = response.header("params", "");
                parseBoundingBoxes(text);
                runnable.run();
            }
        };
        call.enqueue(callback);
    }

    /**
     * Send GET "get_pic" request
     *
     * @param runnable To run on completion. Should check this.error
     * @param index index of output photo to retrieve
     * @param out File to write output image to
     */
    void do_get_pic(final Runnable runnable, int index, final File out) {
        Request request = new Request.Builder()
                .get()
                .header("request", "get_pic")
                .header("id", id)
                .header("params", Integer.toString(index))
                .url(endpoint)
                .build();

        Call call = client.newCall(request);
        Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                error = true;
                runnable.run();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                error = !response.header("params", "").equals("ok");
                if (!error) {
                    try {
                        int contentLength = (int) response.body().contentLength();
                        InputStream raw = response.body().byteStream();
                        InputStream in = new BufferedInputStream(raw);
                        byte[] data = new byte[contentLength];
                        int bytesRead, offset = 0;
                        while (offset < contentLength) {
                            bytesRead = in.read(data, offset, data.length - offset);
                            if (bytesRead == -1)
                                break;
                            offset += bytesRead;
                        }
                        in.close();
                        if (offset != contentLength) {
                            error = true;
                        } else {
                            FileOutputStream fileOutputStream = new FileOutputStream(out);
                            fileOutputStream.write(data);
                            fileOutputStream.flush();
                            fileOutputStream.close();
                        }
                    } catch (IOException | NullPointerException e) {
                        error = true;
                    }
                }
                runnable.run();
            }
        };
        call.enqueue(callback);
    }

    /**
     * Send GET "process" request
     *
     * @param runnable To run on completion. Should check this.error
     * @param indexes array specifying from which image to take each face
     * @param out File to write output image to
     */
    void do_process(final Runnable runnable, ArrayList<Integer> indexes, final File out) {
        Request request = new Request.Builder()
                .get()
                .header("request", "process")
                .header("id", id)
                .header("params", buildIndexString(indexes))
                .url(endpoint)
                .build();

        Call call = client.newCall(request);
        Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                error = true;
                runnable.run();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                error = !response.header("params", "").equals("ok");
                if (!error) {
                    try {
                        int contentLength = (int) response.body().contentLength();
                        InputStream raw = response.body().byteStream();
                        InputStream in = new BufferedInputStream(raw);
                        byte[] data = new byte[contentLength];
                        int bytesRead, offset = 0;
                        while (offset < contentLength) {
                            bytesRead = in.read(data, offset, data.length - offset);
                            if (bytesRead == -1)
                                break;
                            offset += bytesRead;
                        }
                        in.close();
                        if (offset != contentLength) {
                            error = true;
                        } else {
                            FileOutputStream fileOutputStream = new FileOutputStream(out);
                            fileOutputStream.write(data);
                            fileOutputStream.flush();
                            fileOutputStream.close();
                            outputPath = out.getAbsolutePath();
                        }
                    } catch (IOException | NullPointerException e) {
                        error = true;
                    }
                }
                runnable.run();
            }
        };
        call.enqueue(callback);
    }

    /**
     * Send HEAD "end" request
     */
    void do_end() {
        Request request = new Request.Builder()
                .head()
                .header("request", "end")
                .header("id", id)
                .header("params", "")
                .url(endpoint)
                .build();

        Call call = client.newCall(request);
        Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("bunniez client", "failed to do_end");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i("bunniez client", "do_end success");
            }
        };
        call.enqueue(callback);
    }







}
