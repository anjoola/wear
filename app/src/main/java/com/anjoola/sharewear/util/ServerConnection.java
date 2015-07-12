package com.anjoola.sharewear.util;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/**
 * Singleton class. Makes requests to the server.
 */
public class ServerConnection {
    // Server location.
    private final static String SERVER_URL = "http://sharewear.anjoola.com:3990";

    // The HTTP client. Used to connect to the server.
    private static DefaultHttpClient client;

    /**
     * Does a POST request to the server with the given JSON data.
     * @param json Data to send to server.
     */
    public static void doPost(JSONObject json) {
        if (client == null)
            client = new DefaultHttpClient();

        // Create POST request.
        HttpPost request = new HttpPost(SERVER_URL);
        try {
            StringEntity entity = new StringEntity(json.toString());
            entity.setContentType("application/json");
            request.setEntity(entity);

            // TODO
            HttpResponse response = client.execute(request);
            Log.e("----", EntityUtils.toString(response.getEntity()));
        }
        // TODO
        catch (Exception e) {
            Log.e("---error", e.toString());
        }
    }
}
