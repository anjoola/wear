package com.anjoola.sharewear;

import android.app.Application;

/**
 * Contains global state for the application, including the following:
 *   - Location sharing status (on/off)
 */
public class ShareWearApplication extends Application {
    // Whether or not location sharing is turned on.
    private boolean locationSharingOn = false;

    public boolean isLocationSharingOn() {
        return locationSharingOn;
    }
    public void setLocationSharingOn(boolean on) {
        locationSharingOn = on;
    }
}
