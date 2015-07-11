package com.anjoola.sharewear.util;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Service for getting the new registration ID.
 */
public class ShareWearIDListenerService extends InstanceIDListenerService {
    @Override
    public void onTokenRefresh() {
        // If new GCM registration token is refreshed, send the new one to the
        // server.
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }
}
