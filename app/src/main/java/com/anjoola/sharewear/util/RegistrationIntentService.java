package com.anjoola.sharewear.util;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.anjoola.sharewear.R;
import com.anjoola.sharewear.ShareWearApplication;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

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

                // TODO
                Log.e(TAG, "GCM Registration Token: " + token);

                // Check to see if we need to send the token to the server.
                ShareWearApplication app = (ShareWearApplication) getApplication();
                if (app.getGcmToken() == null || !app.getGcmToken().equals(token))
                    sendRegistrationToServer(token);

                app.setGcmToken(token);
            }
        }
        catch (Exception e) {
            // TODO
            Log.e(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
        }
    }

    /**
     * Persist registration to cloud server.
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // TODO
    }
}