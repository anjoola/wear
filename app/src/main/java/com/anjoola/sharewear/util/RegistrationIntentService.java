package com.anjoola.sharewear.util;

import android.app.Application;
import android.app.IntentService;
import android.content.Intent;

import com.anjoola.sharewear.R;
import com.anjoola.sharewear.ShareWearApplication;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get registration ID and send to server.
 */
public class RegistrationIntentService extends IntentService {
    // Tag used for getting registration ID.
    private static final String TAG = "RegIntentService";

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Get the registration ID. Synchronize in the unlikely event that
        // multiple refresh operations occur simultaneously.
        try {
            synchronized (TAG) {
                InstanceID instanceID = InstanceID.getInstance(this);
                String token = instanceID.getToken(getString(R.string.SENDER_ID),
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

                // Check to see if we need to send the token to the server.
                ShareWearApplication app = (ShareWearApplication) getApplication();
                if (app.prefGetGcmToken() == null ||
                        !app.prefGetGcmToken().equals(token)) {
                    sendRegistrationToServer(getApplication(), token);
                }
            }
        }
        catch (Exception e) { }
    }

    /**
     * Persist registration to cloud server. Send a POST request.
     *
     * @param app Reference to the application.
     * @param token The new token.
     */
    public static void sendRegistrationToServer(Application app, String token) {
        try {
            // Get details for this user.
            String details = ((ShareWearApplication) app).myDetails;
            ContactDetails info = ContactDetails.decodeNfcData(details);

            // Construct request.
            JSONObject json = new JSONObject();
            json.put(ServerField.COMMAND, ServerField.NEW_USER);
            json.put(ServerField.USER_ID, token);
            json.put(ServerField.NAME, info.name);
            json.put(ServerField.PHONE, info.phone);
            json.put(ServerField.EMAIL, info.email);
            ServerConnection.doPost(json, new RegistrationCallback(app, token));
        }
        catch (JSONException e) { }
    }

    /**
     * Save the token only if we are able to send it to the server.
     */
    public static class RegistrationCallback implements ServerConnectionCallback {
        private ShareWearApplication app;
        private String token;

        public RegistrationCallback(Application app, String token) {
            this.app = (ShareWearApplication) app;
            this.token = token;
        }

        public void callback(JSONObject json) {
            app.prefSetGcmToken(token);
        }
    }
}
