package com.anjoola.sharewear;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Base activity. Contains the code needed for global preferences.
 */
public class ShareWearBaseActivity extends Activity {
    // Preferences objects.
    SharedPreferences pref;
    SharedPreferences.Editor prefEditor;

    public final static String SIGN_OUT = "SIGN_OUT";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = getPreferences(Context.MODE_PRIVATE);
        prefEditor = pref.edit();
    }

    public String prefGetUser() {
        return pref.getString(getString(R.string.pref_user), null);
    }

    public void prefSetUser(String user) {
        if (user != null) {
            prefEditor.putString(getString(R.string.pref_user), user);
        }
        else {
            prefEditor.remove(getString(R.string.pref_user));
        }
        prefEditor.commit();
    }
}
