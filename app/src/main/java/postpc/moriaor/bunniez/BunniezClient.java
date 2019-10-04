package postpc.moriaor.bunniez;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

class BunniezClient {

    String endpoint;
    String id;
    OkHttpClient client;
    BoundingBox[][] boxes;
    boolean error;


    BunniezClient(String url) {
        this.endpoint = url;
        this.client = new OkHttpClient();
    }


    void parseBoundingBoxes(String text) {
    }

    String buildIndexString(int[] indexes) {
        return null;
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
     * @param file The file to send
     * @param index The index of the file (0,1,...)
     */
    void do_upload(final Runnable runnable, File file, int index) {
        MediaType mt = MediaType.parse("application/octet-stream");
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
                error = !response.header("params", "").equals("ok");
                if (!error) {
                    String text = response.header("params", "");
                    parseBoundingBoxes(text);
                }
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
    void do_process(final Runnable runnable, int[] indexes, final File out) {
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
     *
     * @param runnable To run on completion. Should check this.error
     */
    void do_end(final Runnable runnable) {
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







}
