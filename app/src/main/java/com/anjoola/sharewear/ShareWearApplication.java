package com.anjoola.sharewear;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

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

    // Preferences objects.
    SharedPreferences mPref;
    SharedPreferences.Editor mPrefEditor;

    // Whether or not location sharing is turned on.
    private boolean mLocationSharingOn = false;

    // Google API client for getting account details.
    public GoogleApiClient googleApiClient;

    // Contact details for a newly-created contact.
    public ContactDetails newContactDetails;

    // Contact details for the current users.
    public String myDetails = null;

    @Override
    public void onCreate() {
        super.onCreate();

        mPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        mPrefEditor = mPref.edit();
        mPrefEditor.apply();
    }

    /**
     * Turns location sharing on and off. Does not save this in the preferences.
     */
    public boolean isLocationSharingOn() {
        return mLocationSharingOn;
    }
    public void setLocationSharingOn(boolean on) {
        mLocationSharingOn = on;
    }

    /**
     * Get and set the GCM token for the current user.
     */
    public String prefGetGcmToken() {
        return mPref.getString(getString(R.string.pref_gcm_token), null);
    }
    public void prefSetGcmToken(String token) {
        if (token != null)
            mPrefEditor.putString(getString(R.string.pref_gcm_token), token);
        else
            mPrefEditor.remove(getString(R.string.pref_gcm_token));

        mPrefEditor.commit();
    }

    /**
     * Get and set contact details for the current user.
     */
    public String prefGetContactDetails() {
        return mPref.getString(getString(R.string.pref_contact_details), null);
    }
    public void prefSetContactDetails(String details) {
        if (details != null)
            mPrefEditor.putString(getString(R.string.pref_contact_details), details);
        else
            mPrefEditor.remove(getString(R.string.pref_contact_details));

        mPrefEditor.commit();
    }

    /**
     * Get and set the current user for the Google API connection.
     */
    public String prefGetUser() {
        return mPref.getString(getString(R.string.pref_user), null);
    }
    public void prefSetUser(String user) {
        if (user != null)
            mPrefEditor.putString(getString(R.string.pref_user), user);
        else
            mPrefEditor.remove(getString(R.string.pref_user));

        mPrefEditor.commit();
    }
}
