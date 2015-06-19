package com.anjoola.sharewear;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * Base activity. Contains the code needed for global preferences.
 */
public class ShareWearBaseActivity extends FragmentActivity {
    // Preferences objects.
    SharedPreferences mPref;
    SharedPreferences.Editor mPrefEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPref = getPreferences(Context.MODE_PRIVATE);
        mPrefEditor = mPref.edit();
    }

    public String prefGetUser() {
        return mPref.getString(getString(R.string.pref_user), null);
    }

    public void prefSetUser(String user) {
        if (user != null) {
            mPrefEditor.putString(getString(R.string.pref_user), user);
        }
        else {
            mPrefEditor.remove(getString(R.string.pref_user));
        }
        mPrefEditor.commit();
    }
}
