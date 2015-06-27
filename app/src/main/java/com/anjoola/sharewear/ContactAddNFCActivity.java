package com.anjoola.sharewear;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
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

    // NFC manager.
    NfcManager mNfcManager;
    NfcAdapter mAdapter;

    // Prompt for turning on NFC.
    private AlertDialog mNfcDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_add_nfc_activity);

        // Initialize objects.
        mNfcDialog = null;

        // Get NFC manager and adapter.
        mNfcManager = (NfcManager) this.getApplicationContext()
                .getSystemService(Context.NFC_SERVICE);
        mAdapter = mNfcManager.getDefaultAdapter();

        // Start animation.
        ImageView animationView = (ImageView) findViewById(R.id.nfc_image);
        animationView.setBackgroundResource(R.drawable.nfc_animation);
        animation = (AnimationDrawable) animationView.getBackground();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check NFC status each time.
        checkNFCStatus();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus)
            animation.start();
        else
            animation.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contact_add_nfc_menu, menu);
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
     * Checks if NFC is turned on. If not, prompt the user to turn it on.
     */
    private void checkNFCStatus() {
        // NFC is turned off.
        if (mAdapter != null && !mAdapter.isEnabled()) {
            promptUserNFCEnable();
        }
    }

    /**
     * Prompt user to turn on NFC if they haven't done so already.
     */
    private void promptUserNFCEnable() {
        // Create the dialog if it hasn't been created yet.
        if (mNfcDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Set "Go to Settings" and "Cancel" buttons.
            builder.setPositiveButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Go to NFC settings menu.
                    startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Switch to manual input.
                    //Intent intent = new Intent(this, ContactAddActivity.class);
                    //startActivity(intent);
                    // TODO

                }
            });

            builder.setTitle(R.string.nfc_prompt_title);
            builder.setMessage(R.string.nfc_prompt);
            mNfcDialog = builder.create();
            mNfcDialog.setCancelable(false);
            mNfcDialog.setCanceledOnTouchOutside(false);
        }
        mNfcDialog.show();
    }
}
