package com.anjoola.sharewear;

import android.app.Application;

import com.anjoola.sharewear.util.ContactDetails;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Contains global state for the application, including the following:
 *   - Location sharing status (on/off)
 */
public class ShareWearApplication extends Application {
    // Whether or not location sharing is turned on.
    private boolean locationSharingOn = false;

    // Google API client for getting account details.
    public GoogleApiClient googleApiClient;

    // Contact details for a newly-created contact.
    public ContactDetails newContactDetails;

    public boolean isLocationSharingOn() {
        return locationSharingOn;
    }
    public void setLocationSharingOn(boolean on) {
        locationSharingOn = on;
    }
}
