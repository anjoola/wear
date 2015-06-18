package com.anjoola.sharewear;

import android.app.Application;

import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Contains global state for the application.
 */
public class ShareWearApplication extends Application {

    // Used to connect to Google Play Services and allow Google+ sign in.
    private GoogleApiClient mGoogleApiClient;

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    public void setGoogleApiClient(GoogleApiClient client) {
        mGoogleApiClient = client;
    }
}
