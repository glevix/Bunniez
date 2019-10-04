package postpc.moriaor.bunniez;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class BoundingBox {
    int x, y, w, h;
}

public class BunniezClient {

    String endpoint;
    String id;
    OkHttpClient client;

    BunniezClient(String url) {
        this.endpoint = url;
        this.client = new OkHttpClient();
    }

    BoundingBox[][] parseBoundingBoxes(String text) {
        return null;
    }

    String buildIndexString(int[] indexes) {
        return null;
    }

    /**
     * Send HEAD "init" request, setting id
     * @return True iff success
     */
    boolean do_init() {
        Request request = new Request.Builder()
                .head()
                .header("request", "init")
                .header("id", "")
                .header("params", "")
                .url(endpoint)
                .build();
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            return false;
        }
        if (response.header("params", "").equals("ok")) {
            id = response.header("id", "");
            return true;
        }
        return false;
    }

    /**
     * Send PUT "upload" request, with file
     * @param file The file to send
     * @return True iff success
     */
    boolean do_upload(File file, int index) {
        MediaType mt = MediaType.parse("application/octet-stream");
        Request request = new Request.Builder()
                .put(RequestBody.create(mt, file))
                .header("request", "upload")
                .header("id", id)
                .header("params", Integer.toString(index))
                .url(endpoint)
                .build();
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            return false;
        }
        return response.header("params", "").equals("ok");
    }

    /**
     * Send HEAD "preprocess" request
     *
     * @param baseIndex Index of image to be used as base
     * @return An array of bounding boxes for each image
     */
    BoundingBox[][] do_preprocess(int baseIndex) {
        Request request = new Request.Builder()
                .head()
                .header("request", "preprocess")
                .header("id", id)
                .header("params", Integer.toString(baseIndex))
                .url(endpoint)
                .build();
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            return null;
        }
        String text = response.header("params", "");
        return parseBoundingBoxes(text);
    }

    /**
     * Send GET "get_pic" request
     * @param index index of output photo to retrieve
     * @param out File to write output image to
     * @return true iff success
     */
    boolean do_get_pic(int index, File out) {
        Request request = new Request.Builder()
                .get()
                .header("request", "get_pic")
                .header("id", id)
                .header("params", Integer.toString(index))
                .url(endpoint)
                .build();
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            return false;
        }
        if (response.header("params", "").equals("ok")) {
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
                    return false;
                }
                FileOutputStream fileOutputStream = new FileOutputStream(out);
                fileOutputStream.write(data);
                fileOutputStream.flush();
                fileOutputStream.close();
                return true;
            } catch (IOException | NullPointerException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Send GET "process" request
     * @param indexes array specifying from which image to take each face
     * @param out File to write output image to
     * @return true iff success
     */
    boolean do_process(int[] indexes, File out) {
        Request request = new Request.Builder()
                .get()
                .header("request", "process")
                .header("id", id)
                .header("params", buildIndexString(indexes))
                .url(endpoint)
                .build();
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            return false;
        }
        if (response.header("params", "").equals("ok")) {
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
                    return false;
                }
                FileOutputStream fileOutputStream = new FileOutputStream(out);
                fileOutputStream.write(data);
                fileOutputStream.flush();
                fileOutputStream.close();
                return true;
            } catch (IOException | NullPointerException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Send HEAD "end" request
     * @return True iff success
     */
    boolean do_end() {
        Request request = new Request.Builder()
                .head()
                .header("request", "end")
                .header("id", id)
                .header("params", "")
                .url(endpoint)
                .build();
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            return false;
        }
        return response.header("params", "").equals("ok");
    }







}
