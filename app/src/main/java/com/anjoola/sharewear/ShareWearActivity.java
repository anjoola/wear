package com.anjoola.sharewear;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Base activity. Contains the code needed for the action bar and signing
 * in and out.
 */
public class ShareWearActivity extends ShareWearBaseActivity {
    public final static String SIGN_OUT = "SIGN_OUT";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Add items to action bar if present.
        getMenuInflater().inflate(R.menu.menu_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sign_out:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Attempt to sign out. Go back go the login activity.
     */
    private void signOut() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(SIGN_OUT, "");
        startActivity(intent);
    }
}
