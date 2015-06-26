package com.anjoola.sharewear;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

/**
 * Activity to exchange contact information over NFC. Can switch to manual
 * input if NFC is not turned on or not desired.
 */
public class ContactAddNFCActivity extends ShareWearActivity {
    // Animation for NFC.
    AnimationDrawable animation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_add_nfc_activity);

        // Start animation.
        ImageView animationView = (ImageView) findViewById(R.id.nfc_image);
        animationView.setBackgroundResource(R.drawable.nfc_animation);
        animation = (AnimationDrawable) animationView.getBackground();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            animation.start();
        }
        else {
            animation.stop();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contact_add_nfc_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                // TODO
                return true;
            case R.id.action_sign_out:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
