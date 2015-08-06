package com.anjoola.sharewear.util;

import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
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
     *
     * @param json Data to send to server.
     * @return Response from the server.
     */
    public static void doPost(JSONObject json) {
        doPost(json, null);
    }

    /**
     * Does a POST request to the server with the given JSON data.
     *
     * @param json Data to send to server.
     * @param callback Callback to call on after response is received.
     * @return Response from the server.
     */
    public static void doPost(JSONObject json, ServerConnectionCallback callback) {
        if (client == null)
            client = new DefaultHttpClient();

        new BackgroundNetworkTask(callback)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, json);
    }

    /**
     * Used for executing the network request in the background (to avoid
     * interfering with the main thread.
     */
    static class BackgroundNetworkTask extends AsyncTask<JSONObject, Void, JSONObject> {

        // Callback to handle the response from the server.
        ServerConnectionCallback callback;

        public BackgroundNetworkTask(ServerConnectionCallback callback) {
            this.callback = callback;
        }

        protected JSONObject doInBackground(JSONObject... params) {
            JSONObject json = params[0];

            // Create POST request.
            HttpPost request = new HttpPost(SERVER_URL);
            try {
                StringEntity entity = new StringEntity(json.toString());
                entity.setContentType("application/json");
                request.setEntity(entity);

                // Response from the server.
                HttpResponse response = client.execute(request);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
                    return new JSONObject(EntityUtils.toString(response.getEntity()));
            }
            // Could not parse JSON, or an empty response.
            catch (JSONException e) {
                return null;
            }
            catch (Exception e) { }

            return null;
        }

        protected void onPostExecute(JSONObject json) {
            if (callback != null)
                callback.callback(json);
        }
    }
}
