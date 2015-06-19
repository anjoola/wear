package com.anjoola.sharewear;

import android.content.Intent;

/**
 * Base activity. Contains the code needed for:
 *   - Signing out
 */
public class ShareWearActivity extends ShareWearBaseActivity {
    public final static String SIGN_OUT = "SIGN_OUT";

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
