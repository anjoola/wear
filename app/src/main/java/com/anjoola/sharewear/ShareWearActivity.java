package com.anjoola.sharewear;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;

/**
 * Base activity. Contains the code needed for:
 *   - Signing out
 */
public class ShareWearActivity extends FragmentActivity {
    public final static String SIGN_OUT = "SIGN_OUT";
    public final static String PHOTO = "PHOTO";
    public final static String NAME = "NAME";
    public final static String PHONE = "PHONE";
    public final static String EMAIL = "EMAIL";

    /**
     * Attempt to sign out. Go back go the login activity.
     */
    protected void signOut() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(SIGN_OUT, "");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
