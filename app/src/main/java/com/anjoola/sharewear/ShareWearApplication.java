package com.anjoola.sharewear;

import android.app.Application;

import com.anjoola.sharewear.util.ContactDetails;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Contains global state for the application, including the following:
 *   - Location sharing status (on/off)
 *   - GCM registration token
 *   - Details for a newly-created contact
 */
public class ShareWearApplication extends Application {
    public static int NOTIFICATION_ID = 399399;

    // Whether or not location sharing is turned on.
    private boolean locationSharingOn = false;

    // Google API client for getting account details.
    public GoogleApiClient googleApiClient;

    // Registration token for GCM.
    private String gcmToken = null;

    // Contact details for a newly-created contact.
    public ContactDetails newContactDetails;

    // Contact details for the current users.
    public String myDetails = null;

    public boolean isLocationSharingOn() {
        return locationSharingOn;
    }
    public void setLocationSharingOn(boolean on) {
        locationSharingOn = on;
    }

    public void setGcmToken(String token) {
        gcmToken = token;
    }
    public String getGcmToken() {
        return gcmToken;
    }
}
