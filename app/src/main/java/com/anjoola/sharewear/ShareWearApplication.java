package com.anjoola.sharewear;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;

import com.anjoola.sharewear.db.FavoriteDbHelper;
import com.anjoola.sharewear.util.ContactDetails;

import java.util.ArrayList;

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

    // Database access.
    FavoriteDbHelper mDbHelper;

    // List of all contacts.
    public ArrayList<ContactDetails> mContactsList;

    // Whether or not location sharing is turned on.
    private boolean mLocationSharingOn = false;

    // Contact details for a newly-created contact.
    public ContactDetails newContactDetails;

    // Contact details for the current user.
    public String myDetails = null;

    // Reference to the favorites fragment.
    public Fragment favoritesFragment = null;

    @Override
    public void onCreate() {
        super.onCreate();

        mPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        mPrefEditor = mPref.edit();
        mPrefEditor.apply();

        mDbHelper = new FavoriteDbHelper(getApplicationContext());
        mContactsList = new ArrayList<ContactDetails>();
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
}
